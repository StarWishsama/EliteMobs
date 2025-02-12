package com.magmaguy.elitemobs.events;

import com.magmaguy.elitemobs.ChatColorConverter;
import com.magmaguy.elitemobs.EliteMobs;
import com.magmaguy.elitemobs.api.EliteMobDeathEvent;
import com.magmaguy.elitemobs.config.ConfigValues;
import com.magmaguy.elitemobs.config.EventsConfig;
import com.magmaguy.elitemobs.custombosses.CustomBossEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.ArrayList;
import java.util.HashSet;

public class EliteEvent extends AbstractEliteEvent {

    private static HashSet<EliteEvent> activeEvents = new HashSet<>();

    public static void addActiveEvent(EliteEvent eliteEvent) {
        activeEvents.add(eliteEvent);
    }

    public static void removeActiveEvent(EliteEvent eliteEvent) {
        activeEvents.remove(eliteEvent);
    }

    public static HashSet<EliteEvent> getActiveEvents() {
        return activeEvents;
    }

    /**
     * This method should be defined in the subclasses!
     *
     * @param location Location at which the event has activated
     */
    @Override
    public void activateEvent(Location location) {

    }

    /**
     * This method should be defined in the subclasses!
     *
     * @param event
     */
    @Override
    public void spawnEventHandler(CreatureSpawnEvent event) {

    }

    /**
     * This method should be defined in the subclasses!
     *
     * @param event
     */
    @Override
    public void bossDeathEventHandler(EliteMobDeathEvent event) {

    }

    /**
     * This method should be defined in the subclasses!
     */
    @Override
    public void endEvent() {

    }

    /**
     * This method should be defined in the subclasses!
     */
    @Override
    public void eventWatchdog() {

    }

    public enum EventType {
        KILL_BOSS,
        SURVIVAL,
        KILL_COUNT
    }

    private ArrayList<World> worlds;
    private World activeWorld;
    private CustomBossEntity bossEntity;
    private boolean bossIsAlive = false;
    private EntityType entityType;
    private EventType eventType;
    private boolean isQueued = true;
    private String eventStartMessage;
    private String eventEndMessage;

    public EliteEvent(ArrayList<World> worlds, EventType eventType, EntityType entityType) {
        this.worlds = worlds;
        if (worlds == null || worlds.isEmpty()) return;
        setEventType(eventType);
        setEntityType(entityType);
        addActiveEvent(this);
    }

    public World getActiveWorld() {
        return this.activeWorld;
    }

    public CustomBossEntity getBossEntity() {
        return this.bossEntity;
    }

    public void setBossEntity(CustomBossEntity bossEntity) {
        this.bossEntity = bossEntity;
        this.bossIsAlive = true;
    }

    public boolean getBossIsAlive() {
        return bossIsAlive;
    }

    public EntityType getEntityType() {
        return this.entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public EventType getEventType() {
        return this.eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public void completeEvent(World world) {
        removeActiveEvent(this);
        if (this.eventEndMessage == null) return;
        sendEventEndMessage(world);
    }

    public void setEventStartMessage(String eventStartMessage) {
        this.eventStartMessage = eventStartMessage;
    }

    public void sendEventStartMessage(World world) {
        String sendString = ChatColorConverter.convert(this.eventStartMessage.replace("$activeWorld", world.getName().replace("_", " ")));
        if (ConfigValues.eventsConfig.getBoolean(EventsConfig.ANNOUNCEMENT_BROADCAST_WORLD_ONLY)) {
            for (Player player : Bukkit.getServer().getOnlinePlayers())
                if (player.getWorld().equals(world))
                    player.sendMessage(sendString);
        } else
            Bukkit.getServer().broadcastMessage(sendString);
    }

    private void sendEventEndMessage(World world) {
        if (ConfigValues.eventsConfig.getBoolean(EventsConfig.ANNOUNCEMENT_BROADCAST_WORLD_ONLY)) {
            for (Player player : Bukkit.getServer().getOnlinePlayers())
                if (player.getWorld().equals(world))
                    player.sendMessage(ChatColorConverter.convert(this.eventEndMessage));
        } else
            Bukkit.getServer().broadcastMessage(ChatColorConverter.convert(this.eventEndMessage));
    }

    public boolean isQueued() {
        return isQueued;
    }

    public void unQueue() {
        this.isQueued = false;
    }

    public static class AbstractEliteEventEvents implements Listener {
        @EventHandler(priority = EventPriority.HIGH)
        public void onSpawn(CreatureSpawnEvent event) {
            if (event.isCancelled()) return;
            if (getActiveEvents().isEmpty()) return;
            if (!EliteMobs.validWorldList.contains(event.getLocation().getWorld())) return;
            if (!(event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.NATURAL) ||
                    event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.CUSTOM)))
                return;
            for (EliteEvent eliteEvent : getActiveEvents()) {
                if (eliteEvent.worlds.contains(event.getEntity().getWorld())) {
                    eliteEvent.activeWorld = event.getEntity().getWorld();
                    eliteEvent.spawnEventHandler(event);
                }
            }
        }

        @EventHandler
        public void onBossDeath(EliteMobDeathEvent event) {
            if (getActiveEvents().isEmpty()) return;
            for (EliteEvent eliteEvent : getActiveEvents())
                if (!event.getEliteMobEntity().equals(eliteEvent.getBossEntity())) {
                    eliteEvent.bossDeathEventHandler(event);
                    return;
                }
        }
    }

}
