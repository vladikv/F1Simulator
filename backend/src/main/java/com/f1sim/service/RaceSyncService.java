package com.f1sim.service;

import com.f1sim.client.OpenF1Client;
import com.f1sim.client.dto.*;
import com.f1sim.entity.Circuit;
import com.f1sim.entity.Driver;
import com.f1sim.entity.Race;
import com.f1sim.entity.Team;
import com.f1sim.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
@Slf4j
public class RaceSyncService {
    private static final Map<String, Integer> CIRCUIT_LAP_COUNTS = Map.ofEntries(
            Map.entry("Yas Marina Circuit", 58),
            Map.entry("Melbourne", 58),
            Map.entry("Spielberg", 71),
            Map.entry("Baku", 51),
            Map.entry("Spa-Francorchamps", 44),
            Map.entry("Sakhir", 57),
            Map.entry("Interlagos", 71),
            Map.entry("Montreal", 70),
            Map.entry("Shanghai", 56),
            Map.entry("Catalunya", 66),
            Map.entry("Silverstone", 52),
            Map.entry("Hungaroring", 70),
            Map.entry("Monza", 53),
            Map.entry("Imola", 63),
            Map.entry("Suzuka", 53),
            Map.entry("Monte Carlo", 78),
            Map.entry("Mexico City", 71),
            Map.entry("Zandvoort", 72),
            Map.entry("Lusail", 57),
            Map.entry("Jeddah", 50),
            Map.entry("Singapore", 62),
            Map.entry("Austin", 56),
            Map.entry("Miami", 57),
            Map.entry("Las Vegas", 50)
    );

    private static final double DEFAULT_LAP_LENGTH_KM = 5.0;
    private static final double DEFAULT_PIT_LANE_LOSS_SECONDS = 22.0;
    private static final int DEFAULT_OVERTAKING_DIFFICULTY = 5;
    private static final double DEFAULT_TEAM_PIT_STOP_SECONDS = 2.4;
    private static final long OPENF1_REQUEST_SPACING_MS = 3_000;
    private final RaceIncidentSyncService incidentSyncService;
    private final RaceWeatherSyncService weatherSyncService;

    private final OpenF1Client openF1Client;
    private final CircuitRepository circuitRepository;
    private final RaceRepository raceRepository;
    private final TeamRepository teamRepository;
    private final DriverRepository driverRepository;
    private final RaceIncidentRepository incidentRepository;
    private final RaceWeatherWindowRepository weatherWindowRepository;

    @Transactional
    public SyncResult syncSeason(int year) {
        List<OpenF1MeetingDto> meetings = openF1Client.getMeetings(year);
        log.info("OpenF1 returned {} meetings for {}", meetings.size(), year);

        int racesCreated = 0;
        int driversUpserted = 0;

        for (OpenF1MeetingDto meeting : meetings) {
            Circuit circuit = upsertCircuit(meeting);

            List<OpenF1SessionDto> raceSessions;
            try {
                raceSessions = openF1Client.getRaceSessions(meeting.meetingKey());
            } catch (HttpClientErrorException.NotFound e) {
                log.info("No Race session for meeting {} ({}), skipping", meeting.meetingKey(), meeting.meetingName());
                continue;
            }

            for (OpenF1SessionDto session : raceSessions) {
                Race race = upsertRace(meeting, session, circuit);
                racesCreated++;

                syncIncidentsAndWeather(race, session.sessionKey());

                List<OpenF1DriverDto> drivers = openF1Client.getDrivers(session.sessionKey());

                for (OpenF1DriverDto driverDto : drivers) {
                    upsertDriver(driverDto);
                    driversUpserted++;
                }
            }
        }

        log.info("Sync complete: {} meetings, {} races, {} driver entries", meetings.size(), racesCreated, driversUpserted);
        return new SyncResult(meetings.size(), racesCreated, driversUpserted);
    }


    private Circuit upsertCircuit(OpenF1MeetingDto meeting) {
        return circuitRepository.findByExternalCircuitKey(meeting.circuitKey())
                .orElseGet(() -> circuitRepository.save(Circuit.builder()
                        .name(meeting.circuitShortName())
                        .country(meeting.countryName())
                        .lapLengthKm(DEFAULT_LAP_LENGTH_KM)
                        .pitLaneTimeLossSeconds(DEFAULT_PIT_LANE_LOSS_SECONDS)
                        .overtakingDifficulty(DEFAULT_OVERTAKING_DIFFICULTY)
                        .externalCircuitKey(meeting.circuitKey())
                        .build()));
    }

    private Race upsertRace(OpenF1MeetingDto meeting, OpenF1SessionDto session, Circuit circuit) {
        String externalKey = String.valueOf(session.sessionKey());
        Race existing = raceRepository.findByExternalSessionKey(externalKey).orElse(null);

        LocalDateTime raceDateTime = parseDateTime(session.dateStart());
        Race.RaceStatus status = raceDateTime.isBefore(LocalDateTime.now(ZoneOffset.UTC))
                ? Race.RaceStatus.FINISHED
                : Race.RaceStatus.UPCOMING;

        Integer totalLaps = CIRCUIT_LAP_COUNTS.get(circuit.getName());

        if (existing != null) {
            existing.setStatus(status);
            if (existing.getTotalLaps() == null && totalLaps != null) {
                existing.setTotalLaps(totalLaps);
            }
            return raceRepository.save(existing);
        }

        return raceRepository.save(Race.builder()
                .grandPrixName(meeting.meetingName())
                .season(meeting.year())
                .circuit(circuit)
                .raceDateTime(raceDateTime)
                .totalLaps(totalLaps)
                .externalSessionKey(externalKey)
                .status(status)
                .build());
    }

    private void upsertDriver(OpenF1DriverDto dto) {
        if (dto.nameAcronym() == null) return;

        Team team = null;
        if (dto.teamName() != null) {
            team = teamRepository.findByName(dto.teamName())
                    .orElseGet(() -> teamRepository.save(Team.builder()
                            .name(dto.teamName())
                            .avgPitStopSeconds(DEFAULT_TEAM_PIT_STOP_SECONDS)
                            .build()));
        }

        Team finalTeam = team;
        driverRepository.findByDriverCode(dto.nameAcronym()).ifPresentOrElse(
                existing -> {
                    existing.setTeam(finalTeam);
                    driverRepository.save(existing);
                },
                () -> driverRepository.save(Driver.builder()
                        .fullName(dto.fullName())
                        .driverCode(dto.nameAcronym())
                        .permanentNumber(dto.driverNumber())
                        .team(finalTeam)
                        .build())
        );
    }

    private void syncIncidentsAndWeather(Race race, int sessionKey) {
        try {
            List<OpenF1SessionResultDto> results = openF1Client.getSessionResult(sessionKey);

            Integer winnerDriverNumber = results.stream()
                    .filter(r -> r.position() != null && r.position() == 1)
                    .map(OpenF1SessionResultDto::driverNumber)
                    .findFirst()
                    .orElse(null);

            if (winnerDriverNumber == null) {
                log.warn("No winner found for session {}, skipping incident/weather sync", sessionKey);
                return;
            }

            List<OpenF1LapDto> referenceLaps = openF1Client.getLaps(sessionKey, winnerDriverNumber);

            incidentRepository.deleteByRaceId(race.getId());
            weatherWindowRepository.deleteByRaceId(race.getId());

            incidentSyncService.sync(race, sessionKey, winnerDriverNumber, referenceLaps);

            List<OpenF1WeatherDto> weatherSamples = openF1Client.getWeather(sessionKey);

            weatherSyncService.sync(race, weatherSamples, referenceLaps);
        } catch (Exception e) {
            log.warn("Incident/weather sync failed for session {}: {}", sessionKey, e.getMessage());
        }
    }

    private LocalDateTime parseDateTime(String isoDateTime) {
        if (isoDateTime == null) return LocalDateTime.now(ZoneOffset.UTC);
        return LocalDateTime.ofInstant(Instant.parse(isoDateTime), ZoneOffset.UTC);
    }


    public record SyncResult(int meetingsFound, int racesUpserted, int driverEntriesUpserted) {}
}