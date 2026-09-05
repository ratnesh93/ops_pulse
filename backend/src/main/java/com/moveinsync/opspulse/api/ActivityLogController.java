package com.moveinsync.opspulse.api;

import com.moveinsync.opspulse.api.dto.ActivityLogEntryDto;
import com.moveinsync.opspulse.domain.AgentAuditLog;
import com.moveinsync.opspulse.repository.AgentAuditLogRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ActivityLogController {

    private final AgentAuditLogRepository auditLogRepository;

    public ActivityLogController(AgentAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/activity-log")
    public List<ActivityLogEntryDto> getActivityLog() {
        return auditLogRepository.findAllByOrderByTimestampDesc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private ActivityLogEntryDto toDto(AgentAuditLog log) {
        ActivityLogEntryDto dto = new ActivityLogEntryDto();
        dto.setId(log.getId());
        dto.setRunId(log.getRunId());
        dto.setStage(log.getStage());
        dto.setMessage(log.getMessage());
        dto.setTimestamp(log.getTimestamp());
        return dto;
    }
}
