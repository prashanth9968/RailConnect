package com.railconnect.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "inventory-service", url = "${app.inventory-service-uri:http://inventory-service:8083}")
public interface InventoryClient {

    @PostMapping("/api/v1/trains/{trainId}/seats/generate")
    void ensureSeatsGenerated(
            @PathVariable("trainId") Long trainId,
            @RequestParam("date") String date
    );
}
