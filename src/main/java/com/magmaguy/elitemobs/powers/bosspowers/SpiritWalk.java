package com.magmaguy.elitemobs.powers.bosspowers;

import com.magmaguy.elitemobs.MetadataHandler;
import com.magmaguy.elitemobs.api.EliteMobDamageEvent;
import com.magmaguy.elitemobs.config.powers.PowersConfig;
import com.magmaguy.elitemobs.powers.BossPower;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class SpiritWalk extends BossPower implements Listener {

    public SpiritWalk() {
        super(PowersConfig.getPower("spirit_walk.yml"));
    }

    private int hitCounter = 0;

    private void incrementHitCounter() {
        hitCounter++;
    }

    private void resetHitsCounter() {
        hitCounter = 0;
    }

    private int getHitsCounter() {
        return hitCounter;
    }

    @EventHandler
    public void onBossMobGotHit(EliteMobDamageEvent event) {
        if (!event.getEliteMobEntity().hasPower(this)) return;
        SpiritWalk spiritWalk = (SpiritWalk) event.getEliteMobEntity().getPower(this);

        if (event.getEntityDamageEvent().getCause().equals(EntityDamageEvent.DamageCause.DROWNING) ||
                event.getEntityDamageEvent().getCause().equals(EntityDamageEvent.DamageCause.SUFFOCATION))
            initializeSpiritWalk(event.getEliteMobEntity().getLivingEntity());


        spiritWalk.incrementHitCounter();

        if (spiritWalk.getHitsCounter() < 9) return;

        spiritWalk.resetHitsCounter();
        initializeSpiritWalk(event.getEliteMobEntity().getLivingEntity());

    }

    private void initializeSpiritWalk(LivingEntity bossMob) {

        new BukkitRunnable() {

            int counter = 1;

            @Override
            public void run() {

                if (counter > 3) cancel();

                Location bossLocation = bossMob.getLocation().clone();

                for (int i = 0; i < 20; i++) {

                    double randomizedX = (ThreadLocalRandom.current().nextDouble() - 0.5) * 5;
                    double randomizedY = ThreadLocalRandom.current().nextDouble() - 0.5;
                    double randomizedZ = (ThreadLocalRandom.current().nextDouble() - 0.5) * 5;

                    Vector normalizedVector = new Vector(randomizedX, randomizedY, randomizedZ).normalize().multiply(7).multiply(counter);

                    Location newSimulatedLocation = bossLocation.add(normalizedVector).clone();

                    Location newValidLocation = checkLocationValidity(newSimulatedLocation);

                    if (newValidLocation != null) {

                        spiritWalkAnimation(bossMob, bossMob.getLocation(), newValidLocation.add(new Vector(0.5, 1, 0.5)));
                        cancel();
                        break;
                    }

                }

                counter++;

            }

        }.runTaskTimer(MetadataHandler.PLUGIN, 0, 1);

    }

    private void spiritWalkAnimation(LivingEntity bossMob, Location entityLocation, Location finalLocation) {

        bossMob.setAI(false);
        bossMob.setInvulnerable(true);
        bossMob.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 10, 1));
        Vector toDestination = finalLocation.clone().subtract(entityLocation.clone()).toVector().normalize().divide(new Vector(2, 2, 2));

        new BukkitRunnable() {

            int counter = 0;

            @Override
            public void run() {

                if (bossMob.getLocation().clone().distance(finalLocation) < 2 || counter > 20 * 10) {

                    bossMob.teleport(finalLocation);
                    bossMob.setAI(true);
                    bossMob.setInvulnerable(false);
                    bossMob.removePotionEffect(PotionEffectType.GLOWING);
                    cancel();

                }

                bossMob.teleport(bossMob.getLocation().clone().add(toDestination.clone()));

                counter++;

            }

        }.runTaskTimer(MetadataHandler.PLUGIN, 0, 1);

    }

    private Location checkLocationValidity(Location simulatedLocation) {

        if (simulatedLocation.getBlock().getType().equals(Material.AIR)) {

            int counter = 1;

            while (true) {

                if (simulatedLocation.getY() < 1) return null;

                Location blockUnderCurrentBlock = simulatedLocation.clone().subtract(new Vector(0, counter, 0));

                if (blockUnderCurrentBlock.getBlock().getType() != Material.AIR) return blockUnderCurrentBlock;

                if (counter > 10) return null;

                counter++;

            }

        }

        return null;

    }

}
