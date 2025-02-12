package com.magmaguy.elitemobs.npcs;

import com.magmaguy.elitemobs.ChatColorConverter;
import com.magmaguy.elitemobs.EntityTracker;
import com.magmaguy.elitemobs.MetadataHandler;
import com.magmaguy.elitemobs.config.npcs.NPCsConfigFields;
import com.magmaguy.elitemobs.npcs.chatter.NPCChatBubble;
import com.magmaguy.elitemobs.utils.WarningMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class NPCEntity {

    private static HashSet<NPCEntity> npcEntityHashSet = new HashSet<>();

    private Villager villager;

    private NPCsConfigFields npCsConfigFields;
    private String name;
    private String role;
    private ArmorStand roleDisplay;
    private Villager.Profession profession;
    private Location spawnLocation;
    private List<String> greetings;
    private List<String> dialog;
    private List<String> farewell;
    private boolean canMove;
    private boolean canTalk;
    private boolean isTalking = false;
    private double activationRadius;
    private boolean disappearsAtNight;
    private boolean isSleeping = false;
    private NPCInteractions.NPCInteractionType npcInteractionType;

    /**
     * Returns the full list of NPCEntities currently registered, alive or not.
     *
     * @return List of all NPCEntities
     */
    private static HashSet<NPCEntity> getNPCEntityList() {
        return npcEntityHashSet;
    }

    public static void addNPCEntity(NPCEntity npcEntity) {
        npcEntityHashSet.add(npcEntity);
    }

    /**
     * Deletes the NPCEntity's data and entities associated to it.
     *
     * @param npcEntity NPCEntity to be deleted
     */
    public static void removeNPCEntity(NPCEntity npcEntity) {
        npcEntity.villager.remove();
        npcEntity.roleDisplay.remove();
        npcEntityHashSet.remove(npcEntity);
    }

    /**
     * Gets a specific NPCEntity based on the config npCsConfigFields they have
     *
     * @param npCsConfigFields Fields to compare
     * @return NPCEntity associated to this npCsConfigFields
     */
    public static NPCEntity getNPCEntityFromFields(NPCsConfigFields npCsConfigFields) {
        for (NPCEntity npcEntity : npcEntityHashSet)
            if (npcEntity.npCsConfigFields.getFileName().equals(npCsConfigFields.getFileName()))
                return npcEntity;

        return null;
    }

    /**
     * Spawns NPC based off of the values in the NPCsConfig config file. Runs at startup and on reload.
     */
    public NPCEntity(NPCsConfigFields npCsConfigFields) {

        this.npCsConfigFields = npCsConfigFields;

        if (!setSpawnLocation(npCsConfigFields.getLocation())) return;
        if (!npCsConfigFields.isEnabled()) return;

        this.villager = (Villager) spawnLocation.getWorld().spawnEntity(spawnLocation, EntityType.VILLAGER);

        this.villager.setRemoveWhenFarAway(true);

        setName(npCsConfigFields.getName());
        initializeRole(npCsConfigFields.getRole());
        setProfession(npCsConfigFields.getProfession());
        setGreetings(npCsConfigFields.getGreetings());
        setDialog(npCsConfigFields.getDialog());
        setFarewell(npCsConfigFields.getFarewell());
        setCanMove(npCsConfigFields.isCanMove());
        setCanTalk(npCsConfigFields.isCanTalk());
        setActivationRadius(npCsConfigFields.getActivationRadius());
        setDisappearsAtNight(npCsConfigFields.isCanSleep());
        setNpcInteractionType(npCsConfigFields.getInteractionType());

        EntityTracker.registerNPCEntity(this);
        addNPCEntity(this);

    }

    /**
     * Respawns the villager associated with the NPCEntity. Used for when chunks that contain NPCEntities load back up
     * as it makes sure the former Villager is slain.
     */
    public void respawnNPC() {
        if (Bukkit.getEntity(villager.getUniqueId()) != null)
            Bukkit.getEntity(villager.getUniqueId()).remove();
        villager = (Villager) spawnLocation.getWorld().spawnEntity(spawnLocation, EntityType.VILLAGER);
        villager.setCustomName(name);
        villager.setCustomNameVisible(true);
        villager.setProfession(profession);
        villager.setAI(canMove);
        villager.setRemoveWhenFarAway(true);
        if (Bukkit.getEntity(roleDisplay.getUniqueId()) != null)
            Bukkit.getEntity(roleDisplay.getUniqueId()).remove();
        initializeRole(role);
        EntityTracker.registerNPCEntity(this);
        addNPCEntity(this);
    }

    /**
     * Returns the Villager associated to this NPCEntity
     *
     * @return Villager associated to this NPCEntity
     */
    public Villager getVillager() {
        return this.villager;
    }

    /**
     * Returns the name of this NCPEntity
     *
     * @return Name of this NPCEntity
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the name of the NPCEntity
     *
     * @param name Name to be set
     */
    public void setName(String name) {
        name = ChatColorConverter.convert(name);
        this.name = name;
        this.villager.setCustomName(name);
        this.villager.setCustomNameVisible(true);
    }

    /**
     * Gets the role of the NPCEntity. The role is the text that shows up below the name of the NPCEntity.
     *
     * @return Role name
     */
    public String getRole() {
        return this.role;
    }

    //Can't be used after the NPCEntity is done initialising
    private void initializeRole(String role) {
        this.role = role;
        this.roleDisplay = (ArmorStand) this.villager.getWorld().spawnEntity(villager.getLocation().add(new Vector(0, 1.72, 0)), EntityType.ARMOR_STAND);
        EntityTracker.registerArmorStands(this.roleDisplay);
        this.roleDisplay.setCustomName(role);
        this.roleDisplay.setCustomNameVisible(true);
        this.roleDisplay.setMarker(true);
        this.roleDisplay.setVisible(false);
        this.roleDisplay.setGravity(false);
    }

    /**
     * Sets the role of the NPCEntity. The role is the text that shows up below the name of the NPCEntity.
     *
     * @param role Role to be set
     */
    public void setRole(String role) {
        this.role = role;
        this.roleDisplay.setCustomName(role);
    }

    public void setTempRole(String tempRole) {
        this.roleDisplay.setCustomName(tempRole);
    }

    /**
     * Sets the profession of the NPC, changing the skin the villager uses.
     *
     * @param profession Career to be set
     */
    public void setProfession(String profession) {
        try {
            this.profession = Villager.Profession.valueOf(profession);
        } catch (Exception ex) {
            this.profession = Villager.Profession.NITWIT;
        }
        this.villager.setProfession(this.profession);
    }

    /**
     * Gets the spawn location of the NPCEntity
     *
     * @return Entity's spawn location
     */
    public Location getSpawnLocation() {
        return this.spawnLocation;
    }

    private boolean setSpawnLocation(String spawnLocation) {
        int counter = 0;
        World world = null;
        double x = 0;
        double y = 0;
        double z = 0;
        float yaw = 0;
        float pitch = 0;
        for (String substring : spawnLocation.split(",")) {
            switch (counter) {
                case 0:
                    /*
                    World is contained here
                     */
                    world = Bukkit.getWorld(substring);
                    break;
                case 1:
                    /*
                    X value is contained here
                     */
                    x = Double.valueOf(substring);
                    break;
                case 2:
                    /*
                    Y value is contained here
                     */
                    y = Double.valueOf(substring);
                    break;
                case 3:
                    /*
                    Z value is contained here
                     */
                    z = Double.valueOf(substring);
                    break;
                case 4:
                    yaw = Float.valueOf(substring);
                    break;
                case 5:
                    pitch = Float.valueOf(substring);
                    break;
            }

            counter++;
        }

        if (world == null) return false;

        this.spawnLocation = new Location(world, x, y, z, yaw, pitch);

        return true;
    }

    /**
     * Returns the list of greetings that this NPCEntity uses
     *
     * @return List of greetings used by this NPCEntity
     */
    public List<String> getGreetings() {
        return this.greetings;
    }

    /**
     * Sets the list of greetings to be used by the NPCEntity
     *
     * @param greetings List of greetings to be set
     */
    public void setGreetings(List<String> greetings) {
        this.greetings = greetings;
    }

    /**
     * Returns the list of dialogs that this NPCEntity uses. Dialog is triggered if the player remains near the NPCEntity
     * after being greeted by it.
     *
     * @return List of dialog used by this NPCEntity
     */
    public List<String> getDialog() {
        return this.dialog;
    }

    /**
     * Sets the list of dialogs to be used by this NPCEntity. Dialog is triggered if the player remains near the NPCEntity
     * after being greeted by it.
     *
     * @param dialog List of dialogs to be set
     */
    public void setDialog(List<String> dialog) {
        this.dialog = dialog;
    }

    /**
     * Returns the list of farewells that this NPCEntity uses. Farewells are triggered when a player exits an NPC's
     * GUI.
     *
     * @return List of farewells used by this NPCEntity
     */
    public List getFarewell() {
        return this.farewell;
    }

    /**
     * Sets the list of farewells to be used by this NPCEntity. Farewells are triggered when a player exits an NPC's
     * GUI.
     *
     * @param farewell List of farewells to be set
     */
    public void setFarewell(List<String> farewell) {
        this.farewell = farewell;
    }

    /**
     * Returns whether or not the NPCEntity can move.
     *
     * @return whether the NPCEntity can move
     */
    public boolean getCanMove() {
        return this.canMove;
    }

    /**
     * Sets the NPCEntity's ability to move
     *
     * @param canMove Sets if the NPCEntity can move
     */
    public void setCanMove(boolean canMove) {
        this.canMove = canMove;
        this.villager.setAI(canMove);
        if (!canMove)
            villager.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 3));
    }

    /**
     * Returns whether the NPCEntity can use NPCChatBubble
     *
     * @return Whether the NPCEntity can use NPCChatBubble
     */
    public boolean getCanTalk() {
        return this.canTalk;
    }

    /**
     * @param canTalk
     */
    public void setCanTalk(boolean canTalk) {
        this.canTalk = canTalk;
    }

    /**
     * Returns if the entity is currently talking. Useful to avoid speech overlapping.
     *
     * @return Whether the NPCEntity is currently talking
     */
    public boolean getIsTalking() {
        return this.isTalking;
    }

    /**
     * Sets whether the NPCEntity is talking
     *
     * @param isTalking Value to be set
     */
    public void setIsTalking(boolean isTalking) {
        this.isTalking = isTalking;
    }

    /**
     * Starts a cooldown for the talking state. Useful to avoid overlapping speech.
     */
    public void startTalkingCooldown() {
        this.isTalking = true;
        new BukkitRunnable() {
            @Override
            public void run() {
                isTalking = false;
            }
        }.runTaskLater(MetadataHandler.PLUGIN, 20 * 3);
    }

    /**
     * Radius to be used to search for players near the NPCEntity. The various dialog options will only trigger if a player
     * is found within this radius.
     *
     * @return Radius to look for players
     */
    public double getActivationRadius() {
        return this.activationRadius;
    }

    /**
     * Sets the radius to be used to search for players near the NPCEntity. The various dialog options will only trigger
     * if a player is found within this radius
     *
     * @param activationRadius Radius to be set
     */
    public void setActivationRadius(double activationRadius) {
        this.activationRadius = activationRadius;
    }

    /**
     * Gets whether this NPCEntity goes to "sleep" during night time. This prevents any interactions with this NPCEntity
     * during night time.
     *
     * @return Whether the entity goes to "sleep" at night time.
     */
    public boolean getDisappearsAtNight() {
        return this.disappearsAtNight;
    }

    /**
     * Sets whether the NPCEntity goes to "sleep" during night time. Going to "sleep" prevents player interactions with
     * it during night time
     *
     * @param disappearsAtNight Whether the entity goes to "sleep" at night time
     */
    public void setDisappearsAtNight(boolean disappearsAtNight) {
        if (disappearsAtNight) NPCWorkingHours.registerSleepEnabledNPC(this);
        this.disappearsAtNight = disappearsAtNight;
    }

    /**
     * Returns if the NPCEntity is currently sleeping, which prevents any player interactions with it
     *
     * @return Whether the entity is sleeping
     */
    public boolean getIsSleeping() {
        return this.isSleeping;
    }

    /**
     * Sets the NPCEntity to a sleep state
     *
     * @param isSleeping Whether the NPCEntity is considered to be sleeping
     */
    public void setIsSleeping(boolean isSleeping) {
        this.isSleeping = isSleeping;
    }

    /**
     * Returns the type of interaction which this NPCEntity has
     *
     * @return Interaction type enum
     */
    public NPCInteractions.NPCInteractionType getInteractionType() {
        return this.npcInteractionType;
    }

    /**
     * Sets the type of interaction which this NPCEntity has
     *
     * @param npcInteractionType Type of interaction enum to be set
     */
    public void setNpcInteractionType(String npcInteractionType) {
        try {
            this.npcInteractionType = NPCInteractions.NPCInteractionType.valueOf(npcInteractionType);
        } catch (Exception ex) {
            new WarningMessage("NPC " + this.npCsConfigFields.getFileName() + " does not have a valid interaction type.");
            this.npcInteractionType = NPCInteractions.NPCInteractionType.NONE;
        }
    }

    /**
     * Selects a message from the list to output through an NPCChatBubble to a player
     *
     * @param messages List of messages to pick from
     * @param player   Player to whom the message is directed
     */
    public void say(List<String> messages, Player player) {
        new NPCChatBubble(selectString(messages), this, player);
    }

    /**
     * Sends a message to a player through an NPCChatBubble
     *
     * @param message List of messages to pick from
     * @param player  Player to whom the message is directed
     */
    public void say(String message, Player player) {
        new NPCChatBubble(message, this, player);
    }

    /**
     * Sends a greeting to a player from the list of greetings the NPCEntity has
     *
     * @param player Player to send the greeting to
     */
    public void sayGreeting(Player player) {
        new NPCChatBubble(selectString(this.greetings), this, player);
    }

    /**
     * Sends a dialog message to a player from the list of dialog messages the NPCEntity has
     *
     * @param player Player to send the dialog to
     */
    public void sayDialog(Player player) {
        new NPCChatBubble(selectString(this.dialog), this, player);
    }

    /**
     * Sends a farewell message to a player from the list of farewell messages the NPCEntity has
     * e
     *
     * @param player
     */
    public void sayFarewell(Player player) {
        new NPCChatBubble(selectString(this.farewell), this, player);
    }

    /**
     * Selects a string from a list of strings
     *
     * @param strings List of string to select from
     * @return Selected String
     */
    private String selectString(List<String> strings) {
        return strings.get(ThreadLocalRandom.current().nextInt(strings.size()));
    }

}
