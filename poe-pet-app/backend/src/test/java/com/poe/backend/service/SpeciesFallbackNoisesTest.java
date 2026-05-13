package com.poe.backend.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpeciesFallbackNoisesTest {

    @Test
    void matchesDogFallbackPhrases() {
        assertTrue(SpeciesFallbackNoises.matchesFallbackPhrase("dog", "*wags tail*"));
        assertTrue(SpeciesFallbackNoises.matchesFallbackPhrase("dog", "  *woof?*  "));
        assertFalse(SpeciesFallbackNoises.matchesFallbackPhrase("dog", "I wag my tail because I'm happy!"));
    }

    @Test
    void goldfishDoesNotMatchDogTail() {
        assertFalse(SpeciesFallbackNoises.matchesFallbackPhrase("goldfish", "*wags tail*"));
        assertTrue(SpeciesFallbackNoises.matchesFallbackPhrase("goldfish", "*blubs happily*"));
    }
}
