package com.f1sim.controller;

import com.f1sim.dto.LeaderboardEntryDto;
import com.f1sim.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
@Tag(name = "Leaderboard", description = "Public ranking by strategy prediction accuracy")
public class LeaderboardController {

    private final UserRepository userRepository;

    @GetMapping
    public List<LeaderboardEntryDto> getLeaderboard() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "ratingScore")).stream()
                .limit(50)
                .map(u -> new LeaderboardEntryDto(u.getId(), u.getUsername(), u.getRatingScore()))
                .toList();
    }
}