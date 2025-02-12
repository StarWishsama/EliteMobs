package com.magmaguy.elitemobs.powers;

import com.magmaguy.elitemobs.MetadataHandler;
import com.magmaguy.elitemobs.api.EliteMobDamagedByPlayerEvent;
import com.magmaguy.elitemobs.config.powers.PowersConfigFields;
import com.magmaguy.elitemobs.mobconstructor.EliteMobEntity;
import com.magmaguy.elitemobs.powers.bosspowers.*;
import com.magmaguy.elitemobs.powers.defensivepowers.*;
import com.magmaguy.elitemobs.powers.majorpowers.skeleton.SkeletonPillar;
import com.magmaguy.elitemobs.powers.majorpowers.skeleton.SkeletonTrackingArrow;
import com.magmaguy.elitemobs.powers.majorpowers.zombie.ZombieBloat;
import com.magmaguy.elitemobs.powers.majorpowers.zombie.ZombieFriends;
import com.magmaguy.elitemobs.powers.majorpowers.zombie.ZombieNecronomicon;
import com.magmaguy.elitemobs.powers.majorpowers.zombie.ZombieParents;
import com.magmaguy.elitemobs.powers.miscellaneouspowers.*;
import com.magmaguy.elitemobs.powers.offensivepowers.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.HashSet;

public class ElitePower {

    private static HashSet<ElitePower> elitePowers = new HashSet(Arrays.asList(
            //Boss powers
            new FlamePyre(),
            new Flamethrower(),
            new GoldExplosion(),
            new GoldShotgun(),
            new HyperLoot(),
            new SpiritWalk(),
            new SummonRaug(),
            new SummonTheReturned(),


            //major powers
            new SkeletonPillar(),
            new SkeletonTrackingArrow(),
            new ZombieBloat(),
            new ZombieFriends(),
            new ZombieNecronomicon(),
            new ZombieParents(),

            //Defensive powers
            new Invisibility(),
            new InvulnerabilityArrow(),
            new InvulnerabilityFallDamage(),
            new InvulnerabilityFire(),
            new InvulnerabilityKnockback(),

            //Miscellaneous powers
            new BonusLoot(),
            new Corpse(),
            new Implosion(),
            new MoonWalk(),
            new MovementSpeed(),
            new Taunt(),

            //Offensive powers
            new AttackArrow(),
            new AttackBlinding(),
            new AttackConfusing(),
            new AttackFire(),
            new AttackFireball(),
            new AttackFreeze(),
            new AttackGravity(),
            new AttackLightning(),
            new AttackPoison(),
            new AttackPush(),
            new AttackVacuum(),
            new AttackWeakness(),
            new AttackWeb(),
            new AttackWither()
    ));

    private static HashSet<ElitePower> getElitePowers() {
        return elitePowers;
    }

    public static ElitePower getElitePower(String elitePowerName) {
        for (ElitePower elitePower : getElitePowers())
            if (elitePower.getFileName().equalsIgnoreCase(elitePowerName)) {
                return elitePower;
            }
        return null;
    }

    private boolean cooldown = false;
    private String fileName;
    private String trail;
    private String name;
    private boolean isFiring = false;

    /**
     * This is overwritten by certain classes to apply powers to a living entity upon activation
     *
     * @param livingEntity
     */
    public void applyPowers(LivingEntity livingEntity) {
    }

    public ElitePower(PowersConfigFields powersConfigFields) {
        this.fileName = powersConfigFields.getFileName();
        this.name = powersConfigFields.getName();
        this.trail = powersConfigFields.getEffect();
    }

    public String getFileName() {
        return fileName;
    }

    public String getTrail() {
        return this.trail;
    }

    public String getName() {
        return name;
    }

    public boolean isCooldown() {
        return this.cooldown;
    }

    private void setCooldown(boolean cooldown) {
        this.cooldown = cooldown;
    }

    protected void doCooldown(int ticks, EliteMobEntity eliteMobEntity) {
        setCooldown(true);
        eliteMobEntity.doCooldown();
        new BukkitRunnable() {
            @Override
            public void run() {
                setCooldown(false);
            }
        }.runTaskLater(MetadataHandler.PLUGIN, ticks);
    }

    protected void doCooldown(int ticks) {
        setCooldown(true);
        new BukkitRunnable() {
            @Override
            public void run() {
                setCooldown(false);
            }
        }.runTaskLater(MetadataHandler.PLUGIN, ticks);
    }

    protected boolean eventIsValid(EliteMobDamagedByPlayerEvent event, ElitePower elitePower) {
        if (event.isCancelled()) return false;
        if (elitePower.isCooldown()) return false;
        return !event.getEliteMobEntity().isCooldown();
    }

    public boolean getIsFiring() {
        return this.isFiring;
    }

    public void setIsFiring(boolean isFiring) {
        this.isFiring = isFiring;
    }

}
