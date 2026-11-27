package com.anisboulila.customerservice.api;

import com.anisboulila.customerservice.error.ApiProblemException;
import com.anisboulila.customerservice.service.CustomerApplicationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerListController {

    private final CustomerIdentityValidator identity;
    private final CustomerApplicationService customers;
    private final int defaultSize;
    private final int maxSize;

    public CustomerListController(CustomerIdentityValidator identity, CustomerApplicationService customers,
                                  @Value("${customer.pagination.default-size:20}") int defaultSize,
                                  @Value("${customer.pagination.max-size:100}") int maxSize) {
        if (defaultSize < 1 || maxSize < 1 || defaultSize > maxSize) {
            throw new IllegalArgumentException("Invalid customer pagination configuration");
        }
        this.identity = identity;
        this.customers = customers;
        this.defaultSize = defaultSize;
        this.maxSize = maxSize;
    }

    @GetMapping
    public CustomerPage list(@AuthenticationPrincipal Jwt jwt,
                             @RequestParam(required = false) String page,
                             @RequestParam(required = false) String size) {
        String sub = identity.validatedSubject(jwt);
        int pageNumber = parse(page, 0, "INVALID_PAGE", false);
        int pageSize = parse(size, defaultSize, "INVALID_PAGE_SIZE", true);
        if (pageSize > maxSize) {
            throw invalid("INVALID_PAGE_SIZE");
        }
        Optional<CustomerResponse> result = customers.findByVerifiedSubject(sub).map(CustomerResponse::from);
        // L'unicité de sub limite le résultat à une seule fiche.
        List<CustomerResponse> content = pageNumber == 0 ? result.stream().toList() : List.of();
        int total = result.isPresent() ? 1 : 0;
        return new CustomerPage(content, pageNumber, pageSize, total, total);
    }

    private int parse(String value, int fallback, String code, boolean positive) {
        if (value == null) {
            return fallback;
        }
        try {
            if (!value.matches("[0-9]+")) {
                throw invalid(code);
            }
            int number = Integer.parseInt(value);
            if (positive && number == 0) {
                throw invalid(code);
            }
            return number;
        } catch (NumberFormatException exception) {
            throw invalid(code);
        }
    }

    private ApiProblemException invalid(String code) {
        return new ApiProblemException(HttpStatus.BAD_REQUEST, code,
                "Invalid pagination", "The pagination parameter is invalid.");
    }

    public record CustomerPage(List<CustomerResponse> content, int page, int size,
                               long totalElements, int totalPages) {
    }
}
