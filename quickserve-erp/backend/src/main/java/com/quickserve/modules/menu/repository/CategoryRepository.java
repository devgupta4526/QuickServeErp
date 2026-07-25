package com.quickserve.modules.menu.repository;

import com.quickserve.modules.menu.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByBusinessIdAndActiveTrueOrderBySortOrderAsc(UUID businessId);
    List<Category> findByBusinessIdOrderBySortOrderAsc(UUID businessId);
}
