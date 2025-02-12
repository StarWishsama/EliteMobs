package com.magmaguy.elitemobs.powers.majorpowers.zombie;

import com.magmaguy.elitemobs.ChatColorConverter;
import com.magmaguy.elitemobs.MetadataHandler;
import com.magmaguy.elitemobs.api.EliteMobDamagedByPlayerEvent;
import com.magmaguy.elitemobs.config.ConfigValues;
import com.magmaguy.elitemobs.config.powers.PowersConfig;
import com.magmaguy.elitemobs.custombosses.CustomBossEntity;
import com.magmaguy.elitemobs.mobconstructor.EliteMobEntity;
import com.magmaguy.elitemobs.powers.MajorPower;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by MagmaGuy on 18/05/2017.
 */
public class ZombieFriends extends MajorPower implements Listener {

    public ZombieFriends() {
        super(PowersConfig.getPower("zombie_friends.yml"));
    }

    @EventHandler
    public void onHit(EliteMobDamagedByPlayerEvent event) {
        ZombieFriends zombieFriendConfig = (ZombieFriends) event.getEliteMobEntity().getPower(this);
        if (zombieFriendConfig == null) return;
        if (zombieFriendConfig.getIsFiring()) return;

        if (ThreadLocalRandom.current().nextDouble() > 0.01) return;
        zombieFriendConfig.setIsFiring(true);

        CustomBossEntity reinforcement1 = CustomBossEntity.constructCustomBoss("zombie_friends_friend.yml", event.getEntity().getLocation(), event.getEliteMobEntity().getLevel());
        CustomBossEntity reinforcement2 = CustomBossEntity.constructCustomBoss("zombie_friends_friend.yml", event.getEntity().getLocation(), event.getEliteMobEntity().getLevel());

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!event.getEliteMobEntity().getLivingEntity().isValid() || !reinforcement1.getLivingEntity().isValid() && !reinforcement2.getLivingEntity().isValid()) {

                    if (reinforcement1.getLivingEntity().isValid()) {

                        nameClearer(reinforcement1);

                        reinforcement1.getLivingEntity().setCustomName(ChatColorConverter.convert(ConfigValues.translationConfig.getStringList("ZombieFriends.DeathMessage").
                                get(ThreadLocalRandom.current().nextInt(ConfigValues.translationConfig.getStringList("ZombieFriends.DeathMessage")
                                        .size()))));

                    }

                    if (reinforcement2.getLivingEntity().isValid()) {


                        nameClearer(reinforcement2);

                        reinforcement2.getLivingEntity().setCustomName(ChatColorConverter.convert(ConfigValues.translationConfig.getStringList("ZombieFriends.DeathMessage").
                                get(ThreadLocalRandom.current().nextInt(ConfigValues.translationConfig.getStringList("ZombieFriends.DeathMessage")
                                        .size()))));

                    }

                    cancel();
                    return;

                } else {

                    if (ThreadLocalRandom.current().nextDouble() < 0.5) {

                        nameClearer(event.getEliteMobEntity());

                        event.getEliteMobEntity().getLivingEntity().setCustomName(ChatColorConverter.convert(ConfigValues.translationConfig.getStringList("ZombieFriends.ZombieDialog").
                                get(ThreadLocalRandom.current().nextInt(ConfigValues.translationConfig.getStringList("ZombieFriends.ZombieDialog")
                                        .size()))));

                    }

                    if (ThreadLocalRandom.current().nextDouble() < 0.5 && reinforcement1.getLivingEntity().isValid()) {

                        nameClearer(reinforcement1);

                        reinforcement1.getLivingEntity().setCustomName(ChatColorConverter.convert(ConfigValues.translationConfig.getStringList("ZombieFriends.FriendDialog").
                                get(ThreadLocalRandom.current().nextInt(ConfigValues.translationConfig.getStringList("ZombieFriends.FriendDialog")
                                        .size()))));

                    }

                    if (ThreadLocalRandom.current().nextDouble() < 0.5 && reinforcement2.getLivingEntity().isValid()) {

                        nameClearer(reinforcement2);

                        reinforcement2.getLivingEntity().setCustomName(ChatColorConverter.convert(ConfigValues.translationConfig.getStringList("ZombieFriends.FriendDialog").
                                get(ThreadLocalRandom.current().nextInt(ConfigValues.translationConfig.getStringList("ZombieFriends.FriendDialog")
                                        .size()))));

                    }

                }
            }
        }.runTaskTimer(MetadataHandler.PLUGIN, 20, 20 * 8);

    }

    private void nameClearer(EliteMobEntity eliteMobEntity) {

        new BukkitRunnable() {
            @Override
            public void run() {
                if (eliteMobEntity.getLivingEntity().isValid())
                    eliteMobEntity.setName(eliteMobEntity.getName());
            }
        }.runTaskLater(MetadataHandler.PLUGIN, 20 * 3);

    }

}
