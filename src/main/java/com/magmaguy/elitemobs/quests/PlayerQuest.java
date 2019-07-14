package com.magmaguy.elitemobs.quests;

import com.magmaguy.elitemobs.ChatColorConverter;
import com.magmaguy.elitemobs.items.MobTierCalculator;
import com.magmaguy.elitemobs.mobconstructor.EliteMobEntity;
import com.magmaguy.elitemobs.mobconstructor.mobdata.aggressivemobs.EliteMobProperties;
import com.magmaguy.elitemobs.quests.dynamic.KillAmountQuest;
import com.magmaguy.elitemobs.utils.ItemStackGenerator;
import com.magmaguy.elitemobs.utils.PlayerHeads;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;

public class PlayerQuest implements Cloneable {

    @Override
    public PlayerQuest clone() throws CloneNotSupportedException {
        return (PlayerQuest) super.clone();
    }

    private static HashMap<Player, PlayerQuest> playersInQuests = new HashMap();

    public static HashMap<Player, PlayerQuest> getPlayersInQuests() {
        return playersInQuests;
    }

    public static void addPlayerInQuests(Player player, PlayerQuest playerQuest) {
        getPlayersInQuests().put(player, playerQuest);
    }

    public static boolean hasPlayerQuest(Player player) {
        return getPlayersInQuests().containsKey(player);
    }

    public static PlayerQuest getPlayerQuest(Player player) {
        if (!playersInQuests.containsKey(player)) return null;
        return playersInQuests.get(player);
    }

    public static void removePlayersInQuests(Player player) {
        player.sendMessage(ChatColorConverter.convert("&cYour ongoing quest has been cancelled!"));
        getPlayersInQuests().remove(player);
    }

    public enum QuestType {
        DYNAMIC_KILL_AMOUNT_QUEST
    }

    private QuestType questType;
    private QuestObjective questObjective;
    private QuestReward questReward;

    public PlayerQuest(QuestType questType, QuestObjective questObjective, QuestReward questReward) {
        setQuestType(questType);
        setQuestObjective(questObjective);
        setQuestReward(questReward);
    }

    public QuestType getQuestType() {
        return questType;
    }

    public ItemStack generateQuestItemStack() {
        return ItemStackGenerator.generateItemStack(
                PlayerHeads.exclamation(),
                "&a消灭 " + getQuestObjective().getObjectiveKills() + " " +
                        EliteMobProperties.getPluginData(getQuestObjective().getEntityType()).getName()
                                .replace("$level", MobTierCalculator.findMobLevel(getQuestObjective().getMinimumEliteMobTier()) + ""),
                Arrays.asList("&f你必须消灭 &a" + getQuestObjective().getObjectiveKills() + " &f等级 &a",
                        "&a" + MobTierCalculator.findMobLevel(getQuestObjective().getMinimumEliteMobTier()) + "&e+ &f个生物.",
                        ("&f进度: &a" + getQuestObjective().getCurrentKills() + "&f/&c" + getQuestObjective().getObjectiveKills()),
                        "&a报酬: &e" + getQuestReward().getRewardMessage()));
    }

    public boolean isKillAmountQuest() {
        return getQuestType().equals(QuestType.DYNAMIC_KILL_AMOUNT_QUEST);
    }

    public KillAmountQuest getKillAmountQuest() {
        if (!isKillAmountQuest()) return null;
        return (KillAmountQuest) this;
    }

    private void setQuestType(QuestType questType) {
        this.questType = questType;
    }

    public QuestObjective getQuestObjective() {
        return questObjective;
    }

    private void setQuestObjective(QuestObjective questObjective) {
        this.questObjective = questObjective;
    }

    public QuestReward getQuestReward() {
        return questReward;
    }

    public void setQuestReward(QuestReward questReward) {
        this.questReward = questReward;
    }

    public boolean processQuestProgression(EliteMobEntity eliteMobEntity, Player player) {
        return getQuestObjective().processQuestProgression(eliteMobEntity, player);
    }

    public String getQuestStatus() {
        return ChatColorConverter.convert("&l你已经消灭了 &a" + getQuestObjective().getCurrentKills() + " &f/ &c"
                + questObjective.getObjectiveKills() + " &a" + getQuestObjective().getEliteMobName());
    }

}
