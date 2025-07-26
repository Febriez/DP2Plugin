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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopManager {

    private final SimpleRPGMain plugin;
    private final SkillManager skillManager;
    private final Location shopCenter;
    private final int protectionRadius = 120;

    private Villager currencyNPC;
    private Villager skillNPC;
    private Villager potionNPC;
    
    private final Map<String, Location> registeredShops = new HashMap<>();

    public ShopManager(SimpleRPGMain plugin, SkillManager skillManager) {
        this.plugin = plugin;
        this.skillManager = skillManager;
        this.shopCenter = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);

        loadShopLocations();
        setupShopNPCs();
    }

    private void setupShopNPCs() {
        // 등록된 상점이 있으면 해당 위치에 생성
        if (registeredShops.containsKey("currency")) {
            Location loc = registeredShops.get("currency");
            currencyNPC = createNPC(loc, "§e화폐 교환소");
            setupCurrencyTrades();
        }
        
        if (registeredShops.containsKey("skill")) {
            Location loc = registeredShops.get("skill");
            skillNPC = createNPC(loc, "§b스킬북 상점");
            setupSkillTrades();
        }
        
        if (registeredShops.containsKey("potion")) {
            Location loc = registeredShops.get("potion");
            potionNPC = createNPC(loc, "§c스킬 삭제 상점");
            setupPotionTrades();
        }
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

        // 에메랄드 블록 3개 + 에메랄드 11개 = 200개
        trades.add(createTrade(
                new ItemStack(Material.EMERALD_BLOCK, 3),
                new ItemStack(Material.EMERALD, 11),
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

    @SuppressWarnings("deprecation")
    private @NotNull ItemStack createCurrency(int amount) {
        ItemStack currency = new ItemStack(Material.GOLD_NUGGET, amount);
        ItemMeta meta = currency.getItemMeta();
        meta.displayName(Component.text("§6화폐"));
        meta.lore(List.of(Component.text("§7상점에서 사용할 수 있는 화폐입니다.")));
        meta.setCustomModelData(2001);
        currency.setItemMeta(meta);
        return currency;
    }

    @SuppressWarnings("deprecation")
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
    
    public boolean registerShop(Location location, String shopType) {
        try {
            // 기존 NPC 제거
            switch (shopType) {
                case "currency":
                    if (currencyNPC != null && !currencyNPC.isDead()) {
                        currencyNPC.remove();
                    }
                    registeredShops.put("currency", location.clone());
                    currencyNPC = createNPC(location, "§e화폐 교환소");
                    setupCurrencyTrades();
                    break;
                    
                case "skill":
                    if (skillNPC != null && !skillNPC.isDead()) {
                        skillNPC.remove();
                    }
                    registeredShops.put("skill", location.clone());
                    skillNPC = createNPC(location, "§b스킬북 상점");
                    setupSkillTrades();
                    break;
                    
                case "potion":
                    if (potionNPC != null && !potionNPC.isDead()) {
                        potionNPC.remove();
                    }
                    registeredShops.put("potion", location.clone());
                    potionNPC = createNPC(location, "§c스킬 삭제 상점");
                    setupPotionTrades();
                    break;
                    
                default:
                    return false;
            }
            
            // 상점 정보 저장
            saveShopLocations();
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().warning("상점 등록 중 오류 발생: " + e.getMessage());
            return false;
        }
    }
    
    private void saveShopLocations() {
        plugin.getConfig().set("shops.currency", locationToString(registeredShops.get("currency")));
        plugin.getConfig().set("shops.skill", locationToString(registeredShops.get("skill")));
        plugin.getConfig().set("shops.potion", locationToString(registeredShops.get("potion")));
        plugin.saveConfig();
    }
    
    private void loadShopLocations() {
        if (plugin.getConfig().contains("shops.currency")) {
            registeredShops.put("currency", stringToLocation(plugin.getConfig().getString("shops.currency")));
        }
        if (plugin.getConfig().contains("shops.skill")) {
            registeredShops.put("skill", stringToLocation(plugin.getConfig().getString("shops.skill")));
        }
        if (plugin.getConfig().contains("shops.potion")) {
            registeredShops.put("potion", stringToLocation(plugin.getConfig().getString("shops.potion")));
        }
    }
    
    private String locationToString(Location loc) {
        if (loc == null) return null;
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ();
    }
    
    private Location stringToLocation(String str) {
        if (str == null) return null;
        String[] parts = str.split(",");
        return new Location(
            Bukkit.getWorld(parts[0]),
            Double.parseDouble(parts[1]),
            Double.parseDouble(parts[2]),
            Double.parseDouble(parts[3])
        );
    }
}