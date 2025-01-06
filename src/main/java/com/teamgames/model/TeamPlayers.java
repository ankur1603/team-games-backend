package com.teamgames.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor
@Scope("singleton")
@Component
public class TeamPlayers {
    @Getter
    private final Map<String, String> adminPlayer = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> teamMapping = new ConcurrentHashMap<>();
    private final Map<String, Map<String, List<String>>> team1Mapping = new ConcurrentHashMap<>();
    private final Map<String, Map<String, List<String>>> team2Mapping = new ConcurrentHashMap<>();

    public void addTeamAdmin(String teamName, String playerName) {
        adminPlayer.computeIfAbsent(teamName, (s) -> playerName);
    }

    public void removeTeamAdmin(String teamName) {
        adminPlayer.remove(teamName);
    }

    public void cleanupSubTeamMapping(String teamName) {
        team1Mapping.remove(teamName);
        team2Mapping.remove(teamName);
    }

    public void addPlayerToTeam(String teamName, String playerName) {
        Set<String> playerSet = this.teamMapping.getOrDefault(teamName, new HashSet<>());
        playerSet.add(playerName);
        this.teamMapping.put(teamName, playerSet);
    }

    public void removePlayer(String teamName, String playerName) {
        Set<String> playerSet = this.teamMapping.get(teamName);
        playerSet.remove(playerName);
    }

    public Set<String> getTeamPlayers(String teamName) {
        return this.teamMapping.getOrDefault(teamName, new HashSet<>());
    }

    public List<String> getTeam1Players(String teamName) {
        return this.team1Mapping.getOrDefault(teamName, new HashMap<>()).getOrDefault("team1", new ArrayList<>());
    }

    public List<String> getTeam2Players(String teamName) {
        return this.team2Mapping.getOrDefault(teamName, new HashMap<>()).getOrDefault("team2", new ArrayList<>());
    }


    public boolean isTeamMappingEmpty(String teamName) {
        return this.teamMapping.getOrDefault(teamName, new HashSet<>()).isEmpty();
    }

    public void moveRandomMember(String teamName, String subTeamName) {
        Set<String> players = this.teamMapping.getOrDefault(teamName, new HashSet<>());
        Optional<String> choosenPlayerOption = players.stream().findAny();

        if (!players.isEmpty()) {
            String choosenPlayer = choosenPlayerOption.get();
            if ("team1".equals(subTeamName)) {
                Map<String, List<String>> team1 = this.team1Mapping.getOrDefault(teamName, new HashMap<>());
                List<String> team1Players = team1.getOrDefault(subTeamName, new ArrayList<>());
                team1Players.add(choosenPlayer);
                team1.put(subTeamName, team1Players);
                this.team1Mapping.put(teamName, team1);
            } else if ("team2".equals(subTeamName)) {
                Map<String, List<String>> team2 = this.team1Mapping.getOrDefault(teamName, new HashMap<>());
                List<String> team2Players = team2.getOrDefault(subTeamName, new ArrayList<>());
                team2Players.add(choosenPlayer);
                team2.put(subTeamName, team2Players);
                this.team2Mapping.put(teamName, team2);
            }
            players.remove(choosenPlayer);
        }

    }
}
