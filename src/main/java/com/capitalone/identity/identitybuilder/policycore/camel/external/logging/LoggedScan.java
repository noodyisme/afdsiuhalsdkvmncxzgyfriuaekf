package com.capitalone.identity.identitybuilder.policycore.camel.external.logging;

import com.capitalone.identity.identitybuilder.model.ConfigStoreScanCompleted;
import com.capitalone.identity.identitybuilder.model.ScanRequest;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * see {@link RuntimeUpdateEventLogger}
 */
@Value
public class LoggedScan {
    private static final DateTimeFormatter isoDateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;

    public static LoggedScan newFromResult(@NonNull ConfigStoreScanCompleted result) {
        final ScanRequest request = result.getRequest();
        return new LoggedScan(request.getScanType(),
                isoDateTimeFormatter.format(Instant.ofEpochMilli(request.getStartScheduled()).atOffset(ZoneOffset.UTC)),
                isoDateTimeFormatter.format(Instant.ofEpochMilli(request.getStartActual()).atOffset(ZoneOffset.UTC)),
                isoDateTimeFormatter.format(Instant.ofEpochMilli(result.getEndActual()).atOffset(ZoneOffset.UTC)));
    }

    @NonNull ScanRequest.ScanType scanType;
    @NonNull String scheduled;
    @NonNull String start;
    @NonNull String end;
}
