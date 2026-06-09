package com.ecommerce.rag.rag.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SoftSemanticLexiconTest {

    private SoftSemanticLexicon lexicon;

    @BeforeEach
    void setUp() {
        lexicon = new SoftSemanticLexicon();
        lexicon.init();
    }

    @Test
    void studentPartyShouldReturnCostEffectiveWords() {
        List<String> keywords = lexicon.lookup("学生党耳机");
        assertNotNull(keywords);
        assertTrue(keywords.contains("性价比"));
        assertTrue(keywords.contains("低价"));
        assertTrue(keywords.contains("实用"));
    }

    @Test
    void commuteShouldReturnCommuteWords() {
        List<String> keywords = lexicon.lookup("通勤背包");
        assertTrue(keywords.contains("轻便"));
        assertTrue(keywords.contains("大容量"));
    }

    @Test
    void sensitiveSkinShouldReturnSoothingWords() {
        List<String> keywords = lexicon.lookup("敏感肌洗面奶");
        assertTrue(keywords.contains("温和"));
        assertTrue(keywords.contains("舒缓"));
    }

    @Test
    void noMatchShouldReturnEmptyList() {
        List<String> keywords = lexicon.lookup("xyzzy123nonexistent");
        assertTrue(keywords.isEmpty());
    }

    @Test
    void maxKeywordsShouldLimitResults() {
        List<String> keywords = lexicon.lookup("学生党", 2);
        assertTrue(keywords.size() <= 2);
    }

    @Test
    void nullQueryShouldReturnEmptyList() {
        List<String> keywords = lexicon.lookup(null);
        assertTrue(keywords.isEmpty());
    }

    @Test
    void giftQueryShouldReturnGiftWords() {
        List<String> keywords = lexicon.lookup("送礼用的护肤品");
        assertTrue(keywords.contains("礼盒") || keywords.contains("适合送人"));
    }

    @Test
    void lexiconShouldHaveMinimumEntries() {
        assertTrue(lexicon.size() >= 10, "Lexicon should have at least 10 entries");
    }
}
