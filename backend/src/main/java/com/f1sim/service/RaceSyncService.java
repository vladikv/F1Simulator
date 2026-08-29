package com.f1sim.service;

import com.f1sim.client.OpenF1Client;
import com.f1sim.client.dto.OpenF1DriverDto;
import com.f1sim.client.dto.OpenF1MeetingDto;
import com.f1sim.client.dto.OpenF1SessionDto;
import com.f1sim.entity.Circuit;
import com.f1sim.entity.Driver;
import com.f1sim.entity.Race;
import com.f1sim.entity.Team;
import com.f1sim.repository.CircuitRepository;
import com.f1sim.repository.DriverRepository;
import com.f1sim.repository.RaceRepository;
import com.f1sim.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RaceSyncService {

    private static final double DEFAULT_LAP_LENGTH_KM = 5.0;
    private static final double DEFAULT_PIT_LANE_LOSS_SECONDS = 22.0;
    private static final int DEFAULT_OVERTAKING_DIFFICULTY = 5;
    private static final double DEFAULT_TEAM_PIT_STOP_SECONDS = 2.4;

    private final OpenF1Client openF1Client;
    private final CircuitRepository circuitRepository;
    private final RaceRepository raceRepository;
    private final TeamRepository teamRepository;
    private final DriverRepository driverRepository;

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
            } finally {
                throttle();
            }

            for (OpenF1SessionDto session : raceSessions) {
                Race race = upsertRace(meeting, session, circuit);
                racesCreated++;

                List<OpenF1DriverDto> drivers = openF1Client.getDrivers(session.sessionKey());
                throttle();
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

        if (existing != null) {
            existing.setStatus(status);
            return raceRepository.save(existing);
        }

        return raceRepository.save(Race.builder()
                .grandPrixName(meeting.meetingName())
                .season(meeting.year())
                .circuit(circuit)
                .raceDateTime(raceDateTime)
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

    private LocalDateTime parseDateTime(String isoDateTime) {
        if (isoDateTime == null) return LocalDateTime.now(ZoneOffset.UTC);
        return LocalDateTime.ofInstant(Instant.parse(isoDateTime), ZoneOffset.UTC);
    }

    // OpenF1 caps free-tier usage at 30 requests/minute. A full season sync
    // can easily need 50+ calls (sessions + drivers, per meeting), so pace
    // requests instead of firing them all at once — 2.5s spacing keeps us
    // safely under 24 requests/minute even in the worst case.
    private static final long OPENF1_REQUEST_SPACING_MS = 2_500;

    private void throttle() {
        try {
            Thread.sleep(OPENF1_REQUEST_SPACING_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public record SyncResult(int meetingsFound, int racesUpserted, int driverEntriesUpserted) {}
}