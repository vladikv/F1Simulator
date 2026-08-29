package com.f1sim.dto;

/** A driver as entered in a specific race — used to populate the driver picker. */
public record RaceDriverDto(
        Long id,
        String fullName,
        String driverCode,
        String teamName,
        Integer permanentNumber
) {}