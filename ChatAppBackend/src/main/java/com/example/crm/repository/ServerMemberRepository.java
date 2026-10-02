package com.example.crm.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.crm.entity.ServerMember;

@Repository
public interface ServerMemberRepository extends MongoRepository<ServerMember, String> {

	Optional<ServerMember> findByServerIdAndUserId(String serverId, String userId);

	List<ServerMember> findByUserId(String userId);

	List<ServerMember> findByServerId(String serverId);

	boolean existsByServerIdAndUserId(String serverId, String userId);

	long countByServerId(String serverId);

	void deleteByServerId(String serverId);
}
