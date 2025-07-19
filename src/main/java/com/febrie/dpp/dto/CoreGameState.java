package com.febrie.dpp.dto;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public record CoreGameState(
        boolean active,
        long startTime,
        long endTime,
        Map<String, CoreEntityData> teamCores,
        GamePhase currentPhase
) {
    @Contract(" -> new")
    public static @NotNull CoreGameState createDefault() {
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