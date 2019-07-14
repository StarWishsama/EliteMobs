package com.magmaguy.elitemobs;

import com.magmaguy.elitemobs.api.EliteMobSpawnEvent;
import com.magmaguy.elitemobs.config.ConfigValues;
import com.magmaguy.elitemobs.config.DefaultConfig;
import com.magmaguy.elitemobs.mobconstructor.EliteMobEntity;
import com.magmaguy.elitemobs.mobconstructor.mobdata.aggressivemobs.EliteMobProperties;
import com.magmaguy.elitemobs.mobconstructor.mobdata.passivemobs.SuperMobProperties;
import com.magmaguy.elitemobs.npcs.NPCEntity;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Iterator;

public class EntityTracker implements Listener {

    /*
    These HashSets track basically everything for live plugin entities
     */
    private static HashSet<LivingEntity> superMobs = new HashSet<>();
    private static HashSet<EliteMobEntity> eliteMobs = new HashSet<>();
    private static HashSet<LivingEntity> eliteMobsLivingEntities = new HashSet<>();
    private static HashSet<NPCEntity> npcEntities = new HashSet<>();

    private static HashSet<LivingEntity> naturalEntities = new HashSet<>();
    private static HashSet<ArmorStand> armorStands = new HashSet<>();
    private static HashSet<Item> itemVisualEffects = new HashSet<>();

    /*
    This HashSet shouldn't really be scanned during runtime for aside from the occasional updates, it mostly exists to
    cull entities once the server shuts down
     */
    private static HashSet<Entity> cullablePluginEntities = new HashSet<>();

    /*
    Temporary blocks
     */
    private static HashSet<Block> temporaryBlocks = new HashSet<>();

    /**
     * Gets all living elite mobs
     *
     * @return HashSet of all living elite mobs
     */
    public static HashSet<EliteMobEntity> getEliteMobs() {
        return eliteMobs;
    }

    /**
     * Registers an Entity as an elite mob. Can be cancelled by third party plugins as it calls the EliteMobSpawnEvent
     * which is cancellable. Cancelling that event will cancel the spawn of the Elite Mob.
     *
     * @param eliteMobEntity registers entity as elite mob
     */
    public static boolean registerEliteMob(EliteMobEntity eliteMobEntity) {
        EliteMobSpawnEvent eliteMobSpawnEvent = new EliteMobSpawnEvent(eliteMobEntity.getLivingEntity(), eliteMobEntity, eliteMobEntity.getSpawnReason());
        if (eliteMobSpawnEvent.isCancelled()) return false;
        eliteMobs.add(eliteMobEntity);
        eliteMobsLivingEntities.add(eliteMobEntity.getLivingEntity());
        registerCullableEntity(eliteMobEntity.getLivingEntity());
        eliteMobEntity.getLivingEntity().setMetadata(MetadataHandler.ELITE_MOB_METADATA, new FixedMetadataValue(MetadataHandler.PLUGIN, eliteMobEntity.getLevel()));
        return true;
    }

    /**
     * Fully unregisters an elite mob. Should only run after the Entity has been removed.
     *
     * @param eliteMobEntity unregisters this entity from the plugin
     */
    public static void unregisterEliteMob(EliteMobEntity eliteMobEntity) {
        eliteMobs.remove(eliteMobEntity);
        naturalEntities.remove(eliteMobEntity.getLivingEntity());
        cullablePluginEntities.remove(eliteMobEntity.getLivingEntity());
        eliteMobEntity.getLivingEntity().removeMetadata(MetadataHandler.ELITE_MOB_METADATA, MetadataHandler.PLUGIN);
    }

    /**
     * Returns whether or not an entity is an elite mob
     *
     * @param entity entity which will be checked
     * @return if the entity is an elite mob
     */
    public static boolean isEliteMob(Entity entity) {
        if (!EliteMobProperties.isValidEliteMobType(entity)) return false;
        return eliteMobsLivingEntities.contains(entity);
    }

    /**
     * Gets the EliteMob object from an entity. If the entity isn't an elite mob, it returns null.
     * It's faster to get this and compare it to null than to use #isEliteMob first and then get this method.
     *
     * @param entity entity to check
     * @return returns the EliteMob object or null if the entity isn't one
     */
    public static EliteMobEntity getEliteMobEntity(Entity entity) {
        if (!EliteMobProperties.isValidEliteMobType(entity)) return null;
        for (EliteMobEntity eliteMobEntity : eliteMobs)
            if (eliteMobEntity.getLivingEntity().equals(entity))
                return eliteMobEntity;
        return null;
    }

    /**
     * Fully unregisters an elite mob from an entity. Should only be unregistered after being removed.
     *
     * @param entity Entity to be unregistered
     */
    private static void unregisterEliteMob(Entity entity) {
        if (!isEliteMob(entity)) return;
        EliteMobEntity eliteMobEntity = getEliteMobEntity(entity);
        if (eliteMobEntity == null) return;
        eliteMobEntity.getLivingEntity().remove();
        eliteMobs.remove(eliteMobEntity);
        eliteMobsLivingEntities.remove(eliteMobEntity.getLivingEntity());
    }

    /**
     * Gets a full list of super mobs
     *
     * @return full list of super mobs
     */
    public static HashSet<LivingEntity> getSuperMobs() {
        return superMobs;
    }

    /**
     * Registers an entity as a super mob
     *
     * @param livingEntity entity to be registered
     */
    public static void registerSuperMob(LivingEntity livingEntity) {
        if (!SuperMobProperties.isValidSuperMobType(livingEntity)) return;
        superMobs.add(livingEntity);
        livingEntity.setMetadata(MetadataHandler.SUPER_MOB_METADATA, new FixedMetadataValue(MetadataHandler.PLUGIN, true));
    }

    /**
     * Unregisters an Entity from the super mob list
     *
     * @param entity entity to be unregistered
     */
    public static void unregisterSuperMob(Entity entity) {
        if (!SuperMobProperties.isValidSuperMobType(entity)) return;
        if (!isSuperMob(entity)) return;
        superMobs.remove(entity);
        entity.removeMetadata(MetadataHandler.SUPER_MOB_METADATA, MetadataHandler.PLUGIN);
    }

    /**
     * Gets if the entity is a super mob
     *
     * @param entity entity to check
     * @return whether the entity is a super mob
     */
    public static boolean isSuperMob(Entity entity) {
        if (!SuperMobProperties.isValidSuperMobType(entity)) return false;
        return superMobs.contains(entity);
    }

    /**
     * Gets all living natural entities
     *
     * @return full list of natural entities
     */
    public static HashSet<LivingEntity> getNaturalEntities() {
        return naturalEntities;
    }

    /**
     * Registers a LivingEntity as a natural entity
     *
     * @param livingEntity livingEntity to be registered
     */
    public static void registerNaturalEntity(LivingEntity livingEntity) {
        if (!EliteMobProperties.isValidEliteMobType(livingEntity)) return;
        naturalEntities.add(livingEntity);
    }

    /**
     * Unregisters a LivingEntity from the natural entities list
     *
     * @param livingEntity LivingEntity to unregister
     */
    public static void unregisterNaturalEntity(LivingEntity livingEntity) {
        naturalEntities.remove(livingEntity);
    }

    /**
     * Unregisters an Entity from the Natural Entity list
     *
     * @param entity Entity to unregister
     */
    public static void unregisterNaturalEntity(Entity entity) {
        if (EliteMobProperties.isValidEliteMobType(entity)) return;
        if (!isNaturalEntity(entity)) return;
        naturalEntities.remove(entity);
    }

    /**
     * Checks if an Entity is a natural entity
     *
     * @param entity entity to check
     * @return whether the Entity is naturally spawned
     */
    public static boolean isNaturalEntity(Entity entity) {
        if (!EliteMobProperties.isValidEliteMobType(entity)) return false;
        return naturalEntities.contains(entity);
    }

    /**
     * Registers an Armorstand for specific display purposes
     *
     * @param armorStand Armorstand to register
     */
    public static void registerArmorStands(ArmorStand armorStand) {
        armorStands.add(armorStand);
        registerCullableEntity(armorStand);
    }

    /**
     * Unregisters an Armorstand from the armorstand list
     *
     * @param armorStand Armorstand to unregister
     */
    public static void unregisterArmorStand(Entity armorStand) {
        if (!armorStand.getType().equals(EntityType.ARMOR_STAND)) return;
        if (!isArmorStand(armorStand)) return;
        armorStand.remove();
        armorStands.remove(armorStand);
    }

    /**
     * Checks if an entity is a registered Armorstand. These are used for hologram displays.
     *
     * @param entity Entity to be checked
     * @return whether the Entity is a registered Armorstand
     */
    public static boolean isArmorStand(Entity entity) {
        if (!entity.getType().equals(EntityType.ARMOR_STAND)) return false;
        return (armorStands.contains(entity));
    }

    /**
     * Gets all items currently acting as visual effects
     *
     * @return List of all items acting as visual effects
     */
    public static HashSet<Item> getItemVisualEffects() {
        return itemVisualEffects;
    }

    /**
     * Registers an item visual effect
     *
     * @param item Item to be registered
     */
    public static void registerItemVisualEffects(Item item) {
        itemVisualEffects.add(item);
        registerCullableEntity(item);
    }

    /**
     * Unregisters an item visual effect. Should only happen after the entity has been removed.
     *
     * @param entity Entity to be unregistered
     */
    public static void unregisterItemVisualEffects(Entity entity) {
        if (!entity.getType().equals(EntityType.DROPPED_ITEM)) return;
        if (!isItemVisualEffect(entity)) return;
        itemVisualEffects.remove(entity);
        entity.remove();
    }

    /**
     * Checks if an Entity is a registered item visual effect
     * an
     *
     * @param entity Entity to be checked
     * @return whether the entity is a visual effect
     */
    public static boolean isItemVisualEffect(Entity entity) {
        return (itemVisualEffects.contains(entity));
    }

    /**
     * Registers cullable entities. Cullable entities are killed when chunks get unloaded. Super mobs aren't registered
     * for safety purposes.
     *
     * @param entity Entity to be registered
     */
    public static void registerCullableEntity(Entity entity) {
        cullablePluginEntities.add(entity);
    }

    /**
     * Unregisters a cullable entity. This should only ever happen when the entity has already been removed.
     *
     * @param entity Entity to be unregistered.
     */
    public static void unregisterCullableEntity(Entity entity) {
        if (!cullablePluginEntities.contains(entity)) return;
        cullablePluginEntities.remove(entity);
        entity.remove();
    }

    /**
     * Gets all NPCs currently alive
     *
     * @return HashSet of all living NPC entities
     */
    public static HashSet<NPCEntity> getNPCEntities() {
        return npcEntities;
    }

    /**
     * Sets a NPC as a being a registered NPC
     *
     * @param npc NPC to be registered
     */
    public static void registerNPCEntity(NPCEntity npc) {
        npcEntities.add(npc);
        npc.getVillager().setMetadata(MetadataHandler.NPC_METADATA, new FixedMetadataValue(MetadataHandler.PLUGIN, true));
        registerCullableEntity(npc.getVillager());
    }

    private static void unregisterNPCEntity(Entity entity) {
        if (!entity.getType().equals(EntityType.VILLAGER)) return;
        for (NPCEntity npcEntity : npcEntities)
            if (npcEntity.getVillager().equals(entity)) {
                entity.remove();
                return;
            }
    }

    /**
     * Checks whether or not an entity is a registered NPC
     *
     * @param entity Entity to be checked
     * @return Whether the Entity is a registered NPC
     */
    public static boolean isNPCEntity(Entity entity) {
        if (!entity.getType().equals(EntityType.VILLAGER)) return false;
        for (NPCEntity npcEntity : npcEntities)
            if (npcEntity.getVillager().equals(entity))
                return true;
        return false;
    }

    public static NPCEntity getNPCEntity(Entity entity) {
        if (!entity.getType().equals(EntityType.VILLAGER)) return null;
        for (NPCEntity npcEntity : npcEntities)
            if (npcEntity.getVillager().equals(entity))
                return npcEntity;
        return null;
    }

    public static void addTemporaryBlock(Block block, int ticks, Material replacementMaterial) {
        temporaryBlocks.add(block);
        block.setType(replacementMaterial);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (block.getType().equals(replacementMaterial))
                    block.setType(Material.AIR);
                temporaryBlocks.remove(block);
            }
        }.runTaskLater(MetadataHandler.PLUGIN, ticks);
    }

    public static boolean getIsTemporaryBlock(Block block) {
        return temporaryBlocks.contains(block);
    }

    public static void removeTemporaryBlock(Block block) {
        temporaryBlocks.remove(block);
    }

    @EventHandler
    public void onMine(BlockBreakEvent event) {
        if (!getIsTemporaryBlock(event.getBlock())) return;
        event.setDropItems(false);
        removeTemporaryBlock(event.getBlock());
    }


    /*
    Custom spawn reasons can be considered as natural spawns under specific config options
     */
    @EventHandler(priority = EventPriority.LOW)
    public void registerNaturalEntity(CreatureSpawnEvent event) {
        if (event.isCancelled()) return;
        if (event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.NATURAL) ||
                event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.CUSTOM) &&
                        !ConfigValues.defaultConfig.getBoolean(DefaultConfig.STRICT_SPAWNING_RULES))
            registerNaturalEntity(event.getEntity());
    }


    /**
     * Purges all removable entities for a shutdown (or reload)
     */
    public static void shutdownPurger() {

        for (Entity entity : cullablePluginEntities)
            entity.remove();

        for (Block block : temporaryBlocks)
            block.setType(Material.AIR);

        eliteMobs.clear();
        eliteMobsLivingEntities.clear();
        superMobs.clear();
        itemVisualEffects.clear();
        armorStands.clear();
        naturalEntities.clear();
        cullablePluginEntities.clear();
        npcEntities.clear();
        temporaryBlocks.clear();

    }

    private static void wipeEntity(Entity entity) {
        unregisterEliteMob(entity);
        unregisterCullableEntity(entity);
        unregisterNPCEntity(entity);
        unregisterArmorStand(entity);
        unregisterItemVisualEffects(entity);
        unregisterSuperMob(entity);
        unregisterNaturalEntity(entity);
    }

    /**
     * Wipes a chunk clean of all relevant plugin entities and data.
     *
     * @param event ChunkUnloadEvent to be cleared
     */
    public static void chunkWiper(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities())
            wipeEntity(entity);
    }

    public static void chunkWiper(WorldUnloadEvent event) {
        for (Chunk chunk : event.getWorld().getLoadedChunks())
            for (Entity entity : chunk.getEntities())
                wipeEntity(entity);
    }

    /**
     * Wiped an entity of all relevant plugin data on death.
     *
     * @param event Entity to be wiped.
     */
    public static void deathWipe(EntityDeathEvent event) {
        wipeEntity(event.getEntity());
    }

    public static void entityValidator() {
        new BukkitRunnable() {
            @Override
            public void run() {

                int entitiesCleared = 0;

                for (Iterator<LivingEntity> iterator = superMobs.iterator(); iterator.hasNext(); ) {
                    LivingEntity livingEntity = iterator.next();
                    if (livingEntity == null || livingEntity.isDead()) {
                        iterator.remove();
                        entitiesCleared++;
                    }
                }

                for (Iterator<EliteMobEntity> iterator = eliteMobs.iterator(); iterator.hasNext(); ) {
                    EliteMobEntity eliteMobEntity = iterator.next();
                    if (eliteMobEntity == null || eliteMobEntity.getLivingEntity() == null || eliteMobEntity.getLivingEntity().isDead()) {
                        iterator.remove();
                        entitiesCleared++;
                    }
                }
                for (Iterator<LivingEntity> iterator = eliteMobsLivingEntities.iterator(); iterator.hasNext(); ) {
                    LivingEntity livingEntity = iterator.next();
                    if (livingEntity == null || livingEntity.isDead()) {
                        iterator.remove();
                        entitiesCleared++;
                    }
                }

                for (Iterator<LivingEntity> iterator = naturalEntities.iterator(); iterator.hasNext(); ) {
                    LivingEntity livingEntity = iterator.next();
                    if (livingEntity == null || livingEntity.isDead()) {
                        iterator.remove();
                        entitiesCleared++;
                    }
                }
                for (Iterator<ArmorStand> iterator = armorStands.iterator(); iterator.hasNext(); ) {
                    ArmorStand armorStand = iterator.next();
                    if (armorStand == null || armorStand.isDead()) {
                        iterator.remove();
                        entitiesCleared++;
                    }
                }
                for (Iterator<Item> iterator = itemVisualEffects.iterator(); iterator.hasNext(); ) {
                    Item item = iterator.next();
                    if (item == null || item.isDead()) {
                        iterator.remove();
                        entitiesCleared++;
                    }
                }

//                Bukkit.getLogger().warning("[EliteMobs] Entities cleared: " + entitiesCleared);

            }
        }.runTaskTimer(MetadataHandler.PLUGIN, 20 * 60, 20 * 60);
    }

}
