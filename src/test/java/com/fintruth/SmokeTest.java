package com.fintruth;
import java.time.LocalDate;
import java.util.List;
public class SmokeTest {
    public static void main(String[] args) {
        var v = FinTruth.analyze("test", "Revenue will definitely rise 100% with guaranteed risk-free returns.");
        assert v.tau() > 0.0 && v.tau() <= 1.0;
        assert v.riskTags().contains("language_red_flag");
        assert v.riskTags().contains("numeric_ambiguity");
        var low = FinTruth.analyze("low", "Operating cash flow remained steady. Guidance remains cautious.");
        assert v.tau() > low.tau();
        var p = List.of(new EventStudy.Price(LocalDate.of(2025,1,1),"X",.03,.01),
                        new EventStudy.Price(LocalDate.of(2025,1,2),"X",-.01,.01));
        assert Math.abs(EventStudy.car(p,"X",LocalDate.of(2025,1,1),2)) < 1e-10;
        assert Double.isNaN(EventStudy.car(p,"X",LocalDate.of(2025,1,1),5));
        assert Math.abs(EventStudy.spearman(new double[]{1,2,3},new double[]{3,2,1}) + 1) < 1e-10;
        System.out.println("All smoke tests passed.");
    }
}
