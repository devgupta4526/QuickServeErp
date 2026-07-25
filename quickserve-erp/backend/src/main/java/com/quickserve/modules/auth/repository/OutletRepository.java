package com.quickserve.modules.auth.repository;

import com.quickserve.modules.auth.entity.Outlet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutletRepository extends JpaRepository<Outlet, UUID> {

    List<Outlet> findByBusinessId(UUID businessId);

    List<Outlet> findByBusinessIdAndActiveTrue(UUID businessId);
}
