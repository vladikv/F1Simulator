package com.f1sim.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents an F1 circuit. Pit lane time loss and total lap count
 * are the two fields the Strategy Engine relies on most, since they
 * directly affect how costly each pit stop is.
 */
@Entity
@Table(name = "circuits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Circuit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String country;

    @Column(name = "total_laps")
    private Integer totalLaps;

    @Column(name = "lap_length_km", nullable = false)
    private Double lapLengthKm;

    /** Average time lost by entering and exiting the pit lane, in seconds. */
    @Column(name = "pit_lane_time_loss_seconds", nullable = false)
    private Double pitLaneTimeLossSeconds;

    /** Historical overtaking difficulty, 1 (easy) to 10 (very hard) — used to weight undercut value. */
    @Column(name = "overtaking_difficulty")
    private Integer overtakingDifficulty;

    /** OpenF1's circuit_key, used to match this row on re-sync instead of duplicating it. */
    @Column(name = "external_circuit_key", unique = true)
    private Integer externalCircuitKey;
}