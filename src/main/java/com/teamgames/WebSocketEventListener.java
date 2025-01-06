package com.teamgames;

import com.teamgames.model.TGResponse;
import com.teamgames.model.TGResponseType;
import com.teamgames.model.TeamPlayers;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Component
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
@Log4j2
public class WebSocketEventListener {

    @Autowired
    private final SimpMessageSendingOperations messagingTemplate;
    @Autowired
    private final TeamPlayers teamPlayers;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String,String> simpSessionAttr = (Map<String, String>) headerAccessor.getMessageHeaders().get("simpSessionAttributes");

        String playerName = simpSessionAttr.get("playerName");
        String teamName = simpSessionAttr.get("teamName");
        if (playerName != null && teamName !=null) {
            log.info("user disconnected: {}", playerName);
            teamPlayers.removePlayer(teamName, playerName);
            messagingTemplate.convertAndSend("/topic/".concat(teamName), new TGResponse(TGResponseType.PLAYERS,teamPlayers.getTeamPlayers(teamName)));
        }
    }

}
