package com.moveinsync.opspulse.narration;

import com.moveinsync.opspulse.benchmark.VendorBenchmark;

public interface NarrationClient {
    String narrateVendorBreach(VendorBenchmark benchmark);
    String generateLeadershipMemo(VendorBenchmark benchmark);
}
