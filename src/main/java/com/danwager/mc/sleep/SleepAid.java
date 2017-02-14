package com.danwager.mc.sleep;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
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
    private int sleepTicks;
    private int wakeupTime;
    private boolean announceSleeping;

    @Override
    public void onEnable() {
        this.playerToSleepingTask = new HashMap<>();

        Bukkit.getPluginManager().registerEvents(this, this);

        saveDefaultConfig();
        reload();
    }

    @EventHandler
    public void onSleep(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();

        if (!isOverworld(world)) {
            return;
        }

        UUID uuid = player.getUniqueId();

        if (shouldAnnounceSleeping()) {
            Bukkit.broadcastMessage(player.getDisplayName() + " is sleeping...");
        }

        int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
            if (!this.playerToSleepingTask.containsKey(uuid)) {
                return;
            }

            this.playerToSleepingTask.remove(uuid);
            setDay(world);
        }, this.sleepTicks);

        this.playerToSleepingTask.put(uuid, taskId);
    }

    @EventHandler
    public void onWakeUp(PlayerBedLeaveEvent event) {
        Integer taskId = this.playerToSleepingTask.remove(event.getPlayer().getUniqueId());

        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    private void reload() {
        reloadConfig();

        FileConfiguration config = getConfig();

        int sleepSeconds = config.getInt("sleepSeconds", 5);
        if (sleepSeconds < 1) {
            sleepSeconds = 1;
        } else if (sleepSeconds > 5) {
            sleepSeconds = 5;
        }

        this.sleepTicks = sleepSeconds * 20;
        this.wakeupTime = config.getInt("wakeUpTime", 23459);
        this.announceSleeping = config.getBoolean("announceSleeping", true);
    }

    private boolean shouldAnnounceSleeping() {
        return this.announceSleeping || this.playerToSleepingTask.size() == 0;
    }

    private boolean isOverworld(World world) {
        return world.getEnvironment() == World.Environment.NORMAL;
    }

    private void setDay(World world) {
        world.setTime(this.wakeupTime);

        if (world.isThundering()) {
            world.setThundering(false);
            world.setThunderDuration(0);
        }

        if (world.hasStorm()) {
            world.setStorm(false);
            world.setWeatherDuration(0);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /sleepaid reload");
            return true;
        }

        if (!args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage("Usage: /sleepaid reload");
            return true;
        }

        reload();
        sender.sendMessage("SleepAid config reloaded");

        return true;
    }
}
