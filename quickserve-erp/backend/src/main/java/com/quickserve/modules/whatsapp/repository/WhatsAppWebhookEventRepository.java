package com.quickserve.modules.whatsapp.repository;

import com.quickserve.modules.whatsapp.entity.WhatsAppWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WhatsAppWebhookEventRepository extends JpaRepository<WhatsAppWebhookEvent, UUID> {
}
