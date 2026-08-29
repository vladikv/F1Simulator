package com.f1sim.controller;

import com.f1sim.service.RaceSyncService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/sync")
@RequiredArgsConstructor
@Tag(name = "Sync", description = "Manual triggers for pulling data from OpenF1")
public class SyncController {

    private final RaceSyncService raceSyncService;

    @PostMapping("/season/{year}")
    public RaceSyncService.SyncResult syncSeason(@PathVariable int year) {
        return raceSyncService.syncSeason(year);
    }
}