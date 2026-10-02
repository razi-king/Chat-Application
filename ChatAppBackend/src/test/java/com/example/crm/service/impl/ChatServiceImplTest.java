package com.example.crm.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;

import com.example.crm.dto.request.SendMessageRequest;
import com.example.crm.dto.response.MessageResponse;
import com.example.crm.entity.Message;
import com.example.crm.entity.Room;
import com.example.crm.entity.RoomMember;
import com.example.crm.entity.User;
import com.example.crm.enums.ErrorCode;
import com.example.crm.enums.RoomType;
import com.example.crm.exception.BadRequestException;
import com.example.crm.exception.ChatAppException;
import com.example.crm.exception.ForbiddenException;
import com.example.crm.exception.RateLimitException;
import com.example.crm.exception.ResourceNotFoundException;
import com.example.crm.mapper.MessageMapper;
import com.example.crm.repository.MessageRepository;
import com.example.crm.repository.RoomMemberRepository;
import com.example.crm.repository.RoomRepository;
import com.example.crm.repository.UserRepository;
import com.example.crm.security.AccessGuard;
import com.example.crm.service.AuditLogService;
import com.example.crm.service.NotificationService;
import com.example.crm.service.RateLimitService;
import com.example.crm.service.RealtimeService;
import com.example.crm.service.UnreadService;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {
	private static final String ROOM = "room1";
	private static final String ALICE = "alice";
	private static final String BOB = "bob";

	@Mock private MessageRepository messageRepository;
	@Mock private RoomRepository roomRepository;
	@Mock private RoomMemberRepository roomMemberRepository;
	@Mock private UserRepository userRepository;
	@Mock private AccessGuard accessGuard;
	@Mock private MessageMapper messageMapper;
	@Mock private RealtimeService realtimeService;
	@Mock private UnreadService unreadService;
	@Mock private RateLimitService rateLimitService;
	@Mock private NotificationService notificationService;
	@Mock private AuditLogService auditLogService;
	@InjectMocks private ChatServiceImpl chatService;

	private final Room room = Room.builder().id(ROOM).type(RoomType.DIRECT).build();
	private final RoomMember aliceMember = RoomMember.builder().roomId(ROOM).userId(ALICE).build();
	private final RoomMember bobMember = RoomMember.builder().roomId(ROOM).userId(BOB).build();

	@BeforeEach
	void setUp() {
		lenient().when(accessGuard.requireRoom(ROOM)).thenReturn(room);
		lenient().when(accessGuard.requireRoomMember(ROOM, ALICE)).thenReturn(aliceMember);
		lenient().when(roomMemberRepository.findByRoomId(ROOM)).thenReturn(List.of(aliceMember, bobMember));
		lenient().when(userRepository.findById(ALICE)).thenReturn(Optional.of(User.builder().id(ALICE).displayName("Alice").build()));
		lenient().when(messageMapper.toResponse(any())).thenAnswer(inv -> {
			Message m = inv.getArgument(0);
			return MessageResponse.builder().id(m.getId()).content(m.getContent()).clientMessageId(m.getClientMessageId()).build();
		});
	}

	private static SendMessageRequest request(String content, String clientMessageId) {
		SendMessageRequest r = new SendMessageRequest();
		r.setContent(content);
		r.setClientMessageId(clientMessageId);
		return r;
	}

	// Protects The Core Flow: Persist FIRST, Then Deliver, And Only OTHER Members Get An Unread Badge
	@Test
	void sendMessage_valid_persistsBroadcastsAndCountsUnreadForOthersOnly() {
		when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
			Message m = inv.getArgument(0);
			m.setId("m1");
			return m;
		});

		MessageResponse response = chatService.sendMessage(ROOM, request("  hello  ", "c-1"), ALICE);

		assertThat(response.getId()).isEqualTo("m1");
		assertThat(response.getContent()).isEqualTo("hello");
		verify(realtimeService).toRoom(eq(ROOM), any());
		verify(unreadService).increment(bobMember);
		verify(unreadService, never()).increment(aliceMember);
	}

	// Protects: Least Privilege -> A Non Member Cannot Write Into Someone Else's Conversation
	@Test
	void sendMessage_notMember_forbiddenAndNothingSaved() {
		when(accessGuard.requireRoomMember(ROOM, "mallory")).thenThrow(new ForbiddenException(ErrorCode.NOT_ROOM_MEMBER));

		assertThatThrownBy(() -> chatService.sendMessage(ROOM, request("hi", null), "mallory"))
				.isInstanceOf(ForbiddenException.class);
		verify(messageRepository, never()).save(any());
		verify(realtimeService, never()).toRoom(anyString(), any());
	}

	@Test
	void sendMessage_unknownRoom_notFound() {
		when(accessGuard.requireRoom("nope")).thenThrow(new ResourceNotFoundException(ErrorCode.ROOM_NOT_FOUND));

		assertThatThrownBy(() -> chatService.sendMessage("nope", request("hi", null), ALICE))
				.isInstanceOf(ResourceNotFoundException.class)
				.extracting(ex -> ((ChatAppException) ex).getErrorCode()).isEqualTo(ErrorCode.ROOM_NOT_FOUND);
	}

	// Protects Idempotency: A Network Retry Returns The Stored Message - No Second Row, No Second Unread Badge
	@Test
	void sendMessage_duplicateClientMessageId_returnsOriginalWithoutSaving() {
		Message original = Message.builder().id("m1").roomId(ROOM).senderId(ALICE).clientMessageId("c-1").content("hello").build();
		when(messageRepository.findByRoomIdAndSenderIdAndClientMessageId(ROOM, ALICE, "c-1")).thenReturn(Optional.of(original));

		MessageResponse response = chatService.sendMessage(ROOM, request("hello", "c-1"), ALICE);

		assertThat(response.getId()).isEqualTo("m1");
		verify(messageRepository, never()).save(any());
		verify(unreadService, never()).increment(any());
		verify(rateLimitService, never()).record(anyString(), anyString(), anyInt());
	}

	// Protects Idempotency Under Concurrency: Both Copies Passed The Lookup, The Unique Index Rejected One
	@Test
	void sendMessage_concurrentDuplicate_uniqueIndexLoserReturnsWinner() {
		Message winner = Message.builder().id("m1").roomId(ROOM).senderId(ALICE).clientMessageId("c-1").content("hello").build();
		when(messageRepository.findByRoomIdAndSenderIdAndClientMessageId(ROOM, ALICE, "c-1"))
				.thenReturn(Optional.empty())
				.thenReturn(Optional.of(winner));
		when(messageRepository.save(any(Message.class))).thenThrow(new DuplicateKeyException("E11000 idempotency_key"));

		MessageResponse response = chatService.sendMessage(ROOM, request("hello", "c-1"), ALICE);

		assertThat(response.getId()).isEqualTo("m1");
		verify(unreadService, never()).increment(any());
		// The Losing Duplicate Must Not Count Against The Sender's Rate Limit
		verify(rateLimitService, never()).record(anyString(), anyString(), anyInt());
	}

	@Test
	void sendMessage_rateLimited_nothingSaved() {
		doThrow(new RateLimitException(ErrorCode.RATE_LIMITED)).when(rateLimitService).ensureBelowLimit(eq(ALICE), anyString(), anyInt(), anyInt());

		assertThatThrownBy(() -> chatService.sendMessage(ROOM, request("spam", null), ALICE)).isInstanceOf(RateLimitException.class);
		verify(messageRepository, never()).save(any());
	}

	// Protects: You Cannot Quote A Message From A Room You Are Not In (Data Leak Via replyToId)
	@Test
	void sendMessage_replyToMessageOfAnotherRoom_badRequest() {
		when(messageRepository.findById("foreign"))
				.thenReturn(Optional.of(Message.builder().id("foreign").roomId("other-room").build()));
		SendMessageRequest r = request("hi", null);
		r.setReplyToId("foreign");

		assertThatThrownBy(() -> chatService.sendMessage(ROOM, r, ALICE)).isInstanceOf(BadRequestException.class);
		verify(messageRepository, never()).save(any());
	}

	// Protects: Persistence Failure Is Not Hidden -> No "Ghost" Message Is Delivered That Was Never Stored
	@Test
	void sendMessage_databaseDown_exceptionPropagatesAndNothingDelivered() {
		when(messageRepository.save(any(Message.class))).thenThrow(new DataAccessResourceFailureException("Mongo timeout"));

		assertThatThrownBy(() -> chatService.sendMessage(ROOM, request("hello", "c-9"), ALICE))
				.isInstanceOf(DataAccessResourceFailureException.class);
		verify(realtimeService, never()).toRoom(anyString(), any());
		verify(unreadService, never()).increment(any());
	}

	@Test
	void edit_someoneElsesMessage_forbidden() {
		when(messageRepository.findById("m1")).thenReturn(Optional.of(Message.builder().id("m1").roomId(ROOM).senderId(BOB).content("x").build()));

		assertThatThrownBy(() -> chatService.edit("m1", "changed", ALICE))
				.isInstanceOf(ForbiddenException.class)
				.extracting(ex -> ((ChatAppException) ex).getErrorCode()).isEqualTo(ErrorCode.MESSAGE_NOT_OWNED);
		verify(messageRepository, times(0)).save(any());
	}
}
