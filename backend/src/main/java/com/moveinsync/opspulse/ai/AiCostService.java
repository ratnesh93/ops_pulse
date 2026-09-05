package com.moveinsync.opspulse.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveinsync.opspulse.api.dto.AiCostSummaryDto;
import com.moveinsync.opspulse.api.dto.FacilitiesSummaryDto;
import com.moveinsync.opspulse.config.OpenAiProperties;
import com.moveinsync.opspulse.config.SarvamProperties;
import com.moveinsync.opspulse.domain.AiUsageLog;
import com.moveinsync.opspulse.repository.AiUsageLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AiCostService {

    public static final String OP_STT = "STT";
    public static final String OP_LLM = "LLM";
    public static final String PROVIDER_SARVAM = "SARVAM";
    public static final String PROVIDER_OPENAI = "OPENAI";

    private final AiUsageLogRepository repository;
    private final SarvamProperties sarvamProperties;
    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;

    public AiCostService(
            AiUsageLogRepository repository,
            SarvamProperties sarvamProperties,
            OpenAiProperties openAiProperties,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.sarvamProperties = sarvamProperties;
        this.openAiProperties = openAiProperties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AiUsageLog logSttUsage(String model, int audioBytes, String transcript, Integer audioDurationMs) {
        int inputTokens = estimateAudioInputTokens(audioBytes, audioDurationMs);
        int outputTokens = estimateTextTokens(transcript);
        BigDecimal cost = calculateSttCost(inputTokens, outputTokens, audioDurationMs, audioBytes);

        AiUsageLog log = new AiUsageLog();
        log.setOperationType(OP_STT);
        log.setProvider(PROVIDER_SARVAM);
        log.setModel(model);
        log.setInputTokens(inputTokens);
        log.setOutputTokens(outputTokens);
        log.setCost(cost);
        log.setAudioDurationMs(audioDurationMs);
        log.setMetadataJson(toJson(Map.of(
                "audioBytes", audioBytes,
                "transcriptChars", transcript != null ? transcript.length() : 0)));
        log.setCreatedAt(Instant.now());
        return repository.save(log);
    }

    @Transactional
    public AiUsageLog logOpenAiUsage(String model, int inputTokens, int outputTokens) {
        BigDecimal costUsd = tokenCostUsd(inputTokens, outputTokens,
                openAiProperties.getInputCostPer1kTokensUsd(),
                openAiProperties.getOutputCostPer1kTokensUsd());
        BigDecimal costInr = costUsd.multiply(BigDecimal.valueOf(openAiProperties.getUsdToInr()))
                .setScale(6, RoundingMode.HALF_UP);

        AiUsageLog log = new AiUsageLog();
        log.setOperationType(OP_LLM);
        log.setProvider(PROVIDER_OPENAI);
        log.setModel(model);
        log.setInputTokens(inputTokens);
        log.setOutputTokens(outputTokens);
        log.setCost(costInr);
        log.setCreatedAt(Instant.now());
        return repository.save(log);
    }

    @Transactional
    public AiUsageLog logLlmUsage(String provider, String model, int inputTokens, int outputTokens) {
        BigDecimal cost = calculateLlmCost(inputTokens, outputTokens);

        AiUsageLog log = new AiUsageLog();
        log.setOperationType(OP_LLM);
        log.setProvider(provider);
        log.setModel(model);
        log.setInputTokens(inputTokens);
        log.setOutputTokens(outputTokens);
        log.setCost(cost);
        log.setCreatedAt(Instant.now());
        return repository.save(log);
    }

    public AiCostSummaryDto getSummary() {
        AiCostSummaryDto summary = new AiCostSummaryDto();
        summary.setTotalCostInr(repository.sumTotalCost());
        summary.setTotalInputTokens(repository.sumInputTokens());
        summary.setTotalOutputTokens(repository.sumOutputTokens());

        List<AiCostSummaryDto.OperationBreakdown> breakdown = repository.aggregateByOperationType().stream()
                .map(row -> {
                    AiCostSummaryDto.OperationBreakdown item = new AiCostSummaryDto.OperationBreakdown();
                    item.setOperationType((String) row[0]);
                    item.setRequestCount(((Number) row[1]).longValue());
                    item.setInputTokens(((Number) row[2]).longValue());
                    item.setOutputTokens(((Number) row[3]).longValue());
                    item.setCostInr((BigDecimal) row[4]);
                    return item;
                })
                .toList();
        summary.setByOperation(breakdown);

        List<AiCostSummaryDto.ProviderBreakdown> byProvider = repository.aggregateByProvider().stream()
                .map(row -> {
                    AiCostSummaryDto.ProviderBreakdown item = new AiCostSummaryDto.ProviderBreakdown();
                    item.setProvider((String) row[0]);
                    item.setRequestCount(((Number) row[1]).longValue());
                    item.setInputTokens(((Number) row[2]).longValue());
                    item.setOutputTokens(((Number) row[3]).longValue());
                    item.setCostInr((BigDecimal) row[4]);
                    return item;
                })
                .toList();
        summary.setByProvider(byProvider);

        summary.setTotalRequests(breakdown.stream().mapToLong(AiCostSummaryDto.OperationBreakdown::getRequestCount).sum());

        summary.setRecentUsage(repository.findTop20ByOrderByCreatedAtDesc().stream()
                .map(this::toUsageEntry)
                .toList());

        return summary;
    }

    public BigDecimal getCurrentMonthCostInr() {
        return repository.sumCostSince(currentMonthStart());
    }

    public long getCurrentMonthRequestCount() {
        return repository.countSince(currentMonthStart());
    }

    public String getCurrentMonthLabel() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        String month = now.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        return month + " " + now.getYear();
    }

    public List<FacilitiesSummaryDto.AiProviderMonthlyCost> getCurrentMonthCostByProvider() {
        return repository.aggregateByProviderSince(currentMonthStart()).stream()
                .map(row -> {
                    FacilitiesSummaryDto.AiProviderMonthlyCost item = new FacilitiesSummaryDto.AiProviderMonthlyCost();
                    String provider = (String) row[0];
                    item.setProvider(provider);
                    item.setProviderLabel(providerLabel(provider));
                    item.setRequestCount(((Number) row[1]).longValue());
                    item.setInputTokens(((Number) row[2]).longValue());
                    item.setOutputTokens(((Number) row[3]).longValue());
                    item.setCostInr((BigDecimal) row[4]);
                    return item;
                })
                .toList();
    }

    private String providerLabel(String provider) {
        if (PROVIDER_OPENAI.equals(provider)) {
            return "OpenAI";
        }
        if (PROVIDER_SARVAM.equals(provider)) {
            return "Sarvam AI";
        }
        return provider != null ? provider : "Unknown";
    }

    private Instant currentMonthStart() {
        return ZonedDateTime.now(ZoneId.systemDefault())
                .withDayOfMonth(1)
                .toLocalDate()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();
    }

    public int estimateTextTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, text.length() / 4);
    }

    public int estimateAudioInputTokens(int audioBytes, Integer audioDurationMs) {
        if (audioDurationMs != null && audioDurationMs > 0) {
            int seconds = Math.max(1, audioDurationMs / 1000);
            return seconds * sarvamProperties.getSttTokensPerAudioSecond();
        }
        int estimatedSeconds = Math.max(1, audioBytes / sarvamProperties.getSttBytesPerSecondEstimate());
        return estimatedSeconds * sarvamProperties.getSttTokensPerAudioSecond();
    }

    BigDecimal calculateSttCost(int inputTokens, int outputTokens, Integer audioDurationMs, int audioBytes) {
        if (sarvamProperties.getSttCostPerMinuteInr() > 0) {
            int durationMs = audioDurationMs != null && audioDurationMs > 0
                    ? audioDurationMs
                    : (audioBytes * 1000) / sarvamProperties.getSttBytesPerSecondEstimate();
            BigDecimal minutes = BigDecimal.valueOf(durationMs)
                    .divide(BigDecimal.valueOf(60_000), 6, RoundingMode.HALF_UP);
            return minutes.multiply(BigDecimal.valueOf(sarvamProperties.getSttCostPerMinuteInr()))
                    .setScale(6, RoundingMode.HALF_UP);
        }
        return tokenCost(inputTokens, outputTokens,
                sarvamProperties.getSttInputCostPer1kTokensInr(),
                sarvamProperties.getSttOutputCostPer1kTokensInr());
    }

    BigDecimal calculateLlmCost(int inputTokens, int outputTokens) {
        return tokenCost(inputTokens, outputTokens,
                sarvamProperties.getLlmInputCostPer1kTokensInr(),
                sarvamProperties.getLlmOutputCostPer1kTokensInr());
    }

    private BigDecimal tokenCostUsd(int inputTokens, int outputTokens, double inputRate, double outputRate) {
        BigDecimal inputCost = BigDecimal.valueOf(inputTokens)
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(inputRate));
        BigDecimal outputCost = BigDecimal.valueOf(outputTokens)
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(outputRate));
        return inputCost.add(outputCost).setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal tokenCost(int inputTokens, int outputTokens, double inputRate, double outputRate) {
        BigDecimal inputCost = BigDecimal.valueOf(inputTokens)
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(inputRate));
        BigDecimal outputCost = BigDecimal.valueOf(outputTokens)
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(outputRate));
        return inputCost.add(outputCost).setScale(6, RoundingMode.HALF_UP);
    }

    private AiCostSummaryDto.UsageEntry toUsageEntry(AiUsageLog log) {
        AiCostSummaryDto.UsageEntry entry = new AiCostSummaryDto.UsageEntry();
        entry.setId(log.getId());
        entry.setOperationType(log.getOperationType());
        entry.setProvider(log.getProvider());
        entry.setModel(log.getModel());
        entry.setInputTokens(log.getInputTokens());
        entry.setOutputTokens(log.getOutputTokens());
        entry.setCostInr(log.getCost());
        entry.setAudioDurationMs(log.getAudioDurationMs());
        entry.setCreatedAt(log.getCreatedAt());
        return entry;
    }

    private String toJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
