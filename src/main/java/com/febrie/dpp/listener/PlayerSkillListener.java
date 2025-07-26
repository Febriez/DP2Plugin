package com.febrie.dpp.listener;

import com.febrie.dpp.manager.CooldownManager;
import com.febrie.dpp.manager.SkillManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
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
    
    private Location parseCoordinates(String coords, World world) throws NumberFormatException {
        String[] parts = coords.split(" ");
        if (parts.length != 3) {
            throw new NumberFormatException("Invalid coordinate format");
        }
        
        double x = Double.parseDouble(parts[0]);
        double y = Double.parseDouble(parts[1]);
        double z = Double.parseDouble(parts[2]);
        
        return new Location(world, x, y, z);
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

        if (projectile instanceof ThrownExpBottle && player.getInventory().getItemInMainHand().getType() == Material.BOOK && skillManager.hasSkill(player, "pants_run")) {
            usePantsRun(player, projectile);
        } else if (projectile instanceof Arrow && player.getInventory().getItemInMainHand().getType() == Material.BONE && skillManager.hasSkill(player, "bomb_dog")) {
            event.setCancelled(true); // 화살 발사 취소
            useBombDog(player, player.getLocation());
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
    public void onPlayerDropItem(@NotNull PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();

        if (item.getType() == Material.BOOK && skillManager.hasSkill(player, "pants_run")) {
            event.setCancelled(true);
            usePantsRunFromBook(player, item);
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

        // 불저항 효과 부여
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100, 0));

        // 설정에서 대시 거리와 데미지 가져오기
        int dashDistance = skillManager.getPlugin().getConfig().getInt("skills.burning_step.dash_distance", 5);
        int damageAmount = skillManager.getPlugin().getConfig().getInt("skills.burning_step.damage_amount", 5);

        // 대시
        Vector direction = player.getLocation().getDirection().normalize();
        Location startLocation = player.getLocation();
        Location dashLocation = player.getLocation().add(direction.multiply(dashDistance));
        player.teleport(dashLocation);

        // 불 효과 - 경로에 불 설치
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 10) {
                    this.cancel();
                    return;
                }

                // 시작점부터 끝점까지 불 파티클과 실제 불 생성
                Vector step = dashLocation.toVector().subtract(startLocation.toVector()).normalize().multiply(0.5);
                for (double i = 0; i < dashDistance; i += 0.5) {
                    Location loc = startLocation.clone().add(step.clone().multiply(i));
                    loc.getWorld().spawnParticle(Particle.FLAME, loc, 10, 0.5, 0.5, 0.5, 0.05);

                    // 지면에 불 설치
                    Location groundLoc = loc.clone();
                    while (groundLoc.getBlock().getType() == Material.AIR && groundLoc.getY() > 0) {
                        groundLoc.subtract(0, 1, 0);
                    }
                    if (groundLoc.getBlock().getType() != Material.AIR && groundLoc.clone().add(0, 1, 0).getBlock().getType() == Material.AIR) {
                        groundLoc.add(0, 1, 0).getBlock().setType(Material.FIRE);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(skillManager.getPlugin(), 0, 2);

        // 효과음
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 1);

        // 주변 엔티티에게 데미지 및 불 효과
        for (Entity entity : player.getNearbyEntities(3, 3, 3)) {
            if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                ((LivingEntity) entity).damage(damageAmount);
                entity.setFireTicks(60); // 3초간 불타오름
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

        ItemStack book = player.getInventory().getItemInMainHand();
        String bookName = "";
        if (book.hasItemMeta() && book.getItemMeta().hasDisplayName()) {
            Component displayName = book.getItemMeta().displayName();
            if (displayName != null) {
                bookName = PlainTextComponentSerializer.plainText().serialize(displayName);
            }
        }

        final String coords = bookName.isEmpty() ? "0 64 0" : bookName;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (projectile.isOnGround() || projectile.isDead()) {
                    try {
                        Location teleportLoc = parseCoordinates(coords, player.getWorld());
                        player.teleport(teleportLoc);
                        player.sendMessage(Component.text("§a좌표로 텔레포트했습니다!"));
                        cooldownManager.setCooldown(player, "pants_run");
                    } catch (NumberFormatException e) {
                        player.sendMessage(Component.text("§c책 이름을 'x y z' 형식으로 지정하세요!"));
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(skillManager.getPlugin(), 0, 5);
    }

    private void usePantsRunFromBook(Player player, ItemStack book) {
        if (!cooldownManager.checkCooldown(player, "pants_run")) {
            String message = cooldownManager.formatCooldownMessage(player, "pants_run");
            if (message != null) player.sendMessage(Component.text(message));
            return;
        }

        String bookName = "";
        if (book.hasItemMeta() && book.getItemMeta().hasDisplayName()) {
            Component displayName = book.getItemMeta().displayName();
            if (displayName != null) {
                bookName = PlainTextComponentSerializer.plainText().serialize(displayName);
            }
        }

        if (bookName.isEmpty()) {
            player.sendMessage(Component.text("§c책에 좌표가 지정되지 않았습니다!"));
            return;
        }

        try {
            Location teleportLoc = parseCoordinates(bookName, player.getWorld());
            player.teleport(teleportLoc);
            player.sendMessage(Component.text("§a좌표로 텔레포트했습니다!"));

            book.setAmount(book.getAmount() - 1);
            cooldownManager.setCooldown(player, "pants_run");
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("§c책 이름을 'x y z' 형식으로 지정하세요!"));
        }
    }

    private void useCrystalProtection(Player player) {
        if (!cooldownManager.checkCooldown(player, "crystal_protection")) {
            String message = cooldownManager.formatCooldownMessage(player, "crystal_protection");
            if (message != null) player.sendMessage(Component.text(message));
            return;
        }

        Location center = player.getLocation();
        int radius = 3; // 더 큰 반경

        // 구 형태로 얼음 생성
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x * x + y * y + z * z);
                    if (distance <= radius && distance >= radius - 1) { // 구의 외벽만 생성
                        Location blockLoc = center.clone().add(x, y, z);
                        if (blockLoc.getBlock().getType() == Material.AIR) {
                            blockLoc.getBlock().setType(Material.ICE);
                        }
                    }
                }
            }
        }

        // 중앙에 물 생성
        center.getBlock().setType(Material.WATER);

        // 파티클 효과
        player.getWorld().spawnParticle(Particle.SNOWFLAKE, center, 100, radius, radius, radius, 0.1);
        player.getWorld().playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1, 1);

        new BukkitRunnable() {
            @Override
            public void run() {
                // 모든 얼음 블록 제거
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            Location blockLoc = center.clone().add(x, y, z);
                            if (blockLoc.getBlock().getType() == Material.ICE || blockLoc.getBlock().getType() == Material.WATER) {
                                blockLoc.getBlock().setType(Material.AIR);
                            }
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