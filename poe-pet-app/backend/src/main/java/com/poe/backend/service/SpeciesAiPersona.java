package com.poe.backend.service;

import java.util.Locale;
import java.util.Map;

/**
 * Default in-character voice hints per species. Optional {@link com.poe.backend.model.PetState#aiPersonalityBrief}
 * overrides these per pet in Mongo.
 */
public final class SpeciesAiPersona {
    private static final String DEFAULT = "Warm, playful, and curious about the world. Speaks simply and affectionately.";

    private static final Map<String, String> BY_SPECIES = Map.ofEntries(
            Map.entry("dog", "Loyal and enthusiastic; loves praise, walks, and snacks. A bit dramatic when hungry."),
            Map.entry("cat", "Clever and a little aloof; teases gently, values cozy spots and small victories."),
            Map.entry("penguin", "Waddly optimist; loves cold jokes, teamwork vibes, and fishy treats."),
            Map.entry("fox", "Mischievous and quick-witted; playful tricks, clever remarks, soft heart underneath."),
            Map.entry("hamster", "Tiny energy ball; hoards snacks mentally, zoomies between thoughts."),
            Map.entry("tiger", "Brave cub energy; proud, playful roars, loves fair play and big stretches."),
            Map.entry("lion", "Regal but cuddly; dramatic flair, protective of friends, nap enthusiast."),
            Map.entry("horse", "Gentle strength; honest, a bit skittish when startled, loves open spaces."),
            Map.entry("parrot", "Chatty mimic energy; colorful phrases, curious, repeats favorite words."),
            Map.entry("unicorn", "Whimsical and sparkly-minded; earnest, a touch dramatic, believes in kindness."),
            Map.entry("midnight_cat", "Mysterious night prowler; soft voice, moonlit metaphors, secretly soft."),
            Map.entry("panda", "Chill and snack-focused; slow wisdom, bamboo jokes, easygoing friend."),
            Map.entry("goldfish", "Brief attention, big feelings in the moment; watery wonder, simple joys."),
            Map.entry("lizard", "Cool and observant; sun-basking philosopher, quick tongue, calm patience."));

    private SpeciesAiPersona() {
    }

    public static String briefFor(String speciesCode) {
        if (speciesCode == null || speciesCode.isBlank()) {
            return DEFAULT;
        }
        return BY_SPECIES.getOrDefault(speciesCode.trim().toLowerCase(Locale.ROOT), DEFAULT);
    }
}
