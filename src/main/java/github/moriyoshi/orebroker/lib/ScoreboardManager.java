package github.moriyoshi.orebroker.lib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import fr.mrmicky.fastboard.adventure.FastBoard;
import github.moriyoshi.orebroker.OreBroker;
import github.moriyoshi.orebroker.util.BukkitUtil;
import lombok.val;
import net.kyori.adventure.text.Component;

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
        this.runnable.runTaskTimer(OreBroker.getInstance(), 0L, 20L);
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
        board.updateTitle(BukkitUtil.mm("<gold><bold>OreBroker</bold>"));
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
        List<Component> lines = new ArrayList<>();

        if (!gameManager.isGameRunning() || !gameManager.isPlayerInGame(player)) {
            lines.add(Component.empty());
            lines.add(BukkitUtil.mm("<white>ゲーム開始待機中..."));
            lines.add(Component.empty());
        } else {
            long balance = MarketEngine.getMoney(player);

            lines.add(BukkitUtil.mm("<gray>所持金:"));
            lines.add(BukkitUtil.mm("  <green>" + String.format("%,d", balance) + " G"));
            lines.add(Component.empty());
            lines.add(BukkitUtil.mm("<gray>価格情報:"));

            for (val ore : MarketEngine.Ore.values()) {
                lines.add(formatOrePrice(ore, MarketEngine.getPrice(ore)));
            }

            lines.add(Component.empty());
        }

        board.updateLines(lines);
    }

    private Component formatOrePrice(MarketEngine.Ore ore, int price) {
        String display = StringUtils.rightPad(ore.display, 5, '　');
        String priceString = StringUtils.leftPad(String.valueOf(price), 3, '0');
        return BukkitUtil.mm("<white>" + display + " <yellow>" + priceString + "G");
    }
}
