package com.magmaguy.elitemobs.items;

import com.magmaguy.elitemobs.config.ConfigValues;
import com.magmaguy.elitemobs.config.MobCombatSettingsConfig;
import com.magmaguy.elitemobs.mobconstructor.EliteMobEntity;

public class MobTierCalculator {

    public static final double PER_TIER_LEVEL_INCREASE = ConfigValues.mobCombatSettingsConfig.getDouble(MobCombatSettingsConfig.PER_TIER_LEVEL_INCREASE);

    public static double findMobTier(EliteMobEntity eliteMobEntity) {
        return eliteMobEntity.getLevel() / PER_TIER_LEVEL_INCREASE;
    }

    public static double findMobTier(int mobLevel) {
        return mobLevel / PER_TIER_LEVEL_INCREASE;
    }

    public static int findMobLevel(int mobTier) {
        return (int) (mobTier * PER_TIER_LEVEL_INCREASE);
    }

}
