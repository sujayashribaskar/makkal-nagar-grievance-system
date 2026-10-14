package com.makkalnagar.service;

import com.makkalnagar.entity.ComplaintEntity;
import com.makkalnagar.enums.Category;
import com.makkalnagar.enums.DuplicateStatus;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Intelligent Duplicate Detection Engine
 *
 * Implements automated, multi-factor duplicate comparison based on:
 * 1. Normalized municipal ward/location.
 * 2. Natural language text-similarity with stop-word pruning, synonym canonization,
 *    and typo-tolerant token matching (Levenshtein distance).
 * 3. Complaint category and shared open-issue references.
 *
 * Confidence Score Ranges:
 * - [0.85 - 1.00]: High Confidence / Confirmed Duplicate
 * - [0.60 - 0.84]: Possible Duplicate
 * - [< 0.60]: Unrelated or insufficient similarity (not flagged)
 */
@Component
public class DuplicateDetectionEngine {

    public static final double DUPLICATE_THRESHOLD = 0.60;
    public static final double HIGH_CONFIDENCE_THRESHOLD = 0.85;

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are",
            "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but",
            "by", "can", "could", "did", "do", "does", "doing", "down", "during", "each", "few", "for",
            "from", "further", "had", "has", "have", "having", "he", "her", "here", "hers", "herself",
            "him", "himself", "his", "how", "i", "if", "in", "into", "is", "it", "its", "itself",
            "just", "me", "more", "most", "my", "myself", "no", "nor", "not", "now", "of", "off",
            "on", "once", "only", "or", "other", "our", "ours", "ourselves", "out", "over", "own",
            "same", "she", "should", "so", "some", "such", "than", "that", "the", "their", "theirs",
            "them", "themselves", "then", "there", "these", "they", "this", "those", "through", "to",
            "too", "under", "until", "up", "very", "was", "we", "were", "what", "when", "where",
            "which", "while", "who", "whom", "why", "with", "would", "you", "your", "yours",
            // Municipal filler words that do not indicate identical real-world defects
            "please", "kindly", "urgent", "urgently", "issue", "problem", "complaint", "facing",
            "locality", "area", "street", "road", "ward", "nagar", "colony", "side", "corner",
            "near", "opposite", "around", "since", "days", "day", "yesterday", "today", "help",
            "fix", "resolve", "municipal", "corporation", "people", "resident", "residents",
            "causing", "caused", "also", "due"
    );

    // Canonical synonym map
    private static final Map<String, String> CANONICAL_TERMS = Map.ofEntries(
            Map.entry("potholes", "pothole"),
            Map.entry("crater", "pothole"),
            Map.entry("craters", "pothole"),
            Map.entry("leakage", "leak"),
            Map.entry("leaking", "leak"),
            Map.entry("leaks", "leak"),
            Map.entry("leaked", "leak"),
            Map.entry("bursts", "burst"),
            Map.entry("bursting", "burst"),
            Map.entry("ruptured", "burst"),
            Map.entry("sparking", "spark"),
            Map.entry("sparks", "spark"),
            Map.entry("flickering", "flicker"),
            Map.entry("flickers", "flicker"),
            Map.entry("overflowing", "overflow"),
            Map.entry("overflows", "overflow"),
            Map.entry("garbage", "garbage"),
            Map.entry("trash", "garbage"),
            Map.entry("waste", "garbage"),
            Map.entry("rubbish", "garbage"),
            Map.entry("dumper", "bin"),
            Map.entry("dumpers", "bin"),
            Map.entry("bins", "bin"),
            Map.entry("drain", "drainage"),
            Map.entry("gutter", "drainage"),
            Map.entry("gutters", "drainage"),
            Map.entry("sewage", "sewage"),
            Map.entry("sewer", "sewage"),
            Map.entry("streetlights", "streetlight"),
            Map.entry("light", "streetlight"),
            Map.entry("lights", "streetlight"),
            Map.entry("lamp", "streetlight"),
            Map.entry("lamps", "streetlight"),
            Map.entry("water", "water"),
            Map.entry("pipeline", "pipeline"),
            Map.entry("pipe", "pipeline"),
            Map.entry("pipes", "pipeline"),
            Map.entry("lines", "pipeline"),
            Map.entry("line", "pipeline"),
            Map.entry("wire", "wire"),
            Map.entry("wires", "wire"),
            Map.entry("cable", "wire"),
            Map.entry("cables", "wire"),
            Map.entry("broken", "damage"),
            Map.entry("damaged", "damage"),
            Map.entry("damaging", "damage"),
            Map.entry("blocked", "block"),
            Map.entry("blocking", "block"),
            Map.entry("blockage", "block")
    );

    public static class DetectionResult {
        private final boolean duplicate;
        private final double confidenceScore;
        private final DuplicateStatus status;
        private final String reason;
        private final String similarityDetails;
        private final List<String> sharedKeywords;

        public DetectionResult(boolean duplicate, double confidenceScore, DuplicateStatus status,
                               String reason, String similarityDetails, List<String> sharedKeywords) {
            this.duplicate = duplicate;
            this.confidenceScore = Math.round(confidenceScore * 100.0) / 100.0;
            this.status = status;
            this.reason = reason;
            this.similarityDetails = similarityDetails;
            this.sharedKeywords = sharedKeywords;
        }

        public boolean isDuplicate() { return duplicate; }
        public double getConfidenceScore() { return confidenceScore; }
        public DuplicateStatus getStatus() { return status; }
        public String getReason() { return reason; }
        public String getSimilarityDetails() { return similarityDetails; }
        public List<String> getSharedKeywords() { return sharedKeywords; }
    }

    /**
     * Compares a new complaint against an existing complaint and computes duplicate confidence.
     */
    public DetectionResult evaluate(ComplaintEntity newComp, ComplaintEntity existingComp) {
        // 1. Normalized Location check
        String normLocNew = normalizeLocation(newComp.getLocation());
        String normLocExist = normalizeLocation(existingComp.getLocation());
        boolean sameLocation = !normLocNew.isEmpty() && normLocNew.equals(normLocExist);

        // 2. Category check
        boolean sameCategory = newComp.getCategory() == existingComp.getCategory();

        // 3. Open Issue Reference check
        String refNew = newComp.getOpenIssueRef() != null ? newComp.getOpenIssueRef().trim() : null;
        String refExist = existingComp.getOpenIssueRef() != null ? existingComp.getOpenIssueRef().trim() : null;
        boolean hasRef = refNew != null && !refNew.isEmpty() && refExist != null && !refExist.isEmpty();
        boolean sameIssueRef = hasRef && refNew.equalsIgnoreCase(refExist);

        // 4. Description Text Similarity
        Set<String> keywordsNew = extractKeywords(newComp.getDescription());
        Set<String> keywordsExist = extractKeywords(existingComp.getDescription());

        List<String> sharedKeywords = findSharedKeywords(keywordsNew, keywordsExist);
        double descSim = computeTextSimilarity(newComp.getDescription(), existingComp.getDescription(),
                keywordsNew, keywordsExist, sharedKeywords);

        // 5. Confidence Score Computation
        double confidence;
        String matchReason;
        String similarityDetails;

        if (sameIssueRef) {
            if (sameCategory && sameLocation) {
                // High confidence duplicate when matching issue ref + category + location
                confidence = 0.95 + 0.05 * descSim;
                matchReason = String.format("Confirmed duplicate: Matches Open Issue Reference [%s], Category (%s), and Location (%s).",
                        refNew, newComp.getCategory().getDisplayName(), newComp.getLocation());
            } else if (sameCategory) {
                // Same reference and category but different location -> separate incidents across wards
                confidence = 0.45;
                matchReason = String.format("Shared issue reference [%s] reported at a different location (%s vs %s).",
                        refNew, newComp.getLocation(), existingComp.getLocation());
            } else {
                confidence = 0.30;
                matchReason = "Shared issue reference between different categories.";
            }
        } else {
            if (!sameLocation) {
                // Different locations -> separate physical municipal defects
                confidence = (sameCategory ? 0.20 : 0.0) + (descSim * 0.30);
                confidence = Math.min(confidence, 0.50); // Capped below duplicate threshold
                matchReason = String.format("Similar issue description reported at a different municipal location (%s vs %s).",
                        newComp.getLocation(), existingComp.getLocation());
            } else if (!sameCategory) {
                // Same location but different municipal departments
                confidence = 0.30 + (descSim * 0.25);
                confidence = Math.min(confidence, 0.55);
                matchReason = String.format("Reported at same location (%s) but belongs to distinct categories (%s vs %s).",
                        newComp.getLocation(), newComp.getCategory(), existingComp.getCategory());
            } else {
                // Same location AND Same category!
                if (descSim >= 0.99) {
                    confidence = 1.0;
                    matchReason = String.format("High-confidence confirmed duplicate: Identical description at same location (%s) and category (%s). Shared keywords: %s.",
                            newComp.getLocation(), newComp.getCategory().getDisplayName(), sharedKeywords);
                } else if (descSim < 0.20 || sharedKeywords.size() < 2) {
                    // Same location and category, but unrelated defects (e.g. pothole vs sign board)
                    // Rule: Do not classify complaints as duplicates based only on a single shared common word!
                    confidence = 0.15 * descSim;
                    matchReason = String.format("Same location (%s) and category (%s), but complaints describe unrelated issues.",
                            newComp.getLocation(), newComp.getCategory().getDisplayName());
                } else {
                    confidence = 0.25 + 0.35 + (0.40 * descSim);
                    if (descSim >= 0.85) {
                        matchReason = String.format("High-confidence duplicate: Highly similar description at same location (%s) and category (%s). Shared keywords: %s.",
                                newComp.getLocation(), newComp.getCategory().getDisplayName(), sharedKeywords);
                    } else {
                        matchReason = String.format("Possible duplicate: Similar description at same location (%s) and category (%s). Shared keywords: %s.",
                                newComp.getLocation(), newComp.getCategory().getDisplayName(), sharedKeywords);
                    }
                }
            }
        }

        similarityDetails = String.format("Category Match: %s | Location Match: %s (%s) | Text Similarity: %.1f%% | Overlapping Keywords: %s",
                sameCategory ? "YES" : "NO",
                sameLocation ? "YES" : "NO",
                newComp.getLocation(),
                descSim * 100.0,
                sharedKeywords.isEmpty() ? "None" : String.join(", ", sharedKeywords));

        boolean isDup = confidence >= DUPLICATE_THRESHOLD;
        DuplicateStatus status = confidence >= HIGH_CONFIDENCE_THRESHOLD ?
                DuplicateStatus.CONFIRMED_DUPLICATE : (isDup ? DuplicateStatus.POSSIBLE_DUPLICATE : DuplicateStatus.DISMISSED);

        return new DetectionResult(isDup, confidence, status, matchReason, similarityDetails, sharedKeywords);
    }

    public static String normalizeLocation(String loc) {
        if (loc == null) return "";
        return loc.trim().toLowerCase().replaceAll("[^a-z0-9]+", " ").replaceAll("\\s+", " ").trim();
    }

    public Set<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) return Collections.emptySet();
        String cleaned = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
        String[] tokens = cleaned.split("\\s+");
        Set<String> keywords = new LinkedHashSet<>();
        for (String t : tokens) {
            String trimmed = t.trim();
            if (trimmed.length() >= 3 && !STOP_WORDS.contains(trimmed)) {
                String canonical = CANONICAL_TERMS.getOrDefault(trimmed, trimmed);
                keywords.add(canonical);
            }
        }
        return keywords;
    }

    private List<String> findSharedKeywords(Set<String> k1, Set<String> k2) {
        List<String> shared = new ArrayList<>();
        for (String w1 : k1) {
            for (String w2 : k2) {
                if (w1.equals(w2) || isFuzzyMatch(w1, w2)) {
                    if (!shared.contains(w1)) {
                        shared.add(w1);
                    }
                    break;
                }
            }
        }
        return shared;
    }

    private boolean isFuzzyMatch(String w1, String w2) {
        if (w1.equals(w2)) return true;
        int len1 = w1.length();
        int len2 = w2.length();
        if (Math.abs(len1 - len2) > 2) return false;
        if (len1 >= 5 && len2 >= 5) {
            return computeLevenshteinDistance(w1, w2) <= (Math.min(len1, len2) >= 7 ? 2 : 1);
        }
        return false;
    }

    private double computeTextSimilarity(String d1, String d2, Set<String> k1, Set<String> k2, List<String> sharedKeywords) {
        String clean1 = d1.trim().toLowerCase();
        String clean2 = d2.trim().toLowerCase();

        if (clean1.equals(clean2)) {
            return 1.0;
        }

        if (k1.isEmpty() || k2.isEmpty()) {
            return 0.0;
        }

        // Rule: Do not classify complaints as duplicates based only on a shared common word
        if (sharedKeywords.size() <= 1 && clean1.length() > 20 && clean2.length() > 20) {
            // Check if string distance is very high
            double rawStringSim = 1.0 - ((double) computeLevenshteinDistance(clean1, clean2) / Math.max(clean1.length(), clean2.length()));
            if (rawStringSim < 0.85) {
                return 0.0;
            }
        }

        // 1. Jaccard similarity on normalized keywords
        int unionSize = k1.size() + k2.size() - sharedKeywords.size();
        double jaccard = unionSize > 0 ? (double) sharedKeywords.size() / unionSize : 0.0;

        // 2. Normalized token-sort Levenshtein similarity
        List<String> sorted1 = new ArrayList<>(k1);
        Collections.sort(sorted1);
        List<String> sorted2 = new ArrayList<>(k2);
        Collections.sort(sorted2);

        String s1 = String.join(" ", sorted1);
        String s2 = String.join(" ", sorted2);
        int maxLen = Math.max(s1.length(), s2.length());
        double tokenSortSim = maxLen > 0 ? 1.0 - ((double) computeLevenshteinDistance(s1, s2) / maxLen) : 0.0;

        return (0.65 * jaccard) + (0.35 * Math.max(0.0, tokenSortSim));
    }

    public static int computeLevenshteinDistance(CharSequence s1, CharSequence s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        int[] prev = new int[len2 + 1];
        int[] curr = new int[len2 + 1];

        for (int j = 0; j <= len2; j++) prev[j] = j;

        for (int i = 1; i <= len1; i++) {
            curr[0] = i;
            for (int j = 1; j <= len2; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            System.arraycopy(curr, 0, prev, 0, len2 + 1);
        }
        return prev[len2];
    }
}
