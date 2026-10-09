package com.f1sim.controller;

import com.f1sim.dto.ProfileSimulationDto;
import com.f1sim.dto.StintSummaryDto;
import com.f1sim.entity.StrategySimulation;
import com.f1sim.entity.User;
import com.f1sim.repository.StrategySimulationRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Current user's simulation history")
public class ProfileController {

    private final StrategySimulationRepository simulationRepository;

    @GetMapping("/history")
    public List<ProfileSimulationDto> getHistory(@AuthenticationPrincipal User currentUser) {
        return simulationRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    private ProfileSimulationDto toDto(StrategySimulation s) {
        List<StintSummaryDto> stints = s.getStints().stream()
                .map(stint -> new StintSummaryDto(stint.getCompound(), stint.getStartLap(), stint.getEndLap()))
                .toList();

        return new ProfileSimulationDto(
                s.getId(),
                s.getRace().getGrandPrixName(),
                s.getRace().getSeason(),
                s.getDriver().getDriverCode(),
                s.getCreatedAt(),
                s.getPredictedTotalTimeSeconds(),
                s.getDeltaVsActualSeconds(),
                stints
        );
    }
}