package com.f1sim.controller;

import com.f1sim.dto.RaceDriverDto;
import com.f1sim.dto.RaceSummaryDto;
import com.f1sim.dto.RaceWeatherWindowDto;
import com.f1sim.entity.Race;
import com.f1sim.repository.RaceRepository;
import com.f1sim.repository.RaceWeatherWindowRepository;
import com.f1sim.service.RaceDriverService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Comparator;

@RestController
@RequestMapping("/api/races")
@RequiredArgsConstructor
@Tag(name = "Races", description = "Public, read-only race listing")
public class RaceController {

    private final RaceRepository raceRepository;
    private final RaceDriverService raceDriverService;
    private final RaceWeatherWindowRepository raceWeatherWindowRepository;

    @GetMapping
    public List<RaceSummaryDto> listRaces(@RequestParam(required = false) Integer season) {
        return raceRepository.findAll().stream()
                .filter(r -> season == null || r.getSeason().equals(season))
                .map(this::toSummary)
                .sorted((a, b) -> b.raceDateTime().compareTo(a.raceDateTime()))
                .toList();
    }

    @GetMapping("/seasons")
    public List<Integer> listSeasons() {
        return raceRepository.findAll().stream()
                .map(Race::getSeason)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    @GetMapping("/{id}")
    public RaceSummaryDto getRace(@PathVariable Long id) {
        Race race = raceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Race not found: " + id));
        return toSummary(race);
    }

    @GetMapping("/{id}/drivers")
    public List<RaceDriverDto> getDriversForRace(@PathVariable Long id) {
        return raceDriverService.getDriversForRace(id);
    }

    @GetMapping("/{id}/weather")
    public List<RaceWeatherWindowDto> getWeatherWindows(@PathVariable Long id) {
        return raceWeatherWindowRepository.findByRaceId(id).stream()
                .map(w -> new RaceWeatherWindowDto(w.getStartLap(), w.getEndLap()))
                .toList();
    }

    private RaceSummaryDto toSummary(Race race) {
        return new RaceSummaryDto(
                race.getId(),
                race.getGrandPrixName(),
                race.getSeason(),
                race.getCircuit().getName(),
                race.getCircuit().getCountry(),
                race.getRaceDateTime(),
                race.getStatus().name(),
                race.getTotalLaps()
        );
    }
}