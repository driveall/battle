package com.daw.battle.controller;

import com.daw.battle.entity.BattleEntity;
import com.daw.battle.service.BattleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

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

    @PostMapping("/battle/bot/start")
    public BattleEntity battleWithBotStart(@RequestParam String login) {
        log.info("battle with bot start for {}", login);
        return battleService.startBattleWithBot(login);
    }

    @PostMapping("/battle/start")
    public BattleEntity battleStart(@RequestParam String login,
                                    HttpServletRequest req,
                                    HttpServletResponse res) {
        log.info("battle start for {}", login);
        return battleService.startBattle(login);
    }

    @PostMapping("/battle/move")
    public BattleEntity move(@RequestParam String login,
                             @RequestParam(required = false) String attack,
                             @RequestParam(required = false) String defence,
                             @RequestParam(required = false) String opponent,
                             HttpServletRequest req) {
        log.info("battle move for {}", login);
        return battleService.move(login, opponent, attack, defence);
    }
}
