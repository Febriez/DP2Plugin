package com.febrie.dpp.dto;

import org.bukkit.Location;

import java.util.UUID;

public record CoreEntityData(
    String teamName,
    UUID entityId, 
    Location location,
    long lastMoveTime,
    boolean destroyed
) {
    public static CoreEntityData create(String teamName, UUID entityId, Location location) {
        return new CoreEntityData(
            teamName,
            entityId,
            location,
            System.currentTimeMillis(),
            false
        );
    }
}