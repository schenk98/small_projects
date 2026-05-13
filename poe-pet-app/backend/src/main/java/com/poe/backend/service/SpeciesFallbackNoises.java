package com.poe.backend.service;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Short in-character strings used when the AI gateway is unavailable or degenerates into the same style.
 * Kept in one place so we can detect when the model returns an exact match (treat as low-quality / fallback UX).
 */
public final class SpeciesFallbackNoises {
    private static final String[] DOG = new String[] {
        "*barks cheerfully*", "*woof?*", "*wags tail*", "*makes a curious noise*"
    };

    private static final Map<String, String[]> BY_SPECIES = Map.ofEntries(
            Map.entry("cat", new String[] { "*meows softly*", "*purrs*", "*mrrp?*", "*makes a curious cat noise*" }),
            Map.entry("penguin", new String[] {
                "*chirps thoughtfully*", "*waddles closer*", "*makes a tiny penguin peep*", "*flaps flippers softly*"
            }),
            Map.entry("fox", new String[] {
                "*yips cheekily*", "*swishes a fluffy tail*", "*gives a tiny fox giggle*", "*makes a playful fox sound*"
            }),
            Map.entry("hamster", new String[] {
                "*squeaks with full cheeks*", "*nibbles happily*", "*makes a tiny hamster peep*", "*wiggles its whiskers*"
            }),
            Map.entry("tiger", new String[] {
                "*makes a tiny tiger chuff*", "*swishes a striped tail*", "*practices a baby roar*", "*pads closer with cub paws*"
            }),
            Map.entry("lion", new String[] {
                "*gives a soft cub roar*", "*flicks a tufted tail*", "*puffs up its little mane*", "*nuzzles proudly*"
            }),
            Map.entry("horse", new String[] {
                "*nickers softly*", "*prances in place*", "*flicks a fluffy tail*", "*makes a tiny foal whinny*"
            }),
            Map.entry("parrot", new String[] {
                "*chirps brightly*", "*fluffs colorful feathers*", "*squawks hello softly*", "*tilts a curious beak*"
            }),
            Map.entry("unicorn", new String[] {
                "*whinnies with sparkles*", "*taps golden hooves*", "*shakes a pastel mane*", "*makes a tiny magical neigh*"
            }),
            Map.entry("midnight_cat", new String[] {
                "*purrs like distant stars*", "*swishes a galaxy tail*", "*mrrps mysteriously*", "*sparkles with midnight fur*"
            }),
            Map.entry("panda", new String[] {
                "*blinks cluelessly*", "*hugs a bamboo snack*", "*makes a tiny panda huff*", "*rolls around happily*"
            }),
            Map.entry("goldfish", new String[] {
                "*blubs happily*", "*swishes golden fins*", "*makes tiny aquarium bubbles*", "*circles the bowl cheerfully*"
            }),
            Map.entry("lizard", new String[] {
                "*blinks with tiny lizard eyes*", "*curls a green tail*", "*makes a soft reptile chirp*", "*scampers closer on tiny toes*"
            }));

    private SpeciesFallbackNoises() {
    }

    public static String randomPhrase(String speciesCode) {
        String[] options = optionsFor(speciesCode);
        return options[(int) (Math.random() * options.length)];
    }

    /** True when the model returned text identical to one of this species' canned noise lines. */
    public static boolean matchesFallbackPhrase(String speciesCode, String assistantText) {
        if (assistantText == null) {
            return false;
        }
        String t = assistantText.trim();
        if (t.isEmpty()) {
            return false;
        }
        for (String phrase : optionsFor(speciesCode)) {
            if (t.equals(phrase)) {
                return true;
            }
        }
        return false;
    }

    /** All distinct phrases (any species) — for diagnostics or tests. */
    public static Set<String> allPhrases() {
        return Stream.concat(Stream.of(DOG), BY_SPECIES.values().stream().flatMap(Arrays::stream))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String[] optionsFor(String speciesCode) {
        String s = speciesCode != null ? speciesCode.trim().toLowerCase(Locale.ROOT) : "";
        return BY_SPECIES.getOrDefault(s, DOG);
    }
}
