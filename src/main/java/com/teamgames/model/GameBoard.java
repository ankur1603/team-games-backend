package com.teamgames.model;

import lombok.NoArgsConstructor;
import lombok.Synchronized;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor
@Scope("singleton")
@Component
public class GameBoard {
    private final Map<String, Map<String, Boolean>> teamReady = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Integer>> teamScore = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Cell[][]>> teamBoard = new ConcurrentHashMap<>();
    private final Map<String, String> turnMapping = new ConcurrentHashMap<>();

    @Synchronized
    public void setTurn(String teamName, String subTeamName) {
        this.turnMapping.put(teamName,subTeamName);
    }

    @Synchronized
    public String getTurn(String teamName) {
        return this.turnMapping.get(teamName);
    }

    public void clearTeamBoardAndScore(String teamName) {
        teamScore.remove(teamName);
        teamBoard.remove(teamName);
    }

    public Integer getScore(String teamName, String subTeamName) {
        Map<String, Integer> scoreMapping = this.teamScore.getOrDefault(teamName, new HashMap<>());
        return scoreMapping.getOrDefault(subTeamName, 0);
    }

    public void incrementScore(String teamName, String subTeamName) {
        this.teamScore.compute(teamName, (key, value) -> {
            if (value == null) {
                value = new HashMap<>();
            }
            value.compute(subTeamName, (k, v) -> {
                if (v == null) {
                    v = 0;
                }
                v++;
                return v;
            });

            return value;
        });
    }


    public void deploy(String teamName,
                       String subTeamName,
                       Integer row,
                       Integer col,
                       Boolean isDeployed) {
        this.teamBoard.compute(teamName, (key, value) -> {
            if (value == null) {
                value = new HashMap<>();
            }
            value.compute(subTeamName, (k, v) -> {
                if (v == null) {
                    v = new Cell[10][10];
                }
                v[row][col] = new Cell(isDeployed, "");
                return v;
            });
            return value;
        });
    }

    public Boolean attack(String teamName,
                          String subTeamName,
                          String opponentName,
                          Integer row,
                          Integer col) {
        Map<String, Cell[][]> boardMapping = this.teamBoard.getOrDefault(teamName, new HashMap<>());
        Cell[][] board = boardMapping.getOrDefault(opponentName, new Cell[10][10]);
        Cell cell = board[row][col];
        Boolean hit = cell != null && cell.getIsDeployed();
        if (hit) {
            if(!"hit".equals(cell.getHitOrMis()))
                incrementScore(teamName, subTeamName);
            cell.setHitOrMis("hit");
        }

        return hit;
    }
}
