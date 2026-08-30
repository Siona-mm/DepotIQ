package com.depotiq.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;
import org.springframework.web.multipart.MultipartFile;

/** Parse and validate the entire file before callers change persistent entities. */
final class CsvImportSupport {
    private CsvImportSupport() {}

    static <T> List<T> read(MultipartFile file, List<String> columns, Function<CSVRecord, T> parse) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A non-empty CSV file is required.");
        }
        if (file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new IllegalArgumentException("Only CSV files are supported.");
        }
        try (var reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            reader.mark(1);
            if (reader.read() != '\ufeff') reader.reset();
            try (var parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(true).setIgnoreHeaderCase(true).setTrim(true)
                    .setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW).build().parse(reader)) {
                List<String> missing = columns.stream().filter(column -> parser.getHeaderMap().keySet()
                        .stream().noneMatch(column::equalsIgnoreCase)).toList();
                if (!missing.isEmpty()) {
                    throw new IllegalArgumentException("Missing required columns: " + String.join(", ", missing));
                }
                List<T> rows = new ArrayList<>();
                List<String> errors = new ArrayList<>();
                for (CSVRecord row : parser) {
                    try {
                        if (!row.isConsistent()) throw new IllegalArgumentException("Column count does not match the header.");
                        for (String column : columns) required(row, column);
                        rows.add(parse.apply(row));
                    } catch (IllegalArgumentException exception) {
                        if (errors.size() < 100) errors.add("Line " + (row.getRecordNumber() + 1) + ": " + exception.getMessage());
                    }
                }
                if (!errors.isEmpty()) {
                    throw new IllegalArgumentException("Nothing was imported. Fix these rows and upload again:\n" + String.join("\n", errors));
                }
                if (rows.isEmpty()) throw new IllegalArgumentException("The CSV must contain at least one data row.");
                return rows;
            }
        } catch (IOException | UncheckedIOException exception) {
            throw new IllegalArgumentException("The CSV could not be read. Check quoting and row structure.", exception);
        }
    }

    static String required(CSVRecord row, String column) {
        String value = row.isSet(column) ? row.get(column).trim() : "";
        if (value.isEmpty()) throw new IllegalArgumentException(column + " is required.");
        return value;
    }

    static String text(CSVRecord row, String column, int maxLength) {
        String value = required(row, column);
        if (value.length() > maxLength) throw new IllegalArgumentException(column + " must be at most " + maxLength + " characters.");
        return value;
    }

    static int integer(CSVRecord row, String column, int minimum) {
        try {
            int value = Integer.parseInt(required(row, column));
            if (value < minimum) throw new IllegalArgumentException(column + " must be at least " + minimum + ".");
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(column + " must be a whole number within the supported range.");
        }
    }

    static BigDecimal decimal(CSVRecord row, String column, int precision, int scale) {
        try {
            BigDecimal value = new BigDecimal(required(row, column));
            if (value.signum() < 0) throw new IllegalArgumentException(column + " cannot be negative.");
            if (value.stripTrailingZeros().scale() > scale || value.compareTo(BigDecimal.TEN.pow(precision - scale)) >= 0) {
                throw new IllegalArgumentException(column + " exceeds the supported size or decimal places (" + scale + ").");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(column + " must be a number.");
        }
    }

    static boolean bool(CSVRecord row, String column) {
        return switch (required(row, column).toLowerCase(Locale.ROOT)) {
            case "true", "yes", "1" -> true;
            case "false", "no", "0" -> false;
            default -> throw new IllegalArgumentException(column + " must be true or false.");
        };
    }

    static String fileName(MultipartFile file) {
        String name = file.getOriginalFilename().replace('\\', '/');
        return name.substring(name.lastIndexOf('/') + 1);
    }
}
