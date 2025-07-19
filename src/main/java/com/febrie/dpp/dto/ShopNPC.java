package com.febrie.dpp.dto;

import org.bukkit.Location;

import java.util.List;

public record ShopNPC(
    int npcId,
    Location location,
    List<TradeEntry> trades,
    String npcType
) {}