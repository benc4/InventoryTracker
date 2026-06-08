package com.bencobble.inventorytracker.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// SearchTokensBuilder utility class
// Generates the searchTokens prefixes for each Item
// Used for search filtering for item names through Firebase whereArrayContains queries
public final class SearchTokensBuilder {

    // Maximum number of prefixes for an item
    // Names over 25 characters will only have the first 25 characters tokenized
    private static final int MAX_PREFIX_LENGTH = 25;

    // Constructor
    private SearchTokensBuilder() {}

    // tokenizeName method
    // Takes the item name as a parameter
    // Returns the list of prefix tokens for that name
    public static List<String> tokenizeName(String name) {
        // Null/empty check
        if (name == null || name.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // Set to prevent duplicate tokens
        Set<String> tokens = new LinkedHashSet<>();

        // Sanitize the name and split up any words separated by a space
        String[] words = name.trim().toLowerCase().split("\\s+");

        // Loop through each word in the name to generate their prefix tokens
        for (String word : words) {
            // Limit the max prefix length to either the length of the word or the max length
            int limit = Math.min(word.length(), MAX_PREFIX_LENGTH);

            // Create prefix tokens for the word and add them to the set
            for (int i = 1; i <= limit; i++) {
                tokens.add(word.substring(0, i));
            }
        }

        return new ArrayList<>(tokens);
    }

    // normalizeQuery method
    // Takes the search query as a parameter
    // Returns a Lowercased and trimmed version of the query to match the Item's tokens
    // Returns null if the query is null/empty
    public static String normalizeQuery(String query) {
        if (query == null) return null;
        String trimmed = query.trim().toLowerCase();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
