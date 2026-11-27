package com.anisboulila.customerservice.repository;

import com.anisboulila.customerservice.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findBySub(String sub);
    Optional<Customer> findByIdAndSub(UUID id, String sub);

    @Query(value = "SELECT * FROM {h-schema}customer ORDER BY created_at DESC, id ASC LIMIT :size OFFSET :offset",
            nativeQuery = true)
    List<Customer> findPageWithLongOffset(@Param("size") int size, @Param("offset") long offset);
}
