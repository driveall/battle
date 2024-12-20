package com.daw.battle.controller;

import com.daw.battle.entity.BattleEntity;
import com.daw.battle.service.BattleService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.*;

@RestController()
@Slf4j
public class BattleController {

    private final BattleService battleService;

    public BattleController(BattleService battleService) {
        this.battleService = battleService;
    }

    @GetMapping("/battle/get")
    public BattleEntity get(@NonNull @RequestParam String battleId) {
        log.info("battle get for {}", battleId);
        return battleService.get(battleId);
    }

    @PostMapping("/battle/create")
    public String create(@NonNull @RequestBody BattleEntity battle) {
        log.info("battle create for {}", battle.getTeamOne().stream().findFirst().get().getId());
        return battleService.create(battle);
    }

    @PutMapping("/battle/update")
    public BattleEntity update(@NonNull @RequestBody BattleEntity battle) {
        log.info("battle update for {}", battle.getId());
        battleService.update(battle);
        return battle;
    }

    @DeleteMapping("/battle/delete")
    public HttpStatusCode delete(@NonNull @RequestParam String battleId) {
        log.info("battle delete for {}", battleId);
        battleService.delete(battleId);
        return HttpStatusCode.valueOf(200);
    }

}
