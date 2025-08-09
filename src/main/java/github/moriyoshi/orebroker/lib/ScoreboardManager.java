package github.moriyoshi.orebroker.lib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import fr.mrmicky.fastboard.FastBoard;
import github.moriyoshi.orebroker.OreBroker;
import github.moriyoshi.orebroker.lib.MarketEngine.Ore;
import lombok.val;

public class ScoreboardManager implements Listener {
    private static ScoreboardManager instance;
    private BukkitRunnable runnable;
    private final GameManager gameManager = GameManager.getInstance();
    private final Map<UUID, FastBoard> boards = new HashMap<>();

    private ScoreboardManager() {
    }

    public static ScoreboardManager getInstance() {
        if (instance == null) {
            instance = new ScoreboardManager();
        }
        return instance;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, OreBroker.getInstance());

        this.runnable = new BukkitRunnable() {
            @Override
            public void run() {
                for (val board : boards.values()) {
                    updateBoard(board);
                }
            }
        };
        this.runnable.runTaskTimer(OreBroker.getInstance(), 0L, 20L); // 1秒ごとに更新
    }

    public void stop() {
        if (this.runnable != null) {
            this.runnable.cancel();
        }
        for (val board : boards.values()) {
            board.delete();
        }
        boards.clear();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FastBoard board = new FastBoard(player);
        board.updateTitle(ChatColor.GOLD + "OreBroker");
        boards.put(player.getUniqueId(), board);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        FastBoard board = boards.remove(player.getUniqueId());
        if (board != null) {
            board.delete();
        }
    }

    private void updateBoard(FastBoard board) {
        Player player = board.getPlayer();
        List<String> lines = new ArrayList<>();

        if (!gameManager.isGameRunning() || !gameManager.isPlayerInGame(player)) {
            lines.add("");
            lines.add(ChatColor.WHITE + "ゲーム開始待機中...");
            lines.add("");
            lines.add(ChatColor.GRAY + "moriyoshi.github");
        } else {
            long balance = MarketEngine.getMoney(player);

            lines.add(ChatColor.GRAY + "所持金:");
            lines.add("  " + ChatColor.GREEN + String.format("%,d", balance) + " G");
            lines.add(" ");
            lines.add(ChatColor.GRAY + "価格情報:");
            for (val ore : Ore.values()) {
                lines.add(formatOrePrice(ore, MarketEngine.getPrice(ore)));
            }
            lines.add("  ");
        }

        board.updateLines(lines);
    }

    private String formatOrePrice(MarketEngine.Ore ore, int price) {
        return "  " + ChatColor.WHITE + ore.display + ": " + ChatColor.YELLOW + price + " G";
    }
}
