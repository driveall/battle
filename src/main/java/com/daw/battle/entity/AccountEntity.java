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
public class AccountEntity {
    private String login;
    private String password;
    private String passwordConfirmed;
    private String email;
    private String phone;

    private Integer money;
    private ItemEntity head;
    private ItemEntity body;
    private ItemEntity legs;
    private ItemEntity weapon;
    private Set<ItemEntity> storage;
    private Integer level;
    private Integer points;

    private String createdAt;
    private String updatedAt;
    private String passwordChangedAt;
}
