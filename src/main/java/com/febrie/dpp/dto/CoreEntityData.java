package com.febrie.dpp.dto;

import org.bukkit.Location;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record CoreEntityData(
        String teamName,
        UUID entityId,
        Location location,
        long lastMoveTime,
        boolean destroyed
) {
    @Contract("_, _, _ -> new")
    public static @NotNull CoreEntityData create(String teamName, UUID entityId, Location location) {
        return new CoreEntityData(
                teamName,
                entityId,
                location,
                System.currentTimeMillis(),
                false
        );
    }
}