package com.teamgames.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class Cell {
    private Boolean isDeployed;
    @Setter
    private String hitOrMis;
}
