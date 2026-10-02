package com.example.crm.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.crm.entity.Friendship;

@Repository
public interface FriendshipRepository extends MongoRepository<Friendship, String> {

	Optional<Friendship> findByPairKey(String pairKey);

	List<Friendship> findByRequesterIdOrAddresseeId(String requesterId, String addresseeId);
}
