package com.febrie.dpp.manager;

import com.febrie.dpp.SimpleRPGMain;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ShopManager {

    private final SimpleRPGMain plugin;
    private final SkillManager skillManager;
    private final Location shopCenter;
    private final int protectionRadius = 120;

    private Villager currencyNPC;
    private Villager skillNPC;
    private Villager potionNPC;

    public ShopManager(SimpleRPGMain plugin, SkillManager skillManager) {
        this.plugin = plugin;
        this.skillManager = skillManager;
        this.shopCenter = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);

        setupShopNPCs();
    }

    private void setupShopNPCs() {
        World world = shopCenter.getWorld();
        int groundY = world.getHighestBlockYAt(shopCenter) + 1;

        currencyNPC = createNPC(new Location(world, 0, groundY, 0), "§e화폐 교환소");
        skillNPC = createNPC(new Location(world, 3, groundY, 0), "§b스킬북 상점");
        potionNPC = createNPC(new Location(world, 6, groundY, 0), "§c스킬 삭제 상점");

        setupCurrencyTrades();
        setupSkillTrades();
        setupPotionTrades();
    }

    private @NotNull Villager createNPC(@NotNull Location location, String name) {
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        villager.customName(net.kyori.adventure.text.Component.text(name));
        villager.setCustomNameVisible(true);
        villager.setInvulnerable(true);
        villager.setAI(false);
        villager.setCollidable(false);
        villager.teleport(location);
        villager.setProfession(Villager.Profession.LIBRARIAN);

        return villager;
    }

    private void setupCurrencyTrades() {
        List<MerchantRecipe> trades = new ArrayList<>();

        trades.add(createTrade(
                new ItemStack(Material.EMERALD, 200),
                null,
                createCurrency(1)
        ));

        trades.add(createTrade(
                new ItemStack(Material.NETHERITE_INGOT, 1),
                null,
                createCurrency(1)
        ));

        trades.add(createTrade(
                new ItemStack(Material.CAKE, 2),
                null,
                createAbilityRemovalTicket(1)
        ));

        currencyNPC.setRecipes(trades);
    }

    private void setupSkillTrades() {
        List<MerchantRecipe> trades = new ArrayList<>();
        String[] skills = {"burning_step", "pants_run", "crystal_protection", "flying_carpet", "rage_strike", "bomb_dog"};

        for (String skill : skills) {
            trades.add(createTrade(
                    createCurrency(10),
                    null,
                    skillManager.createSkillBook(skill)
            ));
        }

        skillNPC.setRecipes(trades);
    }

    private void setupPotionTrades() {
        List<MerchantRecipe> trades = new ArrayList<>();
        String[] skills = {"burning_step", "pants_run", "crystal_protection", "flying_carpet", "rage_strike", "bomb_dog"};
        int[] costs = {1, 2, 2, 2, 1, 2};

        for (int i = 0; i < skills.length; i++) {
            trades.add(createTrade(
                    createAbilityRemovalTicket(costs[i]),
                    null,
                    skillManager.createSkillRemovalPotion(skills[i])
            ));
        }

        potionNPC.setRecipes(trades);
    }

    private @NotNull MerchantRecipe createTrade(ItemStack ingredient1, ItemStack ingredient2, ItemStack result) {
        MerchantRecipe recipe = new MerchantRecipe(result, Integer.MAX_VALUE);
        recipe.addIngredient(ingredient1);
        if (ingredient2 != null) {
            recipe.addIngredient(ingredient2);
        }
        return recipe;
    }

    private @NotNull ItemStack createCurrency(int amount) {
        ItemStack currency = new ItemStack(Material.GOLD_NUGGET, amount);
        ItemMeta meta = currency.getItemMeta();
        meta.displayName(Component.text("§6화폐"));
        meta.lore(List.of(Component.text("§7상점에서 사용할 수 있는 화폐입니다.")));
        meta.setCustomModelData(2001);
        currency.setItemMeta(meta);
        return currency;
    }

    private @NotNull ItemStack createAbilityRemovalTicket(int amount) {
        ItemStack ticket = new ItemStack(Material.PAPER, amount);
        ItemMeta meta = ticket.getItemMeta();
        meta.displayName(Component.text("§c능력 삭제권"));
        meta.lore(List.of(Component.text("§7스킬 삭제 포션과 교환할 수 있습니다.")));
        meta.setCustomModelData(2002);
        ticket.setItemMeta(meta);
        return ticket;
    }

    public boolean isInProtectedArea(Location location) {
        return shopCenter.distance(location) <= protectionRadius;
    }

    public boolean canBypass(@NotNull Player player) {
        return player.isOp();
    }

    public void refreshNPCs() {
        if (currencyNPC != null && !currencyNPC.isDead()) {
            currencyNPC.remove();
        }
        if (skillNPC != null && !skillNPC.isDead()) {
            skillNPC.remove();
        }
        if (potionNPC != null && !potionNPC.isDead()) {
            potionNPC.remove();
        }

        setupShopNPCs();
    }
}