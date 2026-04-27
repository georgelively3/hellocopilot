package com.example.hellocopilot.util;

public class StringUtil {

    public static String processProductName(String name) {
        if (name == null) {
            return "";
        }
        if (name.isEmpty()) {
            return "";
        }
        if (name.length() > 100) {
            name = name.substring(0, 100);
        }

        // Check for illegal characters
        StringBuilder cleaned = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_') {
                cleaned.append(c);
            } else if (c == '&') {
                cleaned.append("and");
            } else if (c == '@') {
                cleaned.append("at");
            } else {
                cleaned.append(' ');
            }
        }

        // Collapse multiple spaces
        String collapsed = cleaned.toString().trim();
        while (collapsed.contains("  ")) {
            collapsed = collapsed.replace("  ", " ");
        }

        // Title-case every word
        String[] words = collapsed.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
                result.append(" ");
            }
        }

        return result.toString().trim();
    }

    public static String formatProductLabel(String name, String description,
            String category, String sku, double price, int quantity,
            boolean available) {
        if (name == null || name.isEmpty()) { // duplicated null check (smell 3)
            return "";
        }
        String status = available ? "IN STOCK" : "OUT OF STOCK";
        return String.format("[%s] %s | %s | SKU: %s | $%.2f | Qty: %d | %s",
                category, name, description, sku, price, quantity, status);
    }

    public static boolean isValidName(String s) {
        if (s == null || s.isEmpty()) { // duplicated
            return false;
        }
        return s.matches("[a-zA-Z0-9 \\-_]+");
    }

    public static String truncate(String s, int maxLength) {
        if (s == null || s.isEmpty()) { // duplicated
            return "";
        }
        if (s.length() <= maxLength) {
            return s;
        }
        return s.substring(0, maxLength) + "...";
    }

    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) { // duplicated
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    public static String get_trimmed(String input) {
        if (input == null || input.isEmpty()) { // duplicated
            return "";
        }
        return input.trim();
    }

    public static String CleanInput(String input) {
        if (input == null || input.isEmpty()) { // duplicated
            return "";
        }
        return input.trim().toLowerCase();
    }

    public static String rmSpecialChars(String input) {
        if (input == null || input.isEmpty()) { // duplicated
            return "";
        }
        return input.replaceAll("[^a-zA-Z0-9 ]", "");
    }
}
