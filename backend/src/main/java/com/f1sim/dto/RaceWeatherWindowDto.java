package com.f1sim.dto;

/** One contiguous stretch of laps during which it was raining. */
public record RaceWeatherWindowDto(Integer startLap, Integer endLap) {}