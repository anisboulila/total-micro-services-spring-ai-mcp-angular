package com.anisboulila.customerservice.service;

import com.anisboulila.customerservice.domain.Customer;
import com.anisboulila.customerservice.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class CustomerApplicationService {

    private final CustomerRepository customerRepository;
    private final Clock clock;

    public CustomerApplicationService(CustomerRepository customerRepository, Clock clock) {
        this.customerRepository = customerRepository;
        this.clock = clock;
    }

    public Customer createFromVerifiedIdentity(String sub, String name, String email) {
        Customer customer = Customer.create(sub, name, email, clock.instant());
        return customerRepository.save(customer);
    }
}
