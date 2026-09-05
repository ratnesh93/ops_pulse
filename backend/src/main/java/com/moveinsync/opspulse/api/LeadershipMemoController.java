package com.moveinsync.opspulse.api;

import com.moveinsync.opspulse.agent.AgentOrchestrator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/leadership")
public class LeadershipMemoController {

    private final AgentOrchestrator agentOrchestrator;

    public LeadershipMemoController(AgentOrchestrator agentOrchestrator) {
        this.agentOrchestrator = agentOrchestrator;
    }

    @GetMapping("/memo")
    public Map<String, String> getMemo() {
        return Map.of("memo", agentOrchestrator.getLeadershipMemo());
    }
}
