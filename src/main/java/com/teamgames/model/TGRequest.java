package com.teamgames.model;

import lombok.*;

@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TGRequest {
    private String playerName;
    private String teamName;
    private String subTeamName;
    private Integer row;
    private Integer column;
    private String payload;
}
