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
    private final GameBoard gameBoard;
    @Autowired
    private TeamPlayers teamPlayers;

    private final String TEAM1 = "team1";
    private final String TEAM2 = "team2";

    private String createTopicName(String... names) {
        return "/topic/".concat(String.join("/", names));
    }

    private String hitOrMiss(Boolean hitOrMiss) {
        return hitOrMiss ? "hit" : "miss";
    }

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
        String topic = createTopicName(teamName);
        messagingTemplate.convertAndSend(topic,
                new TGResponse(TGResponseType.PLAYERS, teamPlayers.getTeamPlayers(teamName)));
        messagingTemplate.convertAndSend(topic,
                new TGResponse(TGResponseType.ADMIN, teamPlayers.getAdminPlayer().get(teamName)));
    }

    @MessageMapping("/splitTeam")
    public void splitTeam(@Payload TGRequest TGRequest) {
        String teamName = TGRequest.getTeamName();
        String topic = createTopicName(teamName);
        teamPlayers.cleanupSubTeamMapping(teamName);
        while (!teamPlayers.isTeamMappingEmpty(teamName)) {
            teamPlayers.moveRandomMember(teamName, TEAM1);
            messagingTemplate.convertAndSend(createTopicName(teamName, TEAM1),
                    new TGResponse(TGResponseType.PLAYERS, teamPlayers.getTeam1Players(teamName)));
            messagingTemplate.convertAndSend(topic,
                    new TGResponse(TGResponseType.PLAYERS, teamPlayers.getTeamPlayers(teamName)));
            teamPlayers.moveRandomMember(teamName, TEAM2);
            messagingTemplate.convertAndSend(createTopicName(teamName, TEAM2),
                    new TGResponse(TGResponseType.PLAYERS, teamPlayers.getTeam2Players(teamName)));
            messagingTemplate.convertAndSend(topic,
                    new TGResponse(TGResponseType.PLAYERS, teamPlayers.getTeamPlayers(teamName)));
            messagingTemplate.convertAndSend(topic, new TGResponse(TGResponseType.TEAM_SPLIT_DONE, null));
        }
    }

    @MessageMapping("/endGame")
    public void endGame(@Payload TGRequest tgRquest) {
        String teamName = tgRquest.getTeamName();
        String topic = createTopicName(teamName);
        teamPlayers.removeTeamAdmin(teamName);
        gameBoard.clearTeamBoardAndScore(teamName);
        teamPlayers.cleanupSubTeamMapping(teamName);
        teamPlayers.removeTeamMapping(teamName);
        messagingTemplate.convertAndSend(topic, new TGResponse(TGResponseType.END_GAME, null));
    }

    @MessageMapping("/startGame")
    public void startGame(@Payload TGRequest tgRquest) {
        String teamName = tgRquest.getTeamName();
        String topic = createTopicName(teamName);
        gameBoard.setTurn(teamName, TEAM1);
        messagingTemplate.convertAndSend(topic, new TGResponse(TGResponseType.START_GAME, null));
    }

    @MessageMapping("/deployShip")
    public void deployShips(@Payload TGRequest tgRequest) {
        String teamName = tgRequest.getTeamName();
        String subTeamName = tgRequest.getSubTeamName();
        String payload = tgRequest.getPayload();
        Integer row = tgRequest.getRow();
        Integer col = tgRequest.getColumn();
        String topic = createTopicName(teamName, subTeamName);
        gameBoard.deploy(teamName, subTeamName, row, col, !"white".equals(payload));
        messagingTemplate.convertAndSend(topic, new TGResponse(TGResponseType.DEPLOY, payload,
                tgRequest.getRow(), tgRequest.getColumn()));
    }

    @MessageMapping("/attackShip")
    public void attackShip(@Payload TGRequest tgRequest) {
        String teamName = tgRequest.getTeamName();
        String subTeamName = tgRequest.getSubTeamName();
        String opponent = tgRequest.getPayload();
        Integer row = tgRequest.getRow();
        Integer col = tgRequest.getColumn();
        String baseTopic = createTopicName(teamName);
        String topic = createTopicName(teamName, subTeamName);
        String opponentTopic = createTopicName(teamName, opponent);
        String turn = gameBoard.getTurn(teamName);
        if (subTeamName.equals(turn)) {
            Boolean hit = gameBoard.attack(teamName, subTeamName, opponent, row, col);
            messagingTemplate.convertAndSend(opponentTopic, new TGResponse(TGResponseType.MARK_OPPONENT, hitOrMiss(hit),
                    tgRequest.getRow(), tgRequest.getColumn(), gameBoard.getScore(teamName, subTeamName)));
            messagingTemplate.convertAndSend(topic, new TGResponse(TGResponseType.MARK_SELF, hitOrMiss(hit),
                    tgRequest.getRow(), tgRequest.getColumn(), gameBoard.getScore(teamName, subTeamName)));
            gameBoard.setTurn(teamName, opponent);
        }
        messagingTemplate.convertAndSend(baseTopic, new TGResponse(TGResponseType.TURN, gameBoard.getTurn(teamName)));
    }

    @MessageMapping("/turn")
    public void getTurn(@Payload TGRequest tgRequest) {
        String teamName = tgRequest.getTeamName();
        messagingTemplate.convertAndSend(createTopicName(teamName), new TGResponse(TGResponseType.TURN, gameBoard.getTurn(teamName)));
    }
}
