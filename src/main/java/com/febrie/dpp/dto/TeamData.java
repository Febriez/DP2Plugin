package com.febrie.dpp.dto;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public record TeamData(
    String teamName,
    Set<UUID> members,
    boolean eliminated,
    boolean won,
    long createdTime
) {
    public static TeamData createDefault(String teamName) {
        return new TeamData(
            teamName,
            ConcurrentHashMap.newKeySet(),
            false,
            false,
            System.currentTimeMillis()
        );
    }
    
    public boolean isFull() {
        return members.size() >= 2;
    }
    
    public boolean isEmpty() {
        return members.isEmpty();
    }
}