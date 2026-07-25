package com.quickserve.modules.menu.repository;

import com.quickserve.modules.menu.entity.TaxSlab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaxSlabRepository extends JpaRepository<TaxSlab, UUID> {
    List<TaxSlab> findByBusinessIdAndActiveTrue(UUID businessId);
}
