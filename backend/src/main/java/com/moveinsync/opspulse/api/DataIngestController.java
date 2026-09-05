package com.moveinsync.opspulse.api;

import com.moveinsync.opspulse.agent.AgentOrchestrator;
import com.moveinsync.opspulse.config.OpsPulseProperties;
import com.moveinsync.opspulse.data.MoveInSyncDatasetAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Temporary admin endpoints to upload CSVs and ingest into the remote DB.
 * Protected by ADMIN_INGEST_SECRET (header X-Admin-Secret).
 */
@RestController
@RequestMapping("/api/admin")
public class DataIngestController {

    private final OpsPulseProperties properties;
    private final MoveInSyncDatasetAdapter datasetAdapter;
    private final AgentOrchestrator agentOrchestrator;

    public DataIngestController(
            OpsPulseProperties properties,
            MoveInSyncDatasetAdapter datasetAdapter,
            AgentOrchestrator agentOrchestrator) {
        this.properties = properties;
        this.datasetAdapter = datasetAdapter;
        this.agentOrchestrator = agentOrchestrator;
    }

    @GetMapping("/data-status")
    public Map<String, Object> dataStatus(@RequestHeader(value = "X-Admin-Secret", required = false) String secret) {
        assertAuthorized(secret);
        Path dataDir = Path.of(properties.getDataPath());
        Map<String, Object> status = new HashMap<>(datasetAdapter.describeDataFiles(dataDir));
        status.put("skipDataLoad", properties.isSkipDataLoad());
        return status;
    }

    @PostMapping("/load-data")
    public Map<String, Object> loadData(
            @RequestHeader(value = "X-Admin-Secret", required = false) String secret,
            @RequestParam(defaultValue = "false") boolean force,
            @RequestParam(defaultValue = "true") boolean runAgent) {
        assertAuthorized(secret);
        Map<String, Object> result = datasetAdapter.loadForced(force);
        if (runAgent && Boolean.TRUE.equals(result.get("success"))) {
            UUID runId = agentOrchestrator.runCycle();
            result.put("agentRunId", runId.toString());
        }
        return result;
    }

    @PostMapping("/upload-csv")
    public Map<String, Object> uploadCsv(
            @RequestHeader(value = "X-Admin-Secret", required = false) String secret,
            @RequestParam(value = "julyTrip", required = false) MultipartFile julyTrip,
            @RequestParam(value = "juneTrip", required = false) MultipartFile juneTrip,
            @RequestParam(value = "bill", required = false) MultipartFile bill,
            @RequestParam(value = "alerts", required = false) MultipartFile alerts,
            @RequestParam(defaultValue = "false") boolean ingest,
            @RequestParam(defaultValue = "false") boolean force,
            @RequestParam(defaultValue = "true") boolean runAgent) throws Exception {
        assertAuthorized(secret);

        Path dataDir = Path.of(properties.getDataPath());
        Files.createDirectories(dataDir);

        Map<String, Object> saved = new HashMap<>();
        saveIfPresent(saved, dataDir.resolve(properties.getTripFile()), julyTrip);
        saveIfPresent(saved, dataDir.resolve(properties.getPriorTripFile()), juneTrip);
        saveIfPresent(saved, dataDir.resolve(properties.getBillFile()), bill);
        saveIfPresent(saved, dataDir.resolve(properties.getAlertsFile()), alerts);

        if (saved.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No files uploaded. Use form fields: julyTrip, juneTrip, bill, alerts");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("saved", saved);
        response.put("files", datasetAdapter.describeDataFiles(dataDir));

        if (ingest) {
            Map<String, Object> ingestResult = datasetAdapter.loadForced(force);
            response.putAll(ingestResult);
            if (runAgent && Boolean.TRUE.equals(ingestResult.get("success"))) {
                UUID runId = agentOrchestrator.runCycle();
                response.put("agentRunId", runId.toString());
            }
        }

        return response;
    }

    private void saveIfPresent(Map<String, Object> saved, Path target, MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return;
        }
        file.transferTo(target);
        saved.put(target.getFileName().toString(), Files.size(target));
    }

    private void assertAuthorized(String secret) {
        String expected = properties.getAdminIngestSecret();
        if (expected == null || expected.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "ADMIN_INGEST_SECRET is not configured on the server");
        }
        if (secret == null || !expected.equals(secret)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing X-Admin-Secret header");
        }
    }
}
