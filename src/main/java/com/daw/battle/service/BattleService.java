package com.daw.battle.service;


import com.daw.battle.entity.BattleEntity;
import com.daw.battle.entity.PlayerEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BattleService {
    private Map<String, BattleEntity> battles = new ConcurrentHashMap<>();

    public String create(BattleEntity battle) {
        var idBuilder = new StringBuilder();
        fillIdWith(battle.getTeamOne(), idBuilder);
        fillIdWith(battle.getTeamTwo(), idBuilder);
        var id = idBuilder.toString();

        battle.setId(id);
        battles.put(id, battle);
        return id;
    }

    public void update(BattleEntity battle) {
        battles.put(battle.getId(), battle);
    }

    public void delete(String battleId) {
        battles.remove(battleId);
    }

    public BattleEntity get(String id) {
        var battleId = battles.keySet().stream().filter(e -> e.contains(id)).findFirst().orElse(null);
        if (battleId != null) {
            return battles.get(battleId);
        }
        return null;
    }

    private StringBuilder fillIdWith(Set<PlayerEntity> players, StringBuilder id) {
        players.forEach(p -> id.append(p.getId() + ";"));
        return id;
    }
}
