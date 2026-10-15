package com.aichatbot.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Text clean-up used before keyword matching: lower-case, strip punctuation, split into words. */
public final class TextNormalizer {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "is", "are", "am", "i", "you", "me", "my", "we", "to", "of", "in", "on",
            "for", "and", "or", "do", "does", "can", "how", "what", "please", "with", "it", "your",
            "our", "be", "about", "would", "will", "s", "t");

    private TextNormalizer() {
    }

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** Words of an already normalized text, without stop words. */
    public static List<String> tokens(String normalized) {
        List<String> words = new ArrayList<>();
        for (String word : normalized.split(" ")) {
            if (!word.isEmpty() && !STOP_WORDS.contains(word)) {
                words.add(word);
            }
        }
        return words;
    }
}
