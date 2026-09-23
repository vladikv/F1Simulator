package com.f1sim.dto;

/** One row of a per-circuit leaderboard, ranked by prediction accuracy. */
public record LeaderboardEntryDto(Long userId, String username, Double bestAbsDeltaSeconds) {}