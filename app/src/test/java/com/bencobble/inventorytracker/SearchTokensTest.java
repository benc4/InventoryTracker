package com.bencobble.inventorytracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.bencobble.inventorytracker.util.SearchTokensBuilder;

import org.junit.Test;

import java.util.List;

// SearchTokensTest
// Tests the SearchTokensBuilder class
public class SearchTokensTest {

    // tokenizeName returns an empty list for null and blank names
    @Test
    public void tokenizeName_nullOrBlank_returnsEmptyList() {
        assertTrue(SearchTokensBuilder.tokenizeName(null).isEmpty());
        assertTrue(SearchTokensBuilder.tokenizeName("").isEmpty());
        assertTrue(SearchTokensBuilder.tokenizeName("   ").isEmpty());
    }

    // tokenizeName builds every prefix for the word in the name
    @Test
    public void tokenize_singleWord_emitsAllPrefixes() {
        List<String> tokens = SearchTokensBuilder.tokenizeName("Box");
        assertEquals(3, tokens.size());
        assertEquals("b", tokens.get(0));
        assertEquals("bo", tokens.get(1));
        assertEquals("box", tokens.get(2));
    }

    // tokenizeName lowercases the input so search queries can be matched case-insensitively
    @Test
    public void tokenize_lowercasesInput() {
        List<String> tokens = SearchTokensBuilder.tokenizeName("BOX");
        assertTrue(tokens.contains("b"));
        assertTrue(tokens.contains("box"));
        assertFalse(tokens.contains("B"));
        assertFalse(tokens.contains("BOX"));
    }

    // tokenizeName splits and separately tokenizes multi-word names
    @Test
    public void tokenize_multipleWords_emitsPrefixesPerWord() {
        List<String> tokens = SearchTokensBuilder.tokenizeName("Cardboard Box");
        assertTrue(tokens.contains("c"));
        assertTrue(tokens.contains("cardboard"));
        assertTrue(tokens.contains("b"));
        assertTrue(tokens.contains("box"));
    }

    // tokenizeName uses a set to prevent duplicate tokens
    @Test
    public void tokenize_dedupesSharedPrefixes() {
        List<String> tokens = SearchTokensBuilder.tokenizeName("Black Boxes");
        long countOfB = tokens.stream().filter(t -> t.equals("b")).count();
        assertEquals(1, countOfB);
    }

    // tokenizeName caps prefix length for long names
    @Test
    public void tokenize_capsLongWords() {
        // longName is 30 characters and the max is 25, so there should be 25 prefixes
        String longName = "abcdefghijklmnopqrstuvwxyz1234";
        List<String> tokens = SearchTokensBuilder.tokenizeName(longName);
        assertEquals(25, tokens.size());
        assertEquals("a", tokens.get(0));
        assertEquals("abcdefghijklmnopqrstuvwxy", tokens.get(24));
        assertThrows(java.lang.IndexOutOfBoundsException.class, () -> tokens.get(25));
    }

    // normalizeQuery converts the string to lowercase and trims leading/trailing whitespace
    @Test
    public void normalizeQuery_lowercasesAndTrims() {
        assertEquals("box", SearchTokensBuilder.normalizeQuery("  BOX  "));
        assertEquals("box", SearchTokensBuilder.normalizeQuery("Box"));
    }

    // normalizeQuery returns null for null/empty input
    @Test
    public void normalizeQuery_blankReturnsNull() {
        assertNull(SearchTokensBuilder.normalizeQuery(null));
        assertNull(SearchTokensBuilder.normalizeQuery(""));
    }
}
