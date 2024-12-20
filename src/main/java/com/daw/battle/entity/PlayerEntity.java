package com.daw.battle.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlayerEntity {
    private String id;

    // total player damage and armor
    private int damage;
    private int armor;

    private int health;

    private String attack;
    private String defense;
    private String opponent;

    private boolean moveFinished;
    private boolean resultsViewed;
}
