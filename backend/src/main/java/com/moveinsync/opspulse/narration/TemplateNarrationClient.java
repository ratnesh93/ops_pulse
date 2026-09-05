package com.moveinsync.opspulse.narration;

import com.moveinsync.opspulse.benchmark.VendorBenchmark;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TemplateNarrationClient implements NarrationClient {

    @Override
    public String narrateVendorBreach(VendorBenchmark benchmark) {
        String prior = benchmark.getPriorMonthOtaPct() != null
                ? String.format("%.0f%% last month", benchmark.getPriorMonthOtaPct())
                : "prior month unavailable";
        String peer = benchmark.getPeerOtaPct() != null
                ? String.format("%.0f%%", benchmark.getPeerOtaPct())
                : "N/A";

        String attribution = formatAttribution(benchmark.getDelayAttribution());

        return String.format(
                "%s OTA is %.1f%% against a %.0f%% SLA — %s and %.0f points below peer %s at %s. %s Recommend vendor escalation.",
                benchmark.getVendorDisplayName(),
                benchmark.getOtaPct(),
                benchmark.getSlaOtaPct(),
                prior,
                benchmark.getPeerOtaPct() != null ? benchmark.getPeerOtaPct() - benchmark.getOtaPct() : 0,
                benchmark.getPeerVendorName(),
                peer,
                attribution);
    }

    @Override
    public String generateLeadershipMemo(VendorBenchmark benchmark) {
        String costCr = formatCostCrores(benchmark.getTotalCost());
        String office = benchmark.getTopAffectedOffice() != null
                ? benchmark.getTopAffectedOffice()
                : "multiple offices";

        return String.format(
                "Transport Operations Brief — July 2026\n\n" +
                "Vendor %s is operating at %.1f%% on-time arrival against our %.0f%% SLA target. " +
                "This represents a sustained performance gap with %.1f%% OTA in the prior month. " +
                "Peer vendor %s maintains %.1f%% OTA on comparable volume.\n\n" +
                "Financial exposure: ₹%s crore billed over the analysis period. " +
                "Delay concentration is highest at %s. Root causes are primarily employee boarding friction and driver-related delays.\n\n" +
                "Recommended action: Escalate with vendor leadership and institute a 30-day recovery plan with weekly SLA checkpoints.",
                benchmark.getVendorDisplayName(),
                benchmark.getOtaPct(),
                benchmark.getSlaOtaPct(),
                benchmark.getPriorMonthOtaPct() != null ? benchmark.getPriorMonthOtaPct() : 0,
                benchmark.getPeerVendorName(),
                benchmark.getPeerOtaPct() != null ? benchmark.getPeerOtaPct() : 0,
                costCr,
                office);
    }

    private String formatAttribution(Map<String, Double> attribution) {
        if (attribution.isEmpty()) {
            return "";
        }
        String top = attribution.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(2)
                .map(e -> e.getKey() + " " + String.format("%.0f%%", e.getValue()))
                .collect(Collectors.joining(" + "));
        double covered = attribution.values().stream().mapToDouble(Double::doubleValue).sum();
        return String.format("%.0f%% of delays are %s.", covered, top);
    }

    private String formatCostCrores(BigDecimal cost) {
        if (cost == null) {
            return "0.0";
        }
        BigDecimal crores = cost.divide(BigDecimal.valueOf(10_000_000), 1, RoundingMode.HALF_UP);
        return crores.toPlainString();
    }
}
