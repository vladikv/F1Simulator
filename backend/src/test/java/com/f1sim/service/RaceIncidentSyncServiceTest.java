package com.f1sim.service;

import com.f1sim.client.OpenF1Client;
import com.f1sim.client.dto.OpenF1LapDto;
import com.f1sim.client.dto.OpenF1RaceControlDto;
import com.f1sim.entity.Race;
import com.f1sim.entity.RaceIncident;
import com.f1sim.repository.RaceIncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RaceIncidentSyncServiceTest {

    @Mock
    private OpenF1Client openF1Client;

    @Mock
    private RaceIncidentRepository incidentRepository;

    @Mock
    private SessionLapTimelineService timeline;

    private RaceIncidentSyncService service;
    private Race race;

    @BeforeEach
    void setUp() {
        service = new RaceIncidentSyncService(openF1Client, incidentRepository, timeline);
        race = Race.builder().id(25L).grandPrixName("Monaco Grand Prix").build();

        // SC/VSC detection always runs too, and always needs a clean-lap baseline —
        // stub it even though this test doesn't exercise that path.
        when(timeline.medianCleanLapSeconds(anyList())).thenReturn(85.0);
    }

    @Test
    @DisplayName("Monaco 2024: red flag stoppage is measured by wall-clock gap, not lap count")
    void detectsRedFlagUsingWallClockGap() {
        // Real sequence from Monaco 2024's race_control feed: the session is
        // aborted and later resumes via "SESSION STARTED", never a Green flag —
        // this is exactly the case that silently failed before the fix.
        List<OpenF1RaceControlDto> messages = List.of(
                new OpenF1RaceControlDto(9523, "2024-05-26T13:03:44Z", 1, "Flag", "YELLOW", "Sector", "YELLOW IN TRACK SECTOR 5"),
                new OpenF1RaceControlDto(9523, "2024-05-26T13:04:07Z", 1, "SessionStatus", null, null, "SESSION ABORTED"),
                new OpenF1RaceControlDto(9523, "2024-05-26T13:04:08Z", 1, "Flag", "RED", "Track", "RED FLAG"),
                new OpenF1RaceControlDto(9523, "2024-05-26T13:44:00Z", 1, "SessionStatus", null, null, "SESSION STARTED")
        );

        when(openF1Client.getRaceControl(9523)).thenReturn(messages);

        service.sync(race, 9523, 1, Collections.emptyList());

        ArgumentCaptor<List<RaceIncident>> captor = ArgumentCaptor.forClass(List.class);
        verify(incidentRepository).saveAll(captor.capture());

        List<RaceIncident> saved = captor.getValue();
        assertEquals(1, saved.size());

        RaceIncident incident = saved.get(0);
        assertEquals(RaceIncident.IncidentType.RED_FLAG, incident.getType());
        // 13:04:08 -> 13:44:00 is exactly 39 minutes 52 seconds = 2392 seconds
        assertEquals(2392.0, incident.getTimeLossSeconds(), 0.01);
    }

    @Test
    @DisplayName("No red flag messages means no incidents are saved")
    void noIncidentsWhenNoFlags() {
        List<OpenF1RaceControlDto> messages = List.of(
                new OpenF1RaceControlDto(9523, "2024-05-26T13:03:44Z", 1, "Flag", "YELLOW", "Sector", "YELLOW IN TRACK SECTOR 5")
        );

        when(openF1Client.getRaceControl(9523)).thenReturn(messages);

        service.sync(race, 9523, 1, Collections.emptyList());

        ArgumentCaptor<List<RaceIncident>> captor = ArgumentCaptor.forClass(List.class);
        verify(incidentRepository).saveAll(captor.capture());

        assertTrue(captor.getValue().isEmpty());
    }
}