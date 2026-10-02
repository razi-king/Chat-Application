package com.example.crm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.crm.entity.AuditLog;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

	Page<AuditLog> findByServerIdOrderByCreatedAtDesc(String serverId, Pageable pageable);

	void deleteByServerId(String serverId);
}
