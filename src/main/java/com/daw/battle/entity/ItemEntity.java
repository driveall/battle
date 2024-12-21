package com.daw.battle.entity;

import com.daw.battle.enums.ItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ItemEntity {
    private String id;
    private String name;
    private Integer points;
    private String description;
    private String image;
    private Integer level;
    private Integer price;
    private ItemType type;
}
