package com.fintruth;

import java.util.*;
import java.util.regex.*;

/** Deterministic, auditable Java implementation of the FinTruth research rules. */
public final class FinTruth {
    private FinTruth() {}
    public record Opinion(String agent, double score) {}
    public record Forecast(double probabilityDown, double reaction1d, double reaction5d, double reaction20d) {}
    public record Verdict(String documentId, double factScore, double numericScore,
                          double toneScore, double tau, List<String> riskTags, String explanation,
                          Forecast forecast) {}

    private static final String[] RED_FLAGS = {"guaranteed", "risk-free", "zero downside", "insider", "definitely", "必将", "稳赚", "无风险", "内幕"};
    private static final String[] NEGATIVE = {"investigation", "fraud", "default", "probe", "调查", "造假", "违约", "问询"};
    private static final String[] HYPE = {"explosive", "unprecedented", "skyrocket", "暴涨", "颠覆"};
    private static final String[] BASELINE = {"compared", "versus", "vs.", "year-over-year", "yoy", "同比", "环比", "相较", "相比"};
    private static final Pattern NUMBER = Pattern.compile("\\d+(?:\\.\\d+)?");
    private static final Pattern SPLIT = Pattern.compile("[.!?;。！？；]+\\s*");

    public static List<String> extractClaims(String text) {
        if (text == null) throw new IllegalArgumentException("text cannot be null");
        List<String> claims = new ArrayList<>();
        for (String part : SPLIT.split(text.strip())) if (part.strip().length() >= 4) claims.add(part.strip());
        return List.copyOf(claims);
    }
    private static int countTerms(String text, String[] terms) {
        int hits = 0;
        String lower = text.toLowerCase(Locale.ROOT);
        for (String term : terms) if (lower.contains(term.toLowerCase(Locale.ROOT))) hits++;
        return hits;
    }
    public static Verdict analyze(String documentId, String text) {
        if (documentId == null || documentId.isBlank()) throw new IllegalArgumentException("documentId required");
        List<String> claims = extractClaims(text);
        double fact = Math.min(1.0, 0.25 * countTerms(text, RED_FLAGS) + (claims.size() > 5 ? 0.15 : 0));
        int numericalClaims = 0;
        boolean missingBaseline = false;
        for (String claim : claims) {
            if (NUMBER.matcher(claim).find()) {
                numericalClaims++;
                if (claim.contains("%") && countTerms(claim, BASELINE) == 0) missingBaseline = true;
            }
        }
        double numeric = missingBaseline ? 0.4 : 0.1 * Math.min(numericalClaims, 3);
        double tone = Math.min(1.0, 0.35 * countTerms(text, NEGATIVE) + 0.25 * countTerms(text, HYPE));
        double tau = (fact + numeric + tone) / 3.0;
        List<String> tags = new ArrayList<>();
        if (fact >= 0.30) tags.add("language_red_flag");
        if (numeric >= 0.30) tags.add("numeric_ambiguity");
        if (tone >= 0.30) tags.add("tone_extreme");
        double pDown = 1.0 / (1.0 + Math.exp(-4.0 * (tau - 0.4)));
        Forecast forecast = new Forecast(pDown, -0.002 * tau,
                -0.008 * tau * (0.5 + pDown), -0.012 * tau * (0.4 + pDown));
        String explanation = String.format(Locale.ROOT,
                "Fact risk %.3f, numerical ambiguity %.3f, tone risk %.3f; tau %.3f. Rule-based research indicator, not a verified fraud determination.",
                fact, numeric, tone, tau);
        return new Verdict(documentId, fact, numeric, tone, tau, List.copyOf(tags), explanation, forecast);
    }
    public static double dictionarySentiment(String text) {
        int pos = countTerms(text, new String[]{"growth", "improve", "steady", "增长", "改善", "稳健"});
        int neg = countTerms(text, new String[]{"risk", "inquiry", "probe", "调查", "问询", "风险"});
        return (double)(pos - neg) / Math.max(pos + neg, 1);
    }
}
