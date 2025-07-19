package com.febrie.dpp.dto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public record CoreGameState(
    boolean active,
    long startTime,
    long endTime,
    Map<String, CoreEntityData> teamCores,
    GamePhase currentPhase
) {
    public static CoreGameState createDefault() {
        return new CoreGameState(
            false,
            0,
            0,
            new ConcurrentHashMap<>(),
            GamePhase.WAITING
        );
    }
    
    public enum GamePhase {
        WAITING,
        PREPARATION,
        ACTIVE,
        ENDING
    }
}