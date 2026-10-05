package com.f1sim.dto;

import com.f1sim.enums.TyreCompound;

public record StintSummaryDto(TyreCompound compound, Integer startLap, Integer endLap) {}