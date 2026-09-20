package com.f1sim.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * One race-control event that cost time on track (red flag, safety
 * car, VSC). Used to adjust the actual race time before comparing
 * it against a predicted strategy, so the model isn't penalized for
 * incidents it has no way to foresee.
 */
@Entity
@Table(name = "race_incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RaceIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id", nullable = false)
    private Race race;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentType type;

    @Column(name = "start_lap", nullable = false)
    private Integer startLap;

    @Column(name = "end_lap")
    private Integer endLap; // null while ongoing at sync time — rare, but possible

    /** Estimated time lost to this incident, in seconds — used to adjust actualTime. */
    @Column(name = "time_loss_seconds")
    private Double timeLossSeconds;

    public enum IncidentType {
        RED_FLAG, SAFETY_CAR, VIRTUAL_SAFETY_CAR
    }
}