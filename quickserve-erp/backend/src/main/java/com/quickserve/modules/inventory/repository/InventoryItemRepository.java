package com.quickserve.modules.inventory.repository;

import com.quickserve.modules.inventory.entity.InventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
    Page<InventoryItem> findByBusinessId(UUID businessId, Pageable pageable);
}
