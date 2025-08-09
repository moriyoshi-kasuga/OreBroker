package github.moriyoshi.orebroker.lib;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import github.moriyoshi.orebroker.constant.OreBrokerWorld;
import github.moriyoshi.orebroker.util.BukkitUtil;
import lombok.Getter;
import lombok.val;

public class GameManager {
    private static GameManager instance;

    @Getter
    private boolean isGameRunning = false;

    @Getter
    private final Set<UUID> gamePlayers = new HashSet<>();

    public static final ItemStack SELLER_ITEM;

    static {
        SELLER_ITEM = new ItemStack(Material.BLAZE_ROD);
        val meta = SELLER_ITEM.getItemMeta();
        meta.displayName(BukkitUtil.mm("<gold>鉱石買取人 召喚"));
        meta.lore(BukkitUtil.mmList("<gray>手に持って右クリックで、鉱石を売却できます。"));
        SELLER_ITEM.setItemMeta(meta);
    }

    private GameManager() {
    }

    public static GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    public void startGame() {
        this.isGameRunning = true;
        this.gamePlayers.clear();

        val pickaxe = new ItemStack(Material.IRON_PICKAXE);
        val pickaxeMeta = pickaxe.getItemMeta();
        pickaxeMeta.addEnchant(Enchantment.LURE, 1, true);
        pickaxeMeta.setUnbreakable(true);
        pickaxeMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        pickaxe.setItemMeta(pickaxeMeta);

        for (val player : Bukkit.getOnlinePlayers()) {
            this.gamePlayers.add(player.getUniqueId());
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            MarketEngine.setMoney(player, 1000L);
            player.getInventory().addItem(pickaxe);
            player.getInventory().addItem(SELLER_ITEM);
            player.teleport(OreBrokerWorld.GAME_SPAWN_LOC);
            player.sendMessage(BukkitUtil.mm("<green>ゲームを開始しました！"));
        }
    }

    public void stopGame() {
        this.isGameRunning = false;
        for (val uuid : gamePlayers) {
            val player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.getInventory().clear();
                player.teleport(OreBrokerWorld.LOBBY_LOC);
                player.sendMessage(BukkitUtil.mm("<red>ゲームを終了しました。"));
            }
        }
        this.gamePlayers.clear();
    }

    public boolean isPlayerInGame(Player player) {
        return gamePlayers.contains(player.getUniqueId());
    }

    public void addPlayer(Player player) {
        gamePlayers.add(player.getUniqueId());
    }

    public void removePlayer(Player player) {
        gamePlayers.remove(player.getUniqueId());
    }
}
