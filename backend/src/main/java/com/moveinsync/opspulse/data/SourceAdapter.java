package com.moveinsync.opspulse.data;

import java.util.Map;

public interface SourceAdapter {
    void load();

    /** Run ingest even when SKIP_DATA_LOAD=true. Set force=true to clear the data_loaded flag first. */
    Map<String, Object> loadForced(boolean force);
}
