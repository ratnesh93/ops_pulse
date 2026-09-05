package com.moveinsync.opspulse.api;

import com.moveinsync.opspulse.agent.AgentOrchestrator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentOrchestrator agentOrchestrator;

    public AgentController(AgentOrchestrator agentOrchestrator) {
        this.agentOrchestrator = agentOrchestrator;
    }

    @PostMapping("/run")
    public Map<String, Object> runAgent() {
        UUID runId = agentOrchestrator.runCycle();
        return Map.of("runId", runId, "status", "COMPLETE");
    }
}
