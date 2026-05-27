package com.bencobble.inventorytracker.util;

// ParseIntHelper utility helper class
public class ParseIntHelper {
    // Parses a quantity string to an integer
    // Returns the parsed integer on success
    // Returns -1 on NumberFormatException
    public static int parseQuantity (String quantityStr) {
        try {
            return Integer.parseInt(quantityStr.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
