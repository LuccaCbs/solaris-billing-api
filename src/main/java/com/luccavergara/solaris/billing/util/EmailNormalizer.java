package com.luccavergara.solaris.billing.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class EmailNormalizer {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private EmailNormalizer() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }

        String value = raw.trim().toLowerCase(Locale.ROOT);

        while (!value.isEmpty() && isTrailingSeparator(value.charAt(value.length() - 1))) {
            value = value.substring(0, value.length() - 1);
        }

        return value;
    }

    public static boolean isValid(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    private static boolean isTrailingSeparator(char character) {
        return character == ':' || character == ';' || character == ',';
    }
}
