package com.moveinsync.opspulse.api;

import com.moveinsync.opspulse.agent.AgentOrchestrator;
import com.moveinsync.opspulse.api.dto.BriefResponse;
import com.moveinsync.opspulse.domain.AgentAction;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ActionController {

    private final AgentOrchestrator agentOrchestrator;
    private final BriefController briefController;

    public ActionController(AgentOrchestrator agentOrchestrator, BriefController briefController) {
        this.agentOrchestrator = agentOrchestrator;
        this.briefController = briefController;
    }

    @GetMapping("/actions")
    public List<BriefResponse.ActionDto> listActions(@RequestParam(required = false) String status) {
        BriefResponse brief = briefController.getBrief();
        if (status == null) {
            return brief.getPendingActions();
        }
        return brief.getPendingActions().stream()
                .filter(a -> status.equalsIgnoreCase(a.getStatus()))
                .collect(Collectors.toList());
    }

    @PostMapping("/actions/{id}/confirm")
    public Map<String, Object> confirm(@PathVariable Long id) {
        AgentAction action = agentOrchestrator.confirmAction(id);
        return Map.of(
                "id", action.getId(),
                "status", action.getStatus(),
                "confirmedAt", action.getConfirmedAt());
    }
}
