package com.quickserve.modules.crm.service;

import com.quickserve.common.exception.BusinessException;
import com.quickserve.common.exception.ResourceNotFoundException;
import com.quickserve.common.exception.TenantAccessException;
import com.quickserve.common.response.PagedResponse;
import com.quickserve.common.security.TenantContext;
import com.quickserve.modules.crm.entity.Customer;
import com.quickserve.modules.crm.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrmService {

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public PagedResponse<Customer> getCustomers(int page, int size) {
        return PagedResponse.from(customerRepository.findByBusinessId(
                TenantContext.getBusinessId(), PageRequest.of(page, size)));
    }

    @Transactional
    public Customer createCustomer(Customer customer) {
        UUID businessId = TenantContext.getBusinessId();
        if (customer.getPhone() != null
                && customerRepository.existsByBusinessIdAndPhone(businessId, customer.getPhone())) {
            throw new BusinessException("Customer with this phone already exists", HttpStatus.CONFLICT);
        }
        customer.setBusinessId(businessId);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateCustomer(UUID id, Customer updates) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        verifyTenant(customer.getBusinessId());
        if (updates.getName() != null)  customer.setName(updates.getName());
        if (updates.getPhone() != null) customer.setPhone(updates.getPhone());
        if (updates.getEmail() != null) customer.setEmail(updates.getEmail());
        if (updates.getNotes() != null) customer.setNotes(updates.getNotes());
        return customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public List<Customer> searchCustomers(String query) {
        return customerRepository.searchByNameOrPhone(TenantContext.getBusinessId(), query);
    }

    @Transactional
    public Customer earnLoyaltyPoints(UUID customerId, BigDecimal orderAmount) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
        verifyTenant(customer.getBusinessId());

        // 1 point per ₹10 spent
        int points = orderAmount.divide(BigDecimal.TEN, 0, java.math.RoundingMode.FLOOR).intValue();
        customer.setLoyaltyPoints(customer.getLoyaltyPoints() + points);
        customer.setTotalSpend(customer.getTotalSpend().add(orderAmount));
        customer.setVisitCount(customer.getVisitCount() + 1);
        customer.setLastVisitAt(java.time.Instant.now());
        // Update tier
        updateTier(customer);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer redeemLoyaltyPoints(UUID customerId, int pointsToRedeem) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
        verifyTenant(customer.getBusinessId());
        if (customer.getLoyaltyPoints() < pointsToRedeem) {
            throw new BusinessException("Insufficient loyalty points. Available: " + customer.getLoyaltyPoints());
        }
        customer.setLoyaltyPoints(customer.getLoyaltyPoints() - pointsToRedeem);
        return customerRepository.save(customer);
    }

    private void updateTier(Customer customer) {
        int points = customer.getLoyaltyPoints();
        Customer.CustomerTier tier = Customer.CustomerTier.BRONZE;
        if (points >= 5000) tier = Customer.CustomerTier.PLATINUM;
        else if (points >= 2000) tier = Customer.CustomerTier.GOLD;
        else if (points >= 500)  tier = Customer.CustomerTier.SILVER;
        customer.setTier(tier);
    }

    private void verifyTenant(UUID resourceBusinessId) {
        if (!TenantContext.getBusinessId().equals(resourceBusinessId)) {
            throw new TenantAccessException();
        }
    }
}
