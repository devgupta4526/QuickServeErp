package com.quickserve.modules.menu.repository;

import com.quickserve.modules.menu.entity.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {
    Page<MenuItem>  findByBusinessIdAndArchivedFalse(UUID businessId, Pageable pageable);
    List<MenuItem>  findByCategoryIdAndAvailableTrueAndArchivedFalse(UUID categoryId);
    List<MenuItem>  findByBusinessIdAndAvailableTrueAndArchivedFalse(UUID businessId);

    @Query("SELECT m FROM MenuItem m WHERE m.businessId = :businessId AND m.archived = false AND m.name ILIKE %:name%")
    List<MenuItem>  findByBusinessIdAndNameContainingIgnoreCase(UUID businessId, String name);
}
