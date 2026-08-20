package com.depotiq.services;

import java.time.Year;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class BusinessCodeGenerator {
    private final JdbcTemplate jdbcTemplate;

    public BusinessCodeGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String nextStoreCode() {
        return "S%04d".formatted(nextValue("store_code_seq"));
    }

    public String nextProductCode() {
        return "P%04d".formatted(nextValue("product_code_seq"));
    }

    public String nextShipmentNumber() {
        return "SHP-%d-%04d".formatted(
                Year.now().getValue(),
                nextValue("shipment_number_seq")
        );
    }

    private long nextValue(String sequenceName) {
        Long value = jdbcTemplate.queryForObject(
                "SELECT nextval('" + sequenceName + "')",
                Long.class
        );

        if (value == null) {
            throw new IllegalStateException("Could not generate the next business code");
        }

        return value;
    }
}
