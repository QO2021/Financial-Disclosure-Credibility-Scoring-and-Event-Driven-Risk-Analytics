package com.fintruth;

import java.time.LocalDate;
import java.util.*;

/** Market-adjusted cumulative abnormal returns using chronological trading-day observations. */
public final class EventStudy {
    private EventStudy() {}
    public record Price(LocalDate date, String instrument, double stockReturn, double marketReturn) {}
    public static double car(List<Price> prices, String instrument, LocalDate eventDate, int horizon) {
        if (horizon < 1) throw new IllegalArgumentException("horizon must be positive");
        List<Price> ordered = prices.stream().filter(p -> p.instrument().equals(instrument))
                .sorted(Comparator.comparing(Price::date)).toList();
        int start = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (!ordered.get(i).date().isBefore(eventDate)) { start = i; break; }
        }
        if (start < 0 || start + horizon > ordered.size()) return Double.NaN;
        double result = 0;
        for (int i = start; i < start + horizon; i++)
            result += ordered.get(i).stockReturn() - ordered.get(i).marketReturn();
        return result;
    }
    public static double spearman(double[] x, double[] y) {
        if (x.length != y.length || x.length < 3) return Double.NaN;
        return pearson(ranks(x), ranks(y));
    }
    private static double[] ranks(double[] values) {
        Integer[] indices = new Integer[values.length];
        for (int i = 0; i < indices.length; i++) indices[i] = i;
        Arrays.sort(indices, Comparator.comparingDouble(i -> values[i]));
        double[] ranks = new double[values.length];
        for (int i = 0; i < indices.length;) {
            int j = i + 1;
            while (j < indices.length && Double.compare(values[indices[i]], values[indices[j]]) == 0) j++;
            double averageRank = (i + 1 + j) / 2.0;
            for (int k = i; k < j; k++) ranks[indices[k]] = averageRank;
            i = j;
        }
        return ranks;
    }
    private static double pearson(double[] x, double[] y) {
        double mx = Arrays.stream(x).average().orElse(Double.NaN);
        double my = Arrays.stream(y).average().orElse(Double.NaN);
        double cov = 0, vx = 0, vy = 0;
        for (int i = 0; i < x.length; i++) {
            cov += (x[i] - mx) * (y[i] - my);
            vx += (x[i] - mx) * (x[i] - mx);
            vy += (y[i] - my) * (y[i] - my);
        }
        return vx == 0 || vy == 0 ? Double.NaN : cov / Math.sqrt(vx * vy);
    }
}
