package com.anisboulila.customerservice.service;

import com.anisboulila.customerservice.domain.Customer;
import com.anisboulila.customerservice.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class CustomerApplicationService {

    private final CustomerRepository customerRepository;
    private final Clock clock;

    public CustomerApplicationService(CustomerRepository customerRepository, Clock clock) {
        this.customerRepository = customerRepository;
        this.clock = clock;
    }

    // La sauvegarde et son résultat appartiennent à une même transaction.
    @Transactional
    public Customer createFromVerifiedIdentity(String sub, String name, String email) {
        Customer customer = Customer.create(sub, name, email, clock.instant());
        // Déclencher les contraintes avant la fin de la transaction pour traduire les conflits.
        return customerRepository.saveAndFlush(customer);
    }
}
