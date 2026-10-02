package com.example.crm.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.crm.entity.Room;

@Repository
public interface RoomRepository extends MongoRepository<Room, String> {

	Optional<Room> findByDirectKey(String directKey);

	List<Room> findByServerIdOrderByPositionAsc(String serverId);

	long countByServerId(String serverId);

	void deleteByServerId(String serverId);
}
