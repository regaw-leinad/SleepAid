package com.danwager.mc.sleep;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SleepAid extends JavaPlugin implements Listener {

    private Map<UUID, Integer> playerToSleepingTask;

    @Override
    public void onEnable() {
        this.playerToSleepingTask = new HashMap<>();

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onSleep(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();

        if (!isOverworld(world)) {
            return;
        }

        UUID uuid = player.getUniqueId();

        Bukkit.broadcastMessage(player.getDisplayName() + " is sleeping...");

        int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
            if (!this.playerToSleepingTask.containsKey(uuid)) {
                return;
            }

            this.playerToSleepingTask.remove(uuid);
            setDay(world);
        }, 100);

        this.playerToSleepingTask.put(uuid, taskId);
    }

    @EventHandler
    public void onWakeUp(PlayerBedLeaveEvent event) {
        Integer taskId = this.playerToSleepingTask.remove(event.getPlayer().getUniqueId());

        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    private boolean isOverworld(World world) {
        return world.getEnvironment() == World.Environment.NORMAL;
    }

    private void setDay(World world) {
        world.setTime(23459);

        if (world.isThundering()) {
            world.setThundering(false);
            world.setThunderDuration(0);
        }

        if (world.hasStorm()) {
            world.setStorm(false);
            world.setWeatherDuration(0);
        }
    }
}
