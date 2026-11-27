package com.anisboulila.customerservice.service;

import com.anisboulila.customerservice.domain.Customer;
import com.anisboulila.customerservice.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

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

    @Transactional(readOnly = true)
    public Optional<Customer> findByVerifiedSubject(String sub) {
        return customerRepository.findBySub(sub);
    }

    @Transactional(readOnly = true)
    public Optional<Customer> findReadable(UUID id, String sub, boolean operator) {
        return operator ? customerRepository.findById(id) : customerRepository.findByIdAndSub(id, sub);
    }

    @Transactional(readOnly = true)
    public Page<Customer> listForOperator(int page, int size) {
        PageRequest request = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")));
        // JPA limite les offsets à un entier ; PostgreSQL accepte un offset long.
        if (request.getOffset() > Integer.MAX_VALUE) {
            return new PageImpl<>(customerRepository.findPageWithLongOffset(size, request.getOffset()),
                    request, customerRepository.count());
        }
        return customerRepository.findAll(request);
    }
}
