package com.example.crm.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.crm.entity.Server;

@Repository
public interface ServerRepository extends MongoRepository<Server, String> {

	List<Server> findTop30ByDiscoverableTrueOrderByCreatedAtDesc();
}
