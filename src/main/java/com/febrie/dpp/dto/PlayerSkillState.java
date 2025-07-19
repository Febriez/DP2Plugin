package com.febrie.dpp.dto;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record PlayerSkillState(
        UUID playerId,
        String playerName,
        boolean burningStep,
        boolean pantsRun,
        boolean crystalProtection,
        boolean flyingCarpet,
        boolean rageStrike,
        boolean bombDog,
        int virtualLevel,
        long lastUpdated
) {
    @Contract("_, _ -> new")
    public static @NotNull PlayerSkillState createDefault(UUID playerId, String playerName) {
        return new PlayerSkillState(
                playerId,
                playerName,
                false, false, false, false, false, false,
                0,
                System.currentTimeMillis()
        );
    }
}