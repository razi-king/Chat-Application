package com.example.crm.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.crm.entity.RoomMember;

@Repository
public interface RoomMemberRepository extends MongoRepository<RoomMember, String> {

	Optional<RoomMember> findByRoomIdAndUserId(String roomId, String userId);

	List<RoomMember> findByUserId(String userId);

	List<RoomMember> findByRoomId(String roomId);

	List<RoomMember> findByRoomIdIn(java.util.Collection<String> roomIds);

	boolean existsByRoomIdAndUserId(String roomId, String userId);

	void deleteByRoomId(String roomId);

	void deleteByRoomIdIn(java.util.Collection<String> roomIds);
}
