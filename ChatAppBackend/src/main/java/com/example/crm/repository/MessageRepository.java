package com.example.crm.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.crm.entity.Message;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

	Optional<Message> findByRoomIdAndSenderIdAndClientMessageId(String roomId, String senderId, String clientMessageId);

	Slice<Message> findByRoomIdOrderByCreatedAtDesc(String roomId, Pageable pageable);

	Slice<Message> findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(String roomId, Instant before, Pageable pageable);

	List<Message> findByRoomIdAndPinnedTrueOrderByCreatedAtDesc(String roomId);

	List<Message> findTop50ByRoomIdAndDeletedFalseAndContentContainingIgnoreCaseOrderByCreatedAtDesc(String roomId, String content);

	long countByRoomIdAndCreatedAtAfterAndSenderIdNot(String roomId, Instant after, String senderId);

	long countByRoomIdAndSenderIdNot(String roomId, String senderId);

	void deleteByRoomId(String roomId);

	void deleteByRoomIdIn(java.util.Collection<String> roomIds);
}
