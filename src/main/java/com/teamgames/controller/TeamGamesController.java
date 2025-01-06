package com.teamgames.controller;

import com.teamgames.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class TeamGamesController {

    @Autowired
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    private TeamPlayers teamPlayers;

    @Autowired
    private final GameBoard gameBoard;

    @MessageMapping("/joinGame")
    public void joinGame(
            @Payload TGRequest TGRequest,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String playerName = TGRequest.getPlayerName();
        String teamName = TGRequest.getTeamName();
        headerAccessor.getSessionAttributes().put("playerName", playerName);
        headerAccessor.getSessionAttributes().put("teamName", teamName);
        teamPlayers.addPlayerToTeam(teamName, playerName);
        teamPlayers.addTeamAdmin(teamName, playerName);
        String topic = "/topic/" + teamName;
        messagingTemplate.convertAndSend(topic, new TGResponse(TGResponseType.PLAYERS, teamPlayers.getTeamPlayers(teamName)));
        messagingTemplate.convertAndSend(topic, new TGResponse(TGResponseType.ADMIN, teamPlayers.getAdminPlayer().get(teamName)));
    }

    @MessageMapping("/splitTeam")
    public void splitTeam(@Payload TGRequest TGRequest) throws InterruptedException {
        String teamName = TGRequest.getTeamName();
        String topic = "/topic/" + teamName;
        teamPlayers.cleanupSubTeamMapping(teamName);
        String admin = teamPlayers.getTeamPlayers(teamName).stream().findAny().orElseGet(() -> "");
        while (!teamPlayers.isTeamMappingEmpty(teamName)) {
            teamPlayers.moveRandomMember(teamName, "team1");
            Thread.sleep(100);
            messagingTemplate.convertAndSend(topic + "/team1", new TGResponse(TGResponseType.PLAYERS, teamPlayers.getTeam1Players(teamName)));
            messagingTemplate.convertAndSend(topic, new TGResponse(TGResponseType.PLAYERS, teamPlayers.getTeamPlayers(teamName)));
            teamPlayers.moveRandomMember(teamName, "team2");
            Thread.sleep(100);
            messagingTemplate.convertAndSend(topic + "/team2", new TGResponse(TGResponseType.PLAYERS, teamPlayers.getTeam2Players(teamName)));
            messagingTemplate.convertAndSend(topic, new TGResponse(TGResponseType.PLAYERS, teamPlayers.getTeamPlayers(teamName)));
            messagingTemplate.convertAndSend(topic, new TGResponse(TGResponseType.TEAM_SPLIT_DONE, null));
        }
    }

    @MessageMapping("/endGame")
    public void endGame(@Payload TGRequest tgRquest) {
        String playerName = tgRquest.getPlayerName();
        String teamName = tgRquest.getTeamName();
        String topic = "/topic/" + teamName;
        teamPlayers.removeTeamAdmin(teamName);
        gameBoard.clearTeamBoardAndScore(teamName);
        messagingTemplate.convertAndSend(topic, new TGResponse(TGResponseType.END_GAME, null));
    }

    @MessageMapping("/startGame")
    public void startGame(@Payload TGRequest tgRquest) {
        String playerName = tgRquest.getPlayerName();
        String teamName = tgRquest.getTeamName();
        String topic = "/topic/" + teamName;
        messagingTemplate.convertAndSend(topic, new TGResponse(TGResponseType.START_GAME, null));
    }

    @MessageMapping("/deployShip")
    public void deployShips(@Payload TGRequest tgRequest) {
        String playerName = tgRequest.getPlayerName();
        String teamName = tgRequest.getTeamName();
        String subTeamName = tgRequest.getSubTeamName();
        String payload = tgRequest.getPayload();
        Integer row = tgRequest.getRow();
        Integer col = tgRequest.getColumn();
        String topic = "/topic/" + teamName + "/" + subTeamName;
        gameBoard.deploy(teamName, subTeamName, row, col, !"white".equals(payload));
        messagingTemplate.convertAndSend(topic, new TGResponse(TGResponseType.DEPLOY, payload, tgRequest.getRow(), tgRequest.getColumn()));
    }

    @MessageMapping("/attackShip")
    public void attackShip(@Payload TGRequest tgRequest) {
        String playerName = tgRequest.getPlayerName();
        String teamName = tgRequest.getTeamName();
        String subTeamName = tgRequest.getSubTeamName();
        String opponent = tgRequest.getPayload();
        Integer row = tgRequest.getRow();
        Integer col = tgRequest.getColumn();
        String topic = "/topic/" + teamName + "/" + subTeamName;
        String opponentTopic = "/topic/" + teamName + "/" + opponent;
        Boolean hit = gameBoard.attack(teamName,subTeamName, opponent, row, col);
        messagingTemplate.convertAndSend(opponentTopic, new TGResponse(TGResponseType.MARK_OPPONENT, hit ? "hit" : "miss", tgRequest.getRow(), tgRequest.getColumn(), gameBoard.getScore(teamName, opponent)));
        messagingTemplate.convertAndSend(topic, new TGResponse(TGResponseType.MARK_SELF, hit ? "hit" : "miss" , tgRequest.getRow(), tgRequest.getColumn(), gameBoard.getScore(teamName, subTeamName)));
    }

}
