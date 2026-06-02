package com.railconnect.dto.request;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class CancellationRequest {
    private String pnrNumber;
    private String reason;
    private List<UUID> passengerIds; // null = cancel all
}
