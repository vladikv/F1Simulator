package com.f1sim.service;

import com.f1sim.client.OpenF1Client;
import com.f1sim.client.dto.OpenF1DriverDto;
import com.f1sim.dto.RaceDriverDto;
import com.f1sim.entity.Driver;
import com.f1sim.entity.Race;
import com.f1sim.repository.DriverRepository;
import com.f1sim.repository.RaceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RaceDriverService {

    private final RaceRepository raceRepository;
    private final DriverRepository driverRepository;
    private final OpenF1Client openF1Client;

    /**
     * Live-fetches the entry list for a race straight from OpenF1 (not
     * persisted, unlike RaceSyncService's season sync) and matches each
     * entry to our own Driver rows by driverCode/name_acronym. Entries
     * with no matching Driver are skipped — driverId must reference a
     * row StrategySimulationService can actually use.
     */
    public List<RaceDriverDto> getDriversForRace(Long raceId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new EntityNotFoundException("Race not found: " + raceId));

        if (race.getExternalSessionKey() == null) {
            log.warn("Race {} has no externalSessionKey, cannot fetch entry list", raceId);
            return List.of();
        }

        int sessionKey = Integer.parseInt(race.getExternalSessionKey());
        List<OpenF1DriverDto> entries = openF1Client.getDrivers(sessionKey);

        return entries.stream()
                .map(entry -> driverRepository.findByDriverCode(entry.nameAcronym())
                        .map(driver -> toDto(driver, entry)))
                .flatMap(Optional::stream)
                .toList();
    }

    private RaceDriverDto toDto(Driver driver, OpenF1DriverDto entry) {
        return new RaceDriverDto(
                driver.getId(),
                driver.getFullName(),
                driver.getDriverCode(),
                entry.teamName(),
                entry.driverNumber()
        );
    }
}