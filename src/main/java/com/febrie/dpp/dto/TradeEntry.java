package com.febrie.dpp.dto;

import org.bukkit.inventory.ItemStack;

public record TradeEntry(
    ItemStack costItem,
    ItemStack resultItem,
    boolean enabled
) {}