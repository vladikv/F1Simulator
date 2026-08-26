package com.f1sim.controller;

import com.f1sim.dto.RaceSummaryDto;
import com.f1sim.entity.Race;
import com.f1sim.repository.RaceRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/races")
@RequiredArgsConstructor
@Tag(name = "Races", description = "Public, read-only race listing")
public class RaceController {

    private final RaceRepository raceRepository;

    @GetMapping
    public List<RaceSummaryDto> listRaces() {
        return raceRepository.findAll().stream()
                .map(this::toSummary)
                .sorted((a, b) -> b.raceDateTime().compareTo(a.raceDateTime()))
                .toList();
    }

    @GetMapping("/{id}")
    public RaceSummaryDto getRace(@PathVariable Long id) {
        Race race = raceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Race not found: " + id));
        return toSummary(race);
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