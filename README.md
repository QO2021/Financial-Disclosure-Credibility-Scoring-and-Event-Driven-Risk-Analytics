# FinTruth Alpha — Java Research Prototype

An auditable Java implementation of the uploaded FinTruth financial-disclosure credibility research framework.

## Scope

- Sentence/claim segmentation (English and Chinese punctuation)
- Rule-based promotional-language and numerical-ambiguity scoring
- Tone-risk scoring and equal-weight credibility factor `tau`
- Cold-start logistic probability and 1/5/20-day abnormal-return forecasts
- Market-adjusted cumulative abnormal return (CAR) utility and Spearman rank IC
- JSON Lines output, a CLI, and dependency-free smoke tests

**Research caveat:** This is a Java reimplementation based on the uploaded report and available Python entry-point scripts. The original `src.agents.orchestrator` and `src.market.reaction` implementations were not supplied, so precise Python–Java parity is not established. Term dictionaries and claim splitting are reconstructed from the research report; these may differ from the original implementations. The cold-start forecasts are heuristic priors, not calibrated predictions. No historical prices or original disclosure JSON dataset were supplied, so the historical backtest cannot be reproduced here. The sample disclosure file is illustrative. This is not a credit eTrading execution platform.

## Requirements

Java 17+; no third-party libraries required.

## Build and run

From the project root:

```bash
mkdir -p out
javac -encoding UTF-8 -d out src/main/java/com/fintruth/*.java src/test/java/com/fintruth/*.java
java -ea -cp out com.fintruth.SmokeTest
java -cp out com.fintruth.Main data/disclosures.tsv output/results.jsonl
```

Output is one JSON object per input document.

## Input schema

Tab-separated UTF-8 file: `document_id`, `instrument`, `text` (header required). No tabs/newlines inside the text field. An explicit TSV format is used to avoid external JSON dependencies.

## Research methodology

`fact = min(1, 0.25 * red_flag_term_hits + 0.15 * I(claim_count > 5))`

`numeric = 0.4` when a numeric claim has a percentage but no comparison baseline; otherwise `0.1 * min(numeric_claim_count, 3)`.

`tone = min(1, 0.35 * negative_term_hits + 0.25 * hype_term_hits)`

`tau = (fact + numeric + tone) / 3`

`p_down = sigmoid(4 * (tau - 0.4))`

`AR_1 = -0.002 * tau`

`AR_5 = -0.008 * tau * (0.5 + p_down)`

`AR_20 = -0.012 * tau * (0.4 + p_down)`

For event studies, `AR_t = stock_return_t - market_return_t`, with CAR summed over the requested number of observations starting at the first trading date on or after the event date. Note that event-date handling may need adjustment for after-close announcements. The supplied report's notation for CAR windows is not completely consistent with its horizon labels; this implementation explicitly uses exactly `h` observations.

## Credit eTrading extensions (not yet implemented)

To build a separate credit eTrading portfolio project, add a bond pricing engine, spread analytics (Z-spread / OAS), CDS valuation, streaming market data, order book/quote handling, concurrency controls, latency benchmarks, and Python/Java interoperability.
