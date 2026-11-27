package com.anisboulila.customerservice.repository;

import com.anisboulila.customerservice.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findBySub(String sub);
}
