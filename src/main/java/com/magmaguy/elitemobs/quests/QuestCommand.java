package com.magmaguy.elitemobs.quests;

import org.bukkit.entity.Player;

public class QuestCommand {

    public static void doMainQuestCommand(Player player) {
        QuestsMenu questsMenu = new QuestsMenu();
        questsMenu.initializeMainQuestMenu(player);
    }

    public static void doQuestTrackCommand(Player player) {
        if (!PlayerQuest.hasPlayerQuest(player)) {
            player.sendMessage("[EliteMobs] 你还没接受任务!");
            return;
        }

        PlayerQuest.getPlayerQuest(player).getQuestObjective().sendQuestProgressionMessage(player);

    }

}
