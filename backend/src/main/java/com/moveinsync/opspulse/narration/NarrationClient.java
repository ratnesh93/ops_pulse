package com.moveinsync.opspulse.narration;

import com.moveinsync.opspulse.benchmark.CapacityInsight;
import com.moveinsync.opspulse.benchmark.SafetyInsight;
import com.moveinsync.opspulse.benchmark.VendorBenchmark;

public interface NarrationClient {
    String narrateVendorBreach(VendorBenchmark benchmark);
    String narrateSafetyEscalation(SafetyInsight insight);
    String narrateCapacityShortfall(CapacityInsight insight);
    String generateLeadershipMemo(VendorBenchmark benchmark);
}
