package com.quickserve.modules.whatsapp.repository;

import com.quickserve.modules.whatsapp.entity.WhatsAppMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WhatsAppMessageLogRepository extends JpaRepository<WhatsAppMessageLog, UUID> {
    Optional<WhatsAppMessageLog> findByWamid(String wamid);
}
