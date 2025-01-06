package com.teamgames.model;

import lombok.*;

@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TGResponse {
    private TGResponseType type;
    private Object payload;
    private Integer row;
    private Integer column;
    private Integer score;

    public TGResponse(TGResponseType type) {
        this.type = type;
    }

    public TGResponse(TGResponseType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public TGResponse(TGResponseType type, Object payload, Integer row, Integer column) {
        this.type = type;
        this.payload = payload;
        this.row = row;
        this.column = column;
    }
}
