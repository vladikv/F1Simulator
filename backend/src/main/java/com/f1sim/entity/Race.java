package com.f1sim.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "races")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Race {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String grandPrixName;

    @Column(nullable = false)
    private Integer season;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "circuit_id", nullable = false)
    private Circuit circuit;

    @Column(name = "race_datetime", nullable = false)
    private LocalDateTime raceDateTime;

    @Column(name = "total_laps")
    private Integer totalLaps;

    /** External id used to correlate with the OpenF1 API session. */
    @Column(name = "external_session_key")
    private String externalSessionKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RaceStatus status;

    public enum RaceStatus { UPCOMING, LIVE, FINISHED }
}
