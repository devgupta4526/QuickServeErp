package com.quickserve.modules.whatsapp.repository;

import com.quickserve.modules.whatsapp.entity.WhatsAppConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WhatsAppConfigRepository extends JpaRepository<WhatsAppConfig, UUID> {
    Optional<WhatsAppConfig> findByBusinessId(UUID businessId);
}
