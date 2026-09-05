package com.moveinsync.opspulse.benchmark;

import com.moveinsync.opspulse.ai.AiCostService;
import com.moveinsync.opspulse.api.dto.BriefResponse;
import com.moveinsync.opspulse.api.dto.FacilitiesSummaryDto;
import com.moveinsync.opspulse.api.dto.VendorSummaryDto;
import com.moveinsync.opspulse.config.OpsPulseProperties;
import com.moveinsync.opspulse.domain.VendorMonthlyStat;
import com.moveinsync.opspulse.repository.TripRepository;
import com.moveinsync.opspulse.repository.VendorMonthlyStatRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class BenchmarkingService {

    private final OpsPulseProperties properties;
    private final TripRepository tripRepository;
    private final VendorMonthlyStatRepository vendorMonthlyStatRepository;
    private final VendorEnrichmentService vendorEnrichmentService;
    private final OperationalInsightsService operationalInsightsService;
    private final AiCostService aiCostService;

    public BenchmarkingService(
            OpsPulseProperties properties,
            TripRepository tripRepository,
            VendorMonthlyStatRepository vendorMonthlyStatRepository,
            VendorEnrichmentService vendorEnrichmentService,
            OperationalInsightsService operationalInsightsService,
            AiCostService aiCostService) {
        this.properties = properties;
        this.tripRepository = tripRepository;
        this.vendorMonthlyStatRepository = vendorMonthlyStatRepository;
        this.vendorEnrichmentService = vendorEnrichmentService;
        this.operationalInsightsService = operationalInsightsService;
        this.aiCostService = aiCostService;
    }

    public VendorBenchmark benchmarkVendor(String vendorId) {
        long total = tripRepository.countByVendorId(vendorId);
        long onTime = tripRepository.countOnTimeByVendor(vendorId);
        double ota = total == 0 ? 0.0 : round((onTime * 100.0) / total);

        VendorBenchmark benchmark = new VendorBenchmark();
        benchmark.setVendorId(vendorId);
        benchmark.setVendorDisplayName(
                vendorId.equals(properties.getAnalysisVendor())
                        ? properties.getVendorDisplayAlias()
                        : vendorId);
        benchmark.setOtaPct(ota);
        benchmark.setSlaOtaPct(properties.getSlaOtaPct());
        benchmark.setTripCount(total);
        benchmark.setTotalCost(tripRepository.sumCostByVendor(vendorId));

        vendorMonthlyStatRepository.findByVendorIdAndMonthYear(vendorId, "2026-06")
                .map(VendorMonthlyStat::getOtaPct)
                .map(BigDecimal::doubleValue)
                .ifPresent(benchmark::setPriorMonthOtaPct);

        VendorBenchmark peer = benchmarkVendorRaw(properties.getPeerVendor());
        benchmark.setPeerOtaPct(peer.getOtaPct());
        benchmark.setPeerVendorName(shortVendorName(properties.getPeerVendor()));

        List<Object[]> offices = tripRepository.topDelayedOffices(vendorId);
        if (!offices.isEmpty()) {
            benchmark.setTopAffectedOffice((String) offices.get(0)[0]);
        }

        benchmark.setDelayAttribution(calculateDelayAttribution(vendorId));
        return benchmark;
    }

    public VendorBenchmark benchmarkFocusVendor() {
        return benchmarkVendor(properties.getAnalysisVendor());
    }

    public List<String> listRegisteredOffices() {
        return tripRepository.findDistinctOffices();
    }

    public Optional<OfficeSummary> summarizeOffice(String office) {
        long total = tripRepository.countByOffice(office);
        if (total == 0) {
            return Optional.empty();
        }

        long onTime = tripRepository.countOnTimeByOffice(office);
        long delayed = tripRepository.countDelayedByOffice(office);
        long loginRoutes = tripRepository.countDistinctLoginRoutesByOffice(office);
        long logoutRoutes = tripRepository.countDistinctLogoutRoutesByOffice(office);

        OfficeSummary summary = new OfficeSummary();
        summary.setOffice(office);
        summary.setTripCount(total);
        summary.setDelayedCount(delayed);
        summary.setOtaPct(round((onTime * 100.0) / total));
        summary.setLoginRouteCount(loginRoutes);
        summary.setLogoutRouteCount(logoutRoutes);
        return Optional.of(summary);
    }

    public List<String> topVendorsByDelayedTrips(int limit) {
        return tripRepository.topVendorsByDelayedTrips().stream()
                .limit(limit)
                .map(row -> String.format(
                        "%s: %,d delayed July trips",
                        row[0],
                        ((Number) row[1]).longValue()))
                .toList();
    }

    public List<VendorSummaryDto> listAllVendorMetrics() {
        List<VendorSummaryDto> vendors = buildVendorSummaries();
        enrichWithRanksAndPeerGap(vendors);
        enrichWithBillAndSafety(vendors);
        return vendors;
    }

    public FacilitiesSummaryDto buildFacilitiesSummary() {
        List<VendorSummaryDto> vendors = listAllVendorMetrics();
        SafetyInsight safety = operationalInsightsService.analyzeSafety();

        long totalTrips = vendors.stream().mapToLong(VendorSummaryDto::getTripCount).sum();
        long totalOnTime = vendors.stream().mapToLong(VendorSummaryDto::getOnTimeTripCount).sum();
        BigDecimal totalCost = vendorEnrichmentService.billMetricsByVendor().values().stream()
                .map(VendorEnrichmentService.VendorBillMetric::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalKm = vendorEnrichmentService.billMetricsByVendor().values().stream()
                .map(VendorEnrichmentService.VendorBillMetric::getTotalKm)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        FacilitiesSummaryDto summary = new FacilitiesSummaryDto();
        summary.setFleetOtaPct(totalTrips == 0 ? 0.0 : round((totalOnTime * 100.0) / totalTrips));
        summary.setTotalCost(totalCost);
        summary.setTotalKm(totalKm);
        if (totalKm.compareTo(BigDecimal.ZERO) > 0) {
            summary.setCostPerKm(totalCost.divide(totalKm, 2, RoundingMode.HALF_UP));
        }
        summary.setSafetyIncidentCount(safety.getJulyAlertCount());
        summary.setSev1Count(safety.getSev1Count());
        summary.setPanicCount(safety.getPanicCount());
        summary.setVendorCount(vendors.size());
        summary.setVendorsBelowSla((int) vendors.stream().filter(VendorSummaryDto::isSlaBreach).count());

        vendors.stream()
                .filter(v -> v.getCostPerKm() != null)
                .max(Comparator.comparing(VendorSummaryDto::getCostPerKm))
                .ifPresent(v -> {
                    summary.setHighestCostPerKmVendor(v.getDisplayName());
                    summary.setHighestCostPerKm(v.getCostPerKm());
                });

        vendors.stream()
                .min(Comparator.comparing(VendorSummaryDto::getOtaPct))
                .ifPresent(v -> {
                    summary.setLowestOtaVendor(v.getDisplayName());
                    summary.setLowestOtaPct(v.getOtaPct());
                });

        summary.setAiMonthlyCostInr(aiCostService.getCurrentMonthCostInr());
        summary.setAiMonthlyRequestCount(aiCostService.getCurrentMonthRequestCount());
        summary.setAiCostMonth(aiCostService.getCurrentMonthLabel());
        summary.setAiCostByProvider(aiCostService.getCurrentMonthCostByProvider());

        return summary;
    }

    public BriefResponse.MorningBrief buildMorningBrief(int findingCount, int pendingActionCount) {
        VendorBenchmark focus = benchmarkFocusVendor();
        List<VendorSummaryDto> vendors = listAllVendorMetrics();
        long breachCount = vendors.stream().filter(VendorSummaryDto::isSlaBreach).count();
        VendorSummaryDto focusRow = vendors.stream()
                .filter(VendorSummaryDto::isFocusVendor)
                .findFirst()
                .orElse(null);

        BriefResponse.MorningBrief brief = new BriefResponse.MorningBrief();
        brief.setGreeting("Good morning — your July transport brief is ready");
        brief.setItemsNeedingAttention(findingCount + pendingActionCount);
        brief.setVendorsBelowSla((int) breachCount);
        brief.setVendorCount(vendors.size());
        brief.setFocusVendorName(focus.getVendorDisplayName());
        brief.setCostAtRisk(focus.getTotalCost());
        brief.setPeerVendorName(focus.getPeerVendorName());
        if (focusRow != null) {
            brief.setFocusVendorOtaRank(focusRow.getOtaRank());
            brief.setPeerGapPct(focusRow.getPeerGapPct());
        }
        brief.setSummary(String.format(
                "%d items need your attention · %d of %d vendors below SLA · %s ranks #%d on OTA (%.0f pts behind %s)",
                brief.getItemsNeedingAttention(),
                brief.getVendorsBelowSla(),
                brief.getVendorCount(),
                focus.getVendorDisplayName(),
                brief.getFocusVendorOtaRank(),
                brief.getPeerGapPct() != null ? brief.getPeerGapPct() : 0,
                focus.getPeerVendorName()));
        return brief;
    }

    private List<VendorSummaryDto> buildVendorSummaries() {
        double sla = properties.getSlaOtaPct();
        String focusId = properties.getAnalysisVendor();
        String focusAlias = properties.getVendorDisplayAlias();

        return tripRepository.aggregateMetricsByVendor().stream()
                .map(row -> {
                    String vendorId = (String) row[0];
                    long tripCount = ((Number) row[1]).longValue();
                    long onTimeCount = ((Number) row[2]).longValue();
                    BigDecimal totalCost = row[3] instanceof BigDecimal
                            ? (BigDecimal) row[3]
                            : BigDecimal.valueOf(((Number) row[3]).doubleValue());

                    double ota = tripCount == 0 ? 0.0 : round((onTimeCount * 100.0) / tripCount);

                    VendorSummaryDto dto = new VendorSummaryDto();
                    dto.setVendorId(vendorId);
                    dto.setDisplayName(vendorId.equals(focusId) ? focusAlias : vendorId);
                    dto.setOtaPct(ota);
                    dto.setSlaOtaPct(sla);
                    dto.setTripCount(tripCount);
                    dto.setOnTimeTripCount(onTimeCount);
                    dto.setTotalCost(totalCost);
                    dto.setSlaBreach(ota < sla);
                    dto.setFocusVendor(vendorId.equals(focusId));
                    if (onTimeCount > 0) {
                        dto.setCostPerOnTimeTrip(
                                totalCost.divide(BigDecimal.valueOf(onTimeCount), 2, RoundingMode.HALF_UP));
                    }

                    vendorMonthlyStatRepository.findByVendorIdAndMonthYear(vendorId, "2026-06")
                            .map(VendorMonthlyStat::getOtaPct)
                            .map(BigDecimal::doubleValue)
                            .ifPresent(dto::setPriorMonthOtaPct);

                    return dto;
                })
                .toList();
    }

    private void enrichWithRanksAndPeerGap(List<VendorSummaryDto> vendors) {
        double peerOta = vendors.stream()
                .filter(v -> v.getVendorId().equals(properties.getPeerVendor()))
                .map(VendorSummaryDto::getOtaPct)
                .findFirst()
                .orElse(0.0);

        List<VendorSummaryDto> byOta = vendors.stream()
                .sorted(Comparator.comparing(VendorSummaryDto::getOtaPct).reversed())
                .toList();

        Map<String, Integer> rankByVendor = new HashMap<>();
        for (int i = 0; i < byOta.size(); i++) {
            rankByVendor.put(byOta.get(i).getVendorId(), i + 1);
        }

        int total = vendors.size();
        for (VendorSummaryDto vendor : vendors) {
            vendor.setVendorCount(total);
            vendor.setOtaRank(rankByVendor.getOrDefault(vendor.getVendorId(), total));
            if (peerOta > 0) {
                vendor.setPeerGapPct(round(peerOta - vendor.getOtaPct()));
            }
        }
    }

    private void enrichWithBillAndSafety(List<VendorSummaryDto> vendors) {
        Map<String, VendorEnrichmentService.VendorBillMetric> billMetrics = vendorEnrichmentService.billMetricsByVendor();
        Map<String, VendorEnrichmentService.VendorSafetyMetric> safetyMetrics = vendorEnrichmentService.safetyMetricsByVendor();

        for (VendorSummaryDto vendor : vendors) {
            VendorEnrichmentService.VendorBillMetric bill = billMetrics.get(vendor.getVendorId());
            if (bill != null && bill.getCostPerKm() != null) {
                vendor.setCostPerKm(bill.getCostPerKm());
            }

            VendorEnrichmentService.VendorSafetyMetric safety = safetyMetrics.get(vendor.getVendorId());
            if (safety != null) {
                vendor.setSafetyIncidentCount(safety.getIncidentCount());
                vendor.setSev1Count(safety.getSev1Count());
                vendor.setPanicCount(safety.getPanicCount());
            }
        }
    }

    private VendorBenchmark benchmarkVendorRaw(String vendorId) {
        long total = tripRepository.countByVendorId(vendorId);
        long onTime = tripRepository.countOnTimeByVendor(vendorId);
        double ota = total == 0 ? 0.0 : round((onTime * 100.0) / total);
        VendorBenchmark b = new VendorBenchmark();
        b.setVendorId(vendorId);
        b.setOtaPct(ota);
        b.setTripCount(total);
        return b;
    }

    private Map<String, Double> calculateDelayAttribution(String vendorId) {
        List<Object[]> rows = tripRepository.delayAttribution(vendorId);
        long totalDelayed = rows.stream().mapToLong(r -> (Long) r[1]).sum();
        Map<String, Double> attribution = new LinkedHashMap<>();
        if (totalDelayed == 0) {
            return attribution;
        }
        for (Object[] row : rows) {
            String reason = (String) row[0];
            long count = (Long) row[1];
            attribution.put(reason, round((count * 100.0) / totalDelayed));
        }
        return attribution;
    }

    private String shortVendorName(String vendor) {
        if (vendor == null) {
            return "";
        }
        String first = vendor.split(" ")[0];
        return first + " Travel";
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
