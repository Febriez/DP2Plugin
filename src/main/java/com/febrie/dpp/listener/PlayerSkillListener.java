package com.febrie.dpp.listener;

import com.febrie.dpp.manager.CooldownManager;
import com.febrie.dpp.manager.SkillManager;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerSkillListener implements Listener {

    private final SkillManager skillManager;
    private final CooldownManager cooldownManager;
    private final Map<UUID, BukkitRunnable> flyingCarpetTasks = new HashMap<>();

    public PlayerSkillListener(SkillManager skillManager, CooldownManager cooldownManager) {
        this.skillManager = skillManager;
        this.cooldownManager = cooldownManager;
    }

    @EventHandler
    public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            String skillId = skillManager.getSkillIdFromItem(item);
            if (skillId != null) {
                if (skillManager.learnSkill(player, skillId)) {
                    item.setAmount(item.getAmount() - 1);
                }
                event.setCancelled(true);
                return;
            }

            if (item.getType() == Material.WOODEN_SWORD || item.getType() == Material.STONE_SWORD ||
                    item.getType() == Material.IRON_SWORD || item.getType() == Material.GOLDEN_SWORD ||
                    item.getType() == Material.DIAMOND_SWORD || item.getType() == Material.NETHERITE_SWORD) {

                useBurningStep(player);
            } else if (item.getType() == Material.SHIELD) {
                useCrystalProtection(player);
            }
        }
    }

    @EventHandler
    public void onProjectileLaunch(@NotNull ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;

        Projectile projectile = event.getEntity();

        if (projectile instanceof ThrownExpBottle && player.getInventory().getItemInMainHand().getType() == Material.BOOK) {
            usePantsRun(player, projectile);
        } else if (projectile instanceof Arrow && player.getInventory().getItemInMainHand().getType() == Material.BONE) {
            useBombDog(player, projectile.getLocation());
            player.getInventory().getItemInMainHand().setAmount(
                    player.getInventory().getItemInMainHand().getAmount() - 1);
        }
    }

    @EventHandler
    public void onPlayerItemConsume(@NotNull PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        String skillId = skillManager.getSkillIdFromItem(item);
        if (skillId != null) {
            skillManager.removeSkill(player, skillId);
        }
    }

    @EventHandler
    public void onVehicleEnter(@NotNull VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player player)) return;
        if (!(event.getVehicle() instanceof Minecart)) return;

        startFlyingCarpet(player, (Minecart) event.getVehicle());
    }

    @EventHandler
    public void onVehicleExit(@NotNull VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player player)) return;

        stopFlyingCarpet(player);
    }

    private void useBurningStep(Player player) {
        if (!cooldownManager.checkCooldown(player, "burning_step")) {
            String message = cooldownManager.formatCooldownMessage(player, "burning_step");
            if (message != null) player.sendMessage(Component.text(message));
            return;
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100, 0));

        Vector direction = player.getLocation().getDirection().normalize();
        Location dashLocation = player.getLocation().add(direction.multiply(5));
        player.teleport(dashLocation);

        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 20, 1, 1, 1, 0.1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 1);

        for (Entity entity : player.getNearbyEntities(3, 3, 3)) {
            if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                ((LivingEntity) entity).damage(5);
            }
        }

        cooldownManager.setCooldown(player, "burning_step");
    }

    private void usePantsRun(Player player, Projectile projectile) {
        if (!cooldownManager.checkCooldown(player, "pants_run")) {
            String message = cooldownManager.formatCooldownMessage(player, "pants_run");
            if (message != null) player.sendMessage(Component.text(message));
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (projectile.isOnGround() || projectile.isDead()) {
                    String coords = "0 64 0";
                    try {
                        String[] parts = coords.split(" ");
                        if (parts.length == 3) {
                            double x = Double.parseDouble(parts[0]);
                            double y = Double.parseDouble(parts[1]);
                            double z = Double.parseDouble(parts[2]);

                            Location teleportLoc = new Location(player.getWorld(), x, y, z);
                            player.teleport(teleportLoc);
                            player.sendMessage(Component.text("§a좌표로 텔레포트했습니다!"));

                            cooldownManager.setCooldown(player, "pants_run");
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(Component.text("§c잘못된 좌표 형식입니다!"));
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(skillManager.getPlugin(), 0, 5);
    }

    private void useCrystalProtection(Player player) {
        if (!cooldownManager.checkCooldown(player, "crystal_protection")) {
            String message = cooldownManager.formatCooldownMessage(player, "crystal_protection");
            if (message != null) player.sendMessage(Component.text(message));
            return;
        }

        Location center = player.getLocation();

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Location blockLoc = center.clone().add(x, y, z);

                    if (x == 0 && y == 0 && z == 0) {
                        blockLoc.getBlock().setType(Material.WATER);
                    } else {
                        blockLoc.getBlock().setType(Material.ICE);
                    }
                }
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            Location blockLoc = center.clone().add(x, y, z);
                            blockLoc.getBlock().setType(Material.AIR);
                        }
                    }
                }
            }
        }.runTaskLater(skillManager.getPlugin(), 100);

        cooldownManager.setCooldown(player, "crystal_protection");
    }

    private void startFlyingCarpet(@NotNull Player player, Minecart minecart) {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!minecart.getPassengers().contains(player)) {
                    this.cancel();
                    return;
                }

                Vector direction = player.getLocation().getDirection().normalize();
                minecart.setVelocity(direction.multiply(0.5));
            }
        };

        task.runTaskTimer(skillManager.getPlugin(), 0, 1);
        flyingCarpetTasks.put(player.getUniqueId(), task);
    }

    private void stopFlyingCarpet(@NotNull Player player) {
        BukkitRunnable task = flyingCarpetTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    private void useBombDog(Player player, Location location) {
        if (!cooldownManager.checkCooldown(player, "bomb_dog")) {
            String message = cooldownManager.formatCooldownMessage(player, "bomb_dog");
            if (message != null) player.sendMessage(Component.text(message));
            return;
        }

        for (int i = 0; i < 2; i++) {
            Wolf wolf = (Wolf) location.getWorld().spawnEntity(location, EntityType.WOLF);
            wolf.customName(net.kyori.adventure.text.Component.text("§c폭탄견"));
            wolf.setCustomNameVisible(true);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (wolf.isDead()) {
                        this.cancel();
                        return;
                    }

                    Player target = null;
                    double minDistance = Double.MAX_VALUE;

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.equals(player)) continue;

                        double distance = p.getLocation().distance(wolf.getLocation());
                        if (distance < minDistance && distance < 20) {
                            minDistance = distance;
                            target = p;
                        }
                    }

                    if (target != null) {
                        if (target.getLocation().distance(wolf.getLocation()) < 2) {
                            explodeWolf(wolf);
                            this.cancel();
                        } else {
                            wolf.setTarget(target);
                        }
                    }
                }
            }.runTaskTimer(skillManager.getPlugin(), 0, 10);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!wolf.isDead()) {
                        explodeWolf(wolf);
                    }
                }
            }.runTaskLater(skillManager.getPlugin(), 600);
        }

        cooldownManager.setCooldown(player, "bomb_dog");
    }

    private void explodeWolf(@NotNull Wolf wolf) {
        Location loc = wolf.getLocation();
        wolf.getWorld().createExplosion(loc, 2.0f, false, false);
        wolf.remove();
    }
}