package com.moveinsync.opspulse.scheduler;

import com.moveinsync.opspulse.agent.AgentOrchestrator;
import com.moveinsync.opspulse.data.SourceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
public class StartupRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupRunner.class);

    private final SourceAdapter sourceAdapter;
    private final AgentOrchestrator agentOrchestrator;

    public StartupRunner(SourceAdapter sourceAdapter, AgentOrchestrator agentOrchestrator) {
        this.sourceAdapter = sourceAdapter;
        this.agentOrchestrator = agentOrchestrator;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(1)
    public void onReady() {
        log.info("Starting data load...");
        sourceAdapter.load();
        log.info("Running initial agent cycle...");
        agentOrchestrator.runCycle();
        log.info("Ops Pulse ready");
    }
}
