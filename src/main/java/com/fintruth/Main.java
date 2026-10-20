package com.fintruth;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Command-line entry point. Input TSV: document_id<TAB>instrument<TAB>text. */
public final class Main {
    private Main() {}
    private static String esc(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }
    private static String num(double d) { return Double.isFinite(d) ? Double.toString(d) : "null"; }
    public static void main(String[] args) throws Exception {
        Path input = Path.of(args.length > 0 ? args[0] : "data/disclosures.tsv");
        Path output = Path.of(args.length > 1 ? args[1] : "output/results.jsonl");
        List<String> lines = Files.readAllLines(input, StandardCharsets.UTF_8);
        List<String> results = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank() || line.startsWith("#") || (i == 0 && line.startsWith("document_id\t"))) continue;
            String[] fields = line.split("\t", 3);
            if (fields.length != 3) throw new IllegalArgumentException("Expected three TSV fields at line " + (i + 1));
            FinTruth.Verdict v = FinTruth.analyze(fields[0], fields[2]);
            String tags = v.riskTags().stream().map(Main::esc).reduce((a,b) -> a + "," + b).orElse("");
            FinTruth.Forecast f = v.forecast();
            results.add("{\"document_id\":" + esc(fields[0]) + ",\"instrument\":" + esc(fields[1])
                    + ",\"fact_score\":" + num(v.factScore()) + ",\"numeric_score\":" + num(v.numericScore())
                    + ",\"tone_score\":" + num(v.toneScore()) + ",\"tau\":" + num(v.tau())
                    + ",\"probability_down\":" + num(f.probabilityDown())
                    + ",\"reaction_1d\":" + num(f.reaction1d())
                    + ",\"reaction_5d\":" + num(f.reaction5d())
                    + ",\"reaction_20d\":" + num(f.reaction20d())
                    + ",\"risk_tags\":[" + tags + "],\"explanation\":" + esc(v.explanation()) + "}");
        }
        if (output.getParent() != null) Files.createDirectories(output.getParent());
        Files.write(output, results, StandardCharsets.UTF_8);
        System.out.println("Processed " + results.size() + " disclosures into " + output);
    }
}
