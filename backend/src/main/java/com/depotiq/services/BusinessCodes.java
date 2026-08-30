package com.depotiq.services;

import java.math.BigInteger;

public final class BusinessCodes {
    private BusinessCodes() {}

    /** Accept legacy padding in CSV/model inputs; persisted store codes use at least three digits. */
    public static String normalizeStoreCode(String value) {
        if (value == null) return null;
        String code = value.trim();
        if (!code.matches("(?i)S[0-9]+")) return code;
        String number = new BigInteger(code.substring(1)).toString();
        return "S" + "0".repeat(Math.max(0, 3 - number.length())) + number;
    }

    public static int compareStoreCodes(String left, String right) {
        boolean leftNumeric = left.matches("S[0-9]+");
        boolean rightNumeric = right.matches("S[0-9]+");
        if (leftNumeric != rightNumeric) return leftNumeric ? -1 : 1;
        if (leftNumeric) {
            return new BigInteger(left.substring(1)).compareTo(new BigInteger(right.substring(1)));
        }
        return left.compareToIgnoreCase(right);
    }
}
