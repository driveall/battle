package com.daw.battle.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BattleEntity {
    private String id;
    private String playerId;

    private Set<PlayerEntity> teamOne;
    private Set<PlayerEntity> teamTwo;

    private int move = 0;
    private boolean started = false;

    private boolean teamOneFinish = false;
    private boolean teamTwoFinish = false;
    private boolean battleFinished = false;
}
