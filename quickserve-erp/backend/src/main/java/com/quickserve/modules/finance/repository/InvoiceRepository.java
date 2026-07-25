package com.quickserve.modules.finance.repository;

import com.quickserve.modules.finance.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Page<Invoice>   findByBusinessId(UUID businessId, Pageable pageable);
    Optional<Invoice> findByBusinessIdAndInvoiceNumber(UUID businessId, String invoiceNumber);
}
