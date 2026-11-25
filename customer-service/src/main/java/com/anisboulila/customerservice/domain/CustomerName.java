package com.anisboulila.customerservice.domain;

public final class CustomerName {

    private CustomerName() {
    }

    public static String normalize(String name) {
        return name == null ? null : name.strip();
    }
}
