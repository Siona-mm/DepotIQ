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
        return nextUnusedCode("store_code_seq", "stores", "store_code", "S%03d");
    }

    public String nextProductCode() {
        return nextUnusedCode("product_code_seq", "products", "product_code", "P%04d");
    }

    public String nextShipmentNumber() {
        return "SHP-%d-%04d".formatted(
                Year.now().getValue(),
                nextValue("shipment_number_seq")
        );
    }

    // The sequence supplies concurrency-safe candidates. Skip codes created by earlier imports.
    private String nextUnusedCode(String sequence, String table, String column, String format) {
        while (true) {
            String candidate = format.formatted(nextValue(sequence));
            Boolean exists = jdbcTemplate.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM " + table + " WHERE " + column + " = ?)",
                    Boolean.class, candidate);
            if (Boolean.FALSE.equals(exists)) return candidate;
            if (exists == null) throw new IllegalStateException("Could not check generated business code");
        }
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
