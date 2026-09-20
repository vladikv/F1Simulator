package com.f1sim.entity;

import jakarta.persistence.*;
import lombok.*;

/** A contiguous stretch of laps during which it was raining. */
@Entity
@Table(name = "race_weather_windows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RaceWeatherWindow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id", nullable = false)
    private Race race;

    @Column(name = "start_lap", nullable = false)
    private Integer startLap;

    @Column(name = "end_lap", nullable = false)
    private Integer endLap;
}