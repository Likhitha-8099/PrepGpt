package com.prepgpt.repository;

import com.prepgpt.entity.AiTokenUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiTokenUsageRepository extends JpaRepository<AiTokenUsage, Long> {
}
