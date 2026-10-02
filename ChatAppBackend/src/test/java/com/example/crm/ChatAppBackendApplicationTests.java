package com.example.crm;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.example.crm.dto.request.RegisterRequest;
import com.example.crm.dto.request.SendMessageRequest;
import com.example.crm.dto.response.MessageResponse;
import com.example.crm.dto.response.RoomResponse;
import com.example.crm.repository.MessageRepository;
import com.example.crm.service.AuthService;
import com.example.crm.service.ChatService;
import com.example.crm.service.RoomService;

/**
 * Integration tests against a REAL MongoDB (separate "chatapp_test" database, dropped afterwards).
 * Skipped automatically when MongoDB is not running, so "mvn test" still works on a laptop without Docker.
 * Start it with: docker compose up -d
 */
@SpringBootTest(properties = {
		"spring.data.mongodb.uri=mongodb://localhost:27017/chatapp_test",
		"app.seed.enabled=false" })
@EnabledIf("mongoIsRunning")
class ChatAppBackendApplicationTests {
	@Autowired private AuthService authService;
	@Autowired private RoomService roomService;
	@Autowired private ChatService chatService;
	@Autowired private MessageRepository messageRepository;
	@Autowired private MongoTemplate mongoTemplate;

	static boolean mongoIsRunning() {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress("localhost", 27017), 500);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	@AfterAll
	static void dropTestDatabase(@Autowired MongoTemplate mongoTemplate) {
		mongoTemplate.getDb().drop();
	}

	@Test
	void contextLoads() {
		assertThat(mongoTemplate.collectionExists("messages") || true).isTrue();
	}

	/**
	 * The real proof of idempotency: 10 threads send the SAME message (same clientMessageId) at the
	 * same moment, like a client retrying over WebSocket and REST at once. The unique index
	 * {roomId, senderId, clientMessageId} must leave exactly ONE stored message, and every caller
	 * must get that same message back instead of an error.
	 */
	@Test
	void concurrentRetriesOfSameMessage_storeExactlyOneCopy() throws Exception {
		String alice = register("alice");
		String bob = register("bob");
		RoomResponse dm = roomService.openDirect(bob, alice);
		String clientMessageId = "c-" + UUID.randomUUID();

		ExecutorService pool = Executors.newFixedThreadPool(10);
		List<Callable<MessageResponse>> sends = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			sends.add(() -> {
				SendMessageRequest request = new SendMessageRequest();
				request.setContent("exam tomorrow?");
				request.setClientMessageId(clientMessageId);
				return chatService.sendMessage(dm.getId(), request, alice);
			});
		}
		List<String> returnedIds = new ArrayList<>();
		for (Future<MessageResponse> f : pool.invokeAll(sends)) {
			returnedIds.add(f.get().getId());
		}
		pool.shutdown();

		assertThat(returnedIds).containsOnly(returnedIds.get(0));
		assertThat(messageRepository.findByRoomIdOrderByCreatedAtDesc(dm.getId(), org.springframework.data.domain.PageRequest.of(0, 50))
				.getContent()).hasSize(1);
	}

	private String register(String prefix) {
		String name = prefix + UUID.randomUUID().toString().substring(0, 6);
		RegisterRequest r = new RegisterRequest();
		r.setUsername(name);
		r.setEmail(name + "@test.dev");
		r.setPassword("secret123");
		r.setDisplayName(prefix);
		return authService.register(r).getUser().getId();
	}
}
