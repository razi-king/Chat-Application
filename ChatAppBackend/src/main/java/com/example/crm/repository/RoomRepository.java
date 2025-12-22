package com.example.crm.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.crm.entity.Room;

@Repository
public interface RoomRepository extends MongoRepository<Room, String> {

	Room findByRoomId(String roomId);

	boolean existsByRoomId(String roomId);

}
