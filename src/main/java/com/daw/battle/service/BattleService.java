package com.daw.battle.service;

import com.daw.battle.entity.AccountEntity;
import com.daw.battle.entity.BattleEntity;
import com.daw.battle.entity.PlayerEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.daw.battle.Constants.API_GET_URL;
import static com.daw.battle.Constants.API_UPDATE_URL;

@Service
public class BattleService {
    private final RestTemplate restTemplate = new RestTemplate();

    private Map<String, BattleEntity> battles = new ConcurrentHashMap<>();
    private Map<String, BattleEntity> waitList = new ConcurrentHashMap<>();

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
        var existsBattle = get(login);
        // TODO hardcore remove old unknown battles
        if (existsBattle != null) {
            delete(existsBattle.getId());
            existsBattle = null;
        }
        if (waitList.keySet().contains(login)) {
            waitList.remove(login);
        }

        if (!waitList.isEmpty()) {
            var existBattleCreator = waitList.keySet().stream().findFirst().get();
            existsBattle = waitList.remove(existBattleCreator);
            existsBattle.setMove(1);
            existsBattle.setStarted(true);

            var account = getAccountByLogin(login);
            var armor = calculateArmor(account);
            var damage = calculateDamage(account);
            var health = calculateHealth(account);
            existsBattle.setTeamTwo(Set.of(PlayerEntity.builder()
                            .id(account.getLogin())
                            .armor(armor)
                            .damage(damage)
                            .health(health)
                            .build()));
            var response = create(existsBattle);
            existsBattle = get(response);
            existsBattle.setPlayerId(login);
            return existsBattle;
        } else {
            var account = getAccountByLogin(login);
            var armor = calculateArmor(account);
            var damage = calculateDamage(account);
            var health = calculateHealth(account);
            var battle = BattleEntity.builder()
                    .teamOne(Set.of(PlayerEntity.builder()
                            .id(account.getLogin())
                            .armor(armor)
                            .damage(damage)
                            .health(health)
                            .build()))
                    .build();
            waitList.put(login, battle);
            battle.setPlayerId(login);
            return battle;
        }
    }

    public void cancelBattle(String login) {
        var existsBattle = get(login);
        // TODO hardcore remove old unknown battles
        if (existsBattle != null) {
            delete(existsBattle.getId());
            existsBattle = null;
        }
        if (waitList.keySet().contains(login)) {
            waitList.remove(login);
        }
    }

    public BattleEntity move(String login, String opponentId, String attack, String defence) {
        var battle = get(login);
        if (battle == null) {
            if (waitList.containsKey(login)) {
                return waitList.get(login);
            }
        }
        if (battle != null && battle.isStarted() && login != null && !login.isEmpty()) {
            var players = findPlayers(battle, login, opponentId);
            var player = players.get(0);
            if (opponentId != null && !opponentId.equals("null")) {
                var opponent = players.get(1);
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
                    battle.setMoveResults(new HashMap<>());
                    calculateFor(battle.getTeamOne(), battle.getTeamTwo(), battle.getMoveResults());
                    calculateFor(battle.getTeamTwo(), battle.getTeamOne(), battle.getMoveResults());
                    // check battle ends or not
                    if (allTeamLost(battle.getTeamOne()) || allTeamLost(battle.getTeamTwo())) {
                        battle.setBattleFinished(true);
                    } else {
                        prepareNextMove(battle);
                    }
                }
                update(battle);
            }
            battle = get(login);
            if (battle.isBattleFinished()) {
                player.setResultsViewed(true);
            }
        }
        if (allPlayersViewedResults(battle)) {
            payRewards(battle.getTeamOne());
            payRewards(battle.getTeamTwo());
            delete(battle.getId());
        }
        battle.setPlayerId(login);
        return battle;
    }

    public AccountEntity getAccountByLogin(String login) {
        var response = restTemplate.getForEntity(String.format(API_GET_URL, login), AccountEntity.class);
        return response.getBody();
    }

    private void updateAccount(AccountEntity accountEntity) {
        restTemplate.postForEntity(API_UPDATE_URL, accountEntity, AccountEntity.class);
    }

    private void payRewards(Set<PlayerEntity> team) {
        team.forEach(p -> {
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

    private void calculateFor(Set<PlayerEntity> team, Set<PlayerEntity> opponents, Map<String, Integer> moveResults) {
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
                moveResults.put(player.getId(), totalDamage);
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
        var opponentIsPresent = opponentId != null && !opponentId.equals("null");
        PlayerEntity opponent = null;
        Optional<PlayerEntity> player = battle.getTeamOne().stream()
                .filter(e -> e.getId().equals(login))
                .findFirst();
        if (player.isEmpty()) {
            player = battle.getTeamTwo().stream()
                    .filter(e -> e.getId().equals(login))
                    .findFirst();
            if (opponentIsPresent) {
                opponent = battle.getTeamOne().stream()
                        .filter(e -> e.getId().equals(opponentId))
                        .findFirst().get();
            }
        } else if (opponentIsPresent) {
            opponent = battle.getTeamTwo().stream()
                    .filter(e -> e.getId().equals(opponentId))
                    .findFirst().get();
        }
        var players = new LinkedList<PlayerEntity>();
        players.add(player.get());
        if (opponentIsPresent) {
            players.add(opponent);
        }
        return players;
    }

    private int calculateDamage(AccountEntity account) {
        int damage = 0;
        if (account.getWeapon() != null && account.getWeapon().getPoints() != null) {
            damage += account.getWeapon().getPoints();
        }
        return damage <= 0 ? 1 : damage;
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
