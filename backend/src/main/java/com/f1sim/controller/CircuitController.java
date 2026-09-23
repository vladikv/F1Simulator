package com.f1sim.controller;

import com.f1sim.dto.CircuitSummaryDto;
import com.f1sim.repository.CircuitRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/circuits")
@RequiredArgsConstructor
@Tag(name = "Circuits", description = "Public, read-only circuit listing")
public class CircuitController {

    private final CircuitRepository circuitRepository;

    @GetMapping
    public List<CircuitSummaryDto> listCircuits() {
        return circuitRepository.findAll().stream()
                .map(c -> new CircuitSummaryDto(c.getId(), c.getName(), c.getCountry()))
                .toList();
    }
}