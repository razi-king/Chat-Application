package com.example.crm.config;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.crm.dto.request.CreateGroupRequest;
import com.example.crm.dto.request.CreateServerRequest;
import com.example.crm.dto.request.SendMessageRequest;
import com.example.crm.dto.response.RoomResponse;
import com.example.crm.dto.response.ServerResponse;
import com.example.crm.entity.Friendship;
import com.example.crm.entity.Invite;
import com.example.crm.entity.Room;
import com.example.crm.entity.User;
import com.example.crm.enums.FriendshipStatus;
import com.example.crm.enums.MemberRole;
import com.example.crm.repository.FriendshipRepository;
import com.example.crm.repository.InviteRepository;
import com.example.crm.repository.RoomRepository;
import com.example.crm.repository.ServerRepository;
import com.example.crm.repository.UserRepository;
import com.example.crm.service.ChatService;
import com.example.crm.service.RoomService;
import com.example.crm.service.ServerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Fills an EMPTY database with demo users, friendships, two servers, a group and a DM,
 * so the app can be tested right after the first start. Disable with app.seed.enabled=false.
 * Every demo account uses the password from app.seed.password.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {
	private final UserRepository userRepository;
	private final FriendshipRepository friendshipRepository;
	private final ServerRepository serverRepository;
	private final RoomRepository roomRepository;
	private final InviteRepository inviteRepository;
	private final PasswordEncoder passwordEncoder;
	private final ServerService serverService;
	private final RoomService roomService;
	private final ChatService chatService;

	@Value("${app.seed.password}")
	private String demoPassword;

	@Override
	public void run(ApplicationArguments args) {
		if (userRepository.count() > 0) {
			return;
		}
		log.info("Empty database -> creating demo data");
		User razi = user("razi", "Razi Khan", "#22d3ee", "Full stack developer. Building Nexus.");
		User aisha = user("aisha", "Aisha Patel", "#f472b6", "Designer by day, gamer by night");
		User kabir = user("kabir", "Kabir Mehta", "#34d399", "Backend | Java | Coffee");
		User zoya = user("zoya", "Zoya Shaikh", "#a78bfa", "Exam season survivor");
		User dev = user("dev", "Dev Sharma", "#fbbf24", "Open source enthusiast");

		friends(razi, aisha, FriendshipStatus.ACCEPTED);
		friends(razi, kabir, FriendshipStatus.ACCEPTED);
		friends(aisha, kabir, FriendshipStatus.ACCEPTED);
		friends(zoya, razi, FriendshipStatus.PENDING);
		friends(dev, kabir, FriendshipStatus.ACCEPTED);

		// Discord Side: A Server With Roles + A Public One To Discover
		CreateServerRequest hq = new CreateServerRequest();
		hq.setName("Nexus HQ");
		hq.setDescription("Home of the Nexus chat project");
		hq.setIconColor("#22d3ee");
		ServerResponse hqServer = serverService.create(hq, razi.getId());
		var hqEntity = serverRepository.findById(hqServer.getId()).orElseThrow();
		for (User u : List.of(aisha, kabir, zoya)) {
			serverService.addMember(hqEntity, u.getId());
		}
		serverService.updateRole(hqServer.getId(), aisha.getId(), MemberRole.ADMIN, razi.getId());
		Room general = roomRepository.findByServerIdOrderByPositionAsc(hqServer.getId()).get(0);
		say(general.getId(), razi, "Welcome everyone! This server is our playground for the Nexus project.");
		say(general.getId(), aisha, "The new 3D landing page looks insane. Great work @razi!");
		say(general.getId(), kabir, "Redis presence + unread counters are live. Try opening two tabs.");
		say(general.getId(), zoya, "Can we pin the exam schedule somewhere?");
		inviteRepository.save(Invite.builder().code("NEXUSHQ1").serverId(hqServer.getId()).createdBy(razi.getId())
				.maxUses(0).uses(0).createdAt(Instant.now()).build());

		CreateServerRequest lounge = new CreateServerRequest();
		lounge.setName("Open Source Lounge");
		lounge.setDescription("Share projects, get feedback, find collaborators");
		lounge.setIconColor("#34d399");
		lounge.setDiscoverable(true);
		ServerResponse loungeServer = serverService.create(lounge, kabir.getId());
		serverService.addMember(serverRepository.findById(loungeServer.getId()).orElseThrow(), dev.getId());
		Room loungeGeneral = roomRepository.findByServerIdOrderByPositionAsc(loungeServer.getId()).get(0);
		say(loungeGeneral.getId(), dev, "Anyone up for Hacktoberfest this year?");

		// WhatsApp Side: A DM And A Group
		RoomResponse dm = roomService.openDirect(aisha.getId(), razi.getId());
		say(dm.getId(), aisha, "Hey! Did you finish the chat UI?");
		say(dm.getId(), razi, "Almost, adding reactions and replies now");
		say(dm.getId(), aisha, "Send me the link when it is up");

		CreateGroupRequest group = new CreateGroupRequest();
		group.setName("Exam Squad");
		group.setDescription("Notes, doubts and panic");
		group.setMemberIds(List.of(kabir.getId(), zoya.getId()));
		RoomResponse squad = roomService.createGroup(group, razi.getId());
		say(squad.getId(), kabir, "Viva is on Monday, who has the DBMS notes?");
		say(squad.getId(), zoya, "I have them, sharing tonight");
		log.info("Demo data ready ({} users). Invite code: NEXUSHQ1", userRepository.count());
	}

	private User user(String username, String name, String color, String about) {
		return userRepository.save(User.builder().username(username).email(username + "@nexus.dev")
				.passwordHash(passwordEncoder.encode(demoPassword)).displayName(name).avatarColor(color).about(about)
				.lastSeenAt(Instant.now()).build());
	}

	private void friends(User a, User b, FriendshipStatus status) {
		String key = a.getId().compareTo(b.getId()) < 0 ? a.getId() + ":" + b.getId() : b.getId() + ":" + a.getId();
		friendshipRepository.save(Friendship.builder().requesterId(a.getId()).addresseeId(b.getId()).pairKey(key)
				.status(status).createdAt(Instant.now()).respondedAt(status == FriendshipStatus.ACCEPTED ? Instant.now() : null).build());
	}

	private void say(String roomId, User sender, String text) {
		SendMessageRequest request = new SendMessageRequest();
		request.setContent(text);
		chatService.sendMessage(roomId, request, sender.getId());
	}
}
