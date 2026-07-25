package com.quickserve.modules.crm.repository;

import com.quickserve.modules.crm.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Page<Customer> findByBusinessId(UUID businessId, Pageable pageable);

    Optional<Customer> findByBusinessIdAndPhone(UUID businessId, String phone);

    @Query("SELECT c FROM Customer c WHERE c.businessId = :businessId AND " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%',:query,'%')) OR c.phone LIKE CONCAT('%',:query,'%'))")
    List<Customer> searchByNameOrPhone(UUID businessId, String query);

    boolean existsByBusinessIdAndPhone(UUID businessId, String phone);
}
