package com.f1sim.dto;

/** One row of the public leaderboard, ranked by accuracy rating. */
public record LeaderboardEntryDto(
        Long userId,
        String username,
        Double ratingScore
) {}