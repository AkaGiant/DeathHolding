package me.akagiant.deathholding.listeners;

import me.akagiant.deathholding.Main;
import me.akagiant.deathholding.managers.CooldownManager;
import me.akagiant.deathholding.managers.DyingManager;
import me.akagiant.deathholding.managers.RevivalItemManager;
import me.akagiant.deathholding.managers.general.MessageManager;

import me.akagiant.deathholding.managers.general.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.spigotmc.event.entity.EntityDismountEvent;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class onInteract implements Listener {

    private final CooldownManager cooldownManager = new CooldownManager();
    FileConfiguration config = Main.config.getConfig();

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        if (e.getRightClicked() instanceof Player) {
            String path = "Revival.Item";

            boolean useCustomItem = config.getBoolean(path + ".isCustom");

            String material = useCustomItem ? config.getString(path + ".custom-item.type") : config.getString(path + ".default-item");
            ItemStack item = e.getPlayer().getInventory().getItemInMainHand();

            if (useCustomItem && item.getType().equals(Material.valueOf(material))) {
                Bukkit.getLogger().info("yes");
                ItemMeta meta = item.getItemMeta();

                String requiredItemName = config.getString(path + ".custom-item.displayName");
                Bukkit.getLogger().info(requiredItemName + " " + meta.getDisplayName());

                if (meta.getDisplayName().replace("§", "&").equals(requiredItemName)) {
                    execute(((Player) e.getRightClicked()).getPlayer(), e.getPlayer());
                }
            } else {
                if (item.getType().equals(Material.valueOf(material))) {
                    execute(((Player) e.getRightClicked()).getPlayer(), e.getPlayer());
                }
            }
        }
    }

    public void execute(Player target, Player reviver) {

        if (!reviver.hasPermission("DeathHolding.Revive") && Main.usePermissions) {
            PermissionManager.NoPermission(reviver, "DeathHolding.Revive");
            return;
        }

        if (DyingManager.dyingPlayers.contains(target.getUniqueId())) {
            long timeLeft = System.currentTimeMillis() - cooldownManager.getCooldwon(reviver.getUniqueId());
            if (TimeUnit.MILLISECONDS.toSeconds(timeLeft) >= CooldownManager.COOLDOWN) {
                DyingManager.revivePlayer(target, reviver);
                cooldownManager.setCooldown(reviver, System.currentTimeMillis());
                RevivalItemManager.consumeRevivalItem(reviver);
            } else {
                for (String str : Main.config.getConfig().getStringList("Revival.Cooldown.Message")) {
                    reviver.sendMessage(MessageManager.internalPlaceholders(reviver, null, str));
                }
            }
        }
    }

    @EventHandler
    public void onDismount(EntityDismountEvent e) {
        if (e.getEntity() instanceof Player && e.getDismounted().getType().equals(EntityType.ARMOR_STAND) && DyingManager.dyingPlayers.contains(Objects.requireNonNull(((Player) e.getEntity()).getPlayer()).getUniqueId())) e.setCancelled(true);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (DyingManager.dyingPlayers.contains(Objects.requireNonNull(e.getEntity().getPlayer()).getUniqueId())) {
            e.setDeathMessage("");
        }
    }

}
