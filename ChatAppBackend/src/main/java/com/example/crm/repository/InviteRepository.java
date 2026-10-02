package com.example.crm.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.crm.entity.Invite;

@Repository
public interface InviteRepository extends MongoRepository<Invite, String> {

	Optional<Invite> findByCode(String code);

	List<Invite> findByServerIdOrderByCreatedAtDesc(String serverId);

	void deleteByServerId(String serverId);
}
