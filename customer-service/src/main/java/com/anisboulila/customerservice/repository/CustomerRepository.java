package com.anisboulila.customerservice.repository;

import com.anisboulila.customerservice.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
}
