package com.anisboulila.customerservice.api;

import com.anisboulila.customerservice.domain.CustomerName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonDeserialize(using = CreateCustomerRequestDeserializer.class)
public record CreateCustomerRequest(@NotBlank @Size(max = 120) String name) {

    public CreateCustomerRequest {
        name = CustomerName.normalize(name);
    }
}
