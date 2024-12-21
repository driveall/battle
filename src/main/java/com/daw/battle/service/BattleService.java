package com.daw.battle.service;

import com.daw.battle.entity.AccountEntity;
import com.daw.battle.entity.BattleEntity;
import com.daw.battle.entity.PlayerEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.daw.battle.Constants.API_GET_URL;
import static com.daw.battle.Constants.API_UPDATE_URL;

@Service
public class BattleService {
    private final RestTemplate restTemplate = new RestTemplate();

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

    public BattleEntity startBattleWithBot(String login) {
        var existsBattle = get(login);
        // TODO hardcore remove old unknown battle
        if (existsBattle != null) {
            delete(existsBattle.getId());
        }
        var account = getAccountByLogin(login);
        var armor = calculateArmor(account);
        var damage = calculateDamage(account);
        var health = calculateHealth(account);
        var battle = BattleEntity.builder()
                .move(1)
                .started(true)
                .teamOne(Set.of(PlayerEntity.builder()
                        .id(account.getLogin())
                        .armor(armor)
                        .damage(damage)
                        .health(health)
                        .build()))
                .teamTwo(Set.of(PlayerEntity.builder()
                        .id("BotLevel" + account.getLevel())
                        .armor(armor)
                        .damage(damage)
                        .health(health)
                        .resultsViewed(true)
                        .build()))
                .build();
        var response = create(battle);
        battle = get(response);
        battle.setPlayerId(login);
        return battle;
    }

    public BattleEntity startBattle(String login) {
        // TODO add functionality
        return null;
    }

    public BattleEntity move(String login, String opponentId, String attack, String defence) {
        var battle = get(login);
        if (battle != null && login != null && !login.isEmpty()) {
            var players = findPlayers(battle, login, opponentId);
            var player = players.get(0);
            var opponent = players.get(1);
            if (attack != null && !attack.isEmpty()
                    && defence != null && !defence.isEmpty()
                    && opponentId != null && !opponentId.isEmpty()) {
                player.setAttack(attack);
                player.setDefense(defence);
                player.setOpponent(opponentId);
                player.setMoveFinished(true);
                // TODO hardcoded opponent is bot so it finishes move now
                if (opponentId.contains("BotLevel")) {
                    opponent.setAttack("Center");
                    opponent.setDefense("Center");
                    opponent.setOpponent(login);
                    opponent.setMoveFinished(true);
                }
                // set teams finished
                if (teamFinished(battle.getTeamOne())) {
                    battle.setTeamOneFinish(true);
                }
                if (teamFinished(battle.getTeamTwo())) {
                    battle.setTeamTwoFinish(true);
                }
                // calculate
                if (battle.isTeamOneFinish() && battle.isTeamTwoFinish()) {
                    calculateFor(battle.getTeamOne(), battle.getTeamTwo());
                    calculateFor(battle.getTeamTwo(), battle.getTeamOne());
                    // check battle ends or not
                    if (allTeamLost(battle.getTeamOne()) || allTeamLost(battle.getTeamTwo())) {
                        battle.setBattleFinished(true);
                    } else {
                        prepareNextMove(battle);
                    }
                }
                if (battle.isBattleFinished()) {
                    player.setResultsViewed(true);
                }
                update(battle);
            }
            battle = get(login);

            if (allPlayersViewedResults(battle)) {
                payRewards(battle);
                delete(battle.getId());
            }
            battle.setPlayerId(login);
        }
        return battle;
    }

    public AccountEntity getAccountByLogin(String login) {
        var response = restTemplate.getForEntity(String.format(API_GET_URL, login), AccountEntity.class);
        return response.getBody();
    }

    private void updateAccount(AccountEntity accountEntity) {
        restTemplate.postForEntity(API_UPDATE_URL, accountEntity, AccountEntity.class);
    }

    private void payRewards(BattleEntity battle) {
        battle.getTeamOne().forEach(p -> {
            if (p.getHealth() > 0) {
                var account = getAccountByLogin(p.getId());
                account.setMoney(account.getMoney() + 2);
                account.setPoints(account.getPoints() + p.getHealth());
                if (account.getPoints() >= account.getLevel() * account.getLevel() * account.getLevel() * 100) {
                    account.setLevel(account.getLevel() + 1);
                }
                updateAccount(account);
            }
        });
    }

    private boolean allPlayersViewedResults(BattleEntity battle) {
        var viewed = new AtomicBoolean(true);
        battle.getTeamOne().forEach(p -> {
            if (!p.isResultsViewed()) {
                viewed.set(false);
            }
        });
        battle.getTeamTwo().forEach(p -> {
            if (!p.isResultsViewed()) {
                viewed.set(false);
            }
        });
        return viewed.get();
    }

    private void prepareNextMove(BattleEntity battle) {
        battle.getTeamOne().forEach(player -> {
            player.setDefense(null);
            player.setAttack(null);
            player.setOpponent(null);
            player.setMoveFinished(false);
        });
        battle.getTeamTwo().forEach(player -> {
            player.setDefense(null);
            player.setAttack(null);
            player.setOpponent(null);
            player.setMoveFinished(false);
        });
        battle.setMove(battle.getMove() + 1);
        battle.setTeamOneFinish(false);
        battle.setTeamTwoFinish(false);
    }

    private boolean allTeamLost(Set<PlayerEntity> team) {
        var allPlayersLost = new AtomicBoolean(true);
        team.forEach(p -> {
            if (p.getHealth() > 0) {
                allPlayersLost.set(false);
            }
        });
        return allPlayersLost.get();
    }

    private void calculateFor(Set<PlayerEntity> team, Set<PlayerEntity> opponents) {
        team.forEach(player -> {
            var opponent = opponents.stream()
                    .filter(p -> player.getOpponent().equals(p.getId()))
                    .findFirst().get();
            if (player.getAttack().equals(opponent.getDefense())) {
                var totalDamage = player.getDamage() - opponent.getArmor();
                if (totalDamage < 1) {
                    totalDamage = 1;
                }
                opponent.setHealth(opponent.getHealth() - totalDamage);
            }
        });
    }

    private boolean teamFinished(Set<PlayerEntity> players) {
        var allFinished = new AtomicBoolean(true);
        players.forEach(player -> {
            if (!player.isMoveFinished()) {
                allFinished.set(false);
            }
        });
        return allFinished.get();
    }

    private List<PlayerEntity> findPlayers(BattleEntity battle, String login, String opponentId) {
        PlayerEntity opponent = null;
        PlayerEntity player = battle.getTeamOne().stream()
                .filter(e -> e.getId().equals(login))
                .findFirst().get();
        if (player.getId() == null) {
            player = battle.getTeamTwo().stream()
                    .filter(e -> e.getId().equals(login))
                    .findFirst().get();
            opponent = battle.getTeamOne().stream()
                    .filter(e -> e.getId().equals(opponentId))
                    .findFirst().get();
        } else {
            opponent = battle.getTeamTwo().stream()
                    .filter(e -> e.getId().equals(opponentId))
                    .findFirst().get();
        }
        var players = new LinkedList<PlayerEntity>();
        players.add(player);
        players.add(opponent);
        return players;
    }

    private int calculateDamage(AccountEntity account) {
        int damage = 0;
        if (account.getWeapon() != null) {
            damage += account.getWeapon().getPoints();
        }
        return damage;
    }

    private int calculateArmor(AccountEntity account) {
        int armor = 0;
        if (account.getHead() != null && account.getHead().getPoints() != null) {
            armor += account.getHead().getPoints();
        }
        if (account.getLegs() != null && account.getLegs().getPoints() != null) {
            armor += account.getLegs().getPoints();
        }
        if (account.getBody() != null && account.getBody().getPoints() != null) {
            armor += account.getBody().getPoints();
        }
        return armor;
    }

    private int calculateHealth(AccountEntity account) {
        return (account.getLevel() * 10) + (account.getPoints() / 10);
    }

    private StringBuilder fillIdWith(Set<PlayerEntity> players, StringBuilder id) {
        players.forEach(p -> id.append(p.getId() + ";"));
        return id;
    }
}
