package com.magmaguy.elitemobs.quests;

import com.magmaguy.elitemobs.ChatColorConverter;
import com.magmaguy.elitemobs.adventurersguild.GuildRank;
import com.magmaguy.elitemobs.utils.ItemStackGenerator;
import com.magmaguy.elitemobs.utils.MenuUtils;
import com.magmaguy.elitemobs.utils.ObfuscatedStringHandler;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import java.util.Arrays;
import java.util.HashMap;

public class QuestsMenu implements Listener {

    private static final String MAIN_MENU_KEY = ObfuscatedStringHandler.obfuscateString("/////");
    private static final String MAIN_MENU_NAME = "EliteMobs 任务" + MAIN_MENU_KEY;

    /**
     * Opens the main quest menu for a player. This contains all the ranks.
     *
     * @param player Player for whom the quest menu will open
     */
    public void initializeMainQuestMenu(Player player) {

        Inventory inventory = Bukkit.createInventory(player, 18, MAIN_MENU_NAME);

        for (int index = 0; index < 11; index++) {

            if (GuildRank.isWithinActiveRank(player, index)) {
                inventory.setItem(index,
                        ItemStackGenerator.generateItemStack(Material.GREEN_STAINED_GLASS_PANE,
                                "&a接受任务 " + GuildRank.getRankName(index + 1) + " &a!",
                                Arrays.asList("&a接受任务 " + GuildRank.getRankName(index + 1), "&a并获得特殊奖励!")));

            } else if (GuildRank.isWithinRank(player, index + 1)) {
                inventory.setItem(index, ItemStackGenerator.generateItemStack(Material.YELLOW_STAINED_GLASS_PANE,
                        "&e你可以接受 " + GuildRank.getRankName(index + 1) + " &e任务!",
                        Arrays.asList("&e使用命令 /ag 并将你的公会等级", "&e设置到 " + GuildRank.getRankName(index), " &e才能接受任务!")));

            } else {
                inventory.setItem(index, ItemStackGenerator.generateItemStack(Material.RED_STAINED_GLASS_PANE,
                        "&c你现在还不能接受任务 " + GuildRank.getRankName(index + 1) + " &c!",
                        Arrays.asList("&c你必须先解锁公会等级", GuildRank.getRankName(index + 1) + " &c(使用命令 /ag 来解锁)", "&c才能接受这些任务!")));

            }

        }

        player.openInventory(inventory);

    }

    @EventHandler
    public void onMainQuestClick(InventoryClickEvent event) {
        if (!MenuUtils.isValidMenu(event, MAIN_MENU_NAME)) return;
        event.setCancelled(true);
        if (event.getInventory().getType().equals(InventoryType.PLAYER)) return;
        if (!event.getCurrentItem().getType().equals(Material.GREEN_STAINED_GLASS_PANE)) return;

        initializeTierQuestMenu((Player) event.getWhoClicked(), event.getSlot());
    }

    /**
     * Opens the tier quest menu for a player. This lists currently available quests.
     *
     * @param player   Player for whom the menu will open
     * @param menuSlot Menu slot, used to guess the tier of the quest
     */
    public void initializeTierQuestMenu(Player player, int menuSlot) {

        int questTier = menuSlot + 1;
        if (menuSlot == 13) questTier = 10;

        player.openInventory(QuestRefresher.getQuestTierInventory(questTier).getInventory(player));

    }

    @EventHandler
    public void onTierQuestClick(InventoryClickEvent event) throws CloneNotSupportedException {
        if (!event.getView().getTitle().contains(QuestTierMenu.TIER_MENU_NAME)) return;
        if (!MenuUtils.isValidMenu(event)) return;
        event.setCancelled(true);
        if (event.getInventory().getType().equals(InventoryType.PLAYER)) return;

        int tier = 0;

        for (int i = 0; i < 11; i++)
            if (event.getView().getTitle().contains(GuildRank.getRankName(i))) {
                tier = i;
                break;
            }

        QuestTierMenu questTierMenu = QuestRefresher.getQuestTierInventory(tier);
        PlayerQuest playerQuest = questTierMenu.getPlayerQuests().get(event.getSlot() / 2 - 1);

        if (PlayerQuest.hasPlayerQuest((Player) event.getWhoClicked())) {
            initializeCancelQuestDialog((Player) event.getWhoClicked(), playerQuest);
            event.getWhoClicked().closeInventory();
            return;
        }

        PlayerQuest.addPlayerInQuests((Player) event.getWhoClicked(), playerQuest.clone());
        playerQuest.getQuestObjective().sendQuestStartMessage((Player) event.getWhoClicked());
        event.getWhoClicked().closeInventory();

    }

    private static HashMap<Player, PlayerQuest> questPairs = new HashMap();

    public static boolean playerHasPendingQuest(Player player) {
        return questPairs.containsKey(player);
    }

    public static PlayerQuest getPlayerQuestPair(Player player) {
        return questPairs.get(player);
    }

    public static void removePlayerQuestPair(Player player) {
        questPairs.remove(player);
    }

    private void initializeCancelQuestDialog(Player player, PlayerQuest playerQuest) {
        player.sendMessage(ChatColorConverter.convert("&c&l&m&o---------------------------------------------"));
        player.sendMessage(ChatColorConverter.convert("&c" + "你一次只能接受一个任务! 取消你当前的任务将会重置任务进度!"));
        TextComponent interactiveMessage = new TextComponent("[单击此处取消当前任务]");
        interactiveMessage.setColor(ChatColor.GREEN);
        interactiveMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/elitemobs quest cancel " + player.getName() + " confirm"));
        interactiveMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("取消!").create()));
        player.spigot().sendMessage(interactiveMessage);
        player.sendMessage(ChatColorConverter.convert("&7&7使用命令 &a/em quest status &7查看你的任务进度"));
        player.sendMessage(ChatColorConverter.convert("&c&l&m&o---------------------------------------------"));

        questPairs.put(player, playerQuest);
    }

}
