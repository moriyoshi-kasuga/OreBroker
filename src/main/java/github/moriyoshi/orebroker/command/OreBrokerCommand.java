package github.moriyoshi.orebroker.command;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import github.moriyoshi.orebroker.lib.GameManager;
import github.moriyoshi.orebroker.lib.MarketEngine;
import github.moriyoshi.orebroker.util.BukkitUtil;
import lombok.val;

public final class OreBrokerCommand implements TabExecutor {
    private final GameManager gameManager = GameManager.getInstance();

    public OreBrokerCommand() {
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return List.of("start", "stop", "prices").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            sender.sendMessage(BukkitUtil.mm("<red>サブコマンドを指定してください: /orebroker <start|stop|prices>"));
            return true;
        }

        if (!sender.hasPermission("orebroker.admin")) {
            sender.sendMessage(BukkitUtil.mm("<red>このコマンドを実行する権限がありません。"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                handleStart(sender);
                break;
            case "stop":
                handleStop(sender);
                break;
            case "prices":
                handlePrices(sender);
                break;
            default:
                sender.sendMessage(BukkitUtil.mm("<red>不明なサブコマンドです: " + args[0]));
                break;
        }

        return true;
    }

    private void handleStart(CommandSender sender) {
        if (gameManager.isGameRunning()) {
            sender.sendMessage(BukkitUtil.mm("<red>ゲームは既に実行中です。"));
            return;
        }
        gameManager.startGame();
        Bukkit.broadcast(BukkitUtil.mm("<green><bold>ゲームが開始されました！</bold>"));
    }

    private void handleStop(CommandSender sender) {
        if (!gameManager.isGameRunning()) {
            sender.sendMessage(BukkitUtil.mm("<red>ゲームは実行されていません。"));
            return;
        }
        gameManager.stopGame();
        Bukkit.broadcast(BukkitUtil.mm("<red><bold>ゲームが終了しました。</bold>"));
    }

    private void handlePrices(CommandSender sender) {
        val prices = MarketEngine.getAllPrices();
        sender.sendMessage(BukkitUtil.mm("<gold>--- 現在の鉱石価格 ---"));
        for (val entry : prices.entrySet()) {
            val ore = entry.getKey();
            val price = entry.getValue();
            sender.sendMessage(BukkitUtil.mm("<gray>" + ore.display + ": <white>" + price + "G"));
        }
        sender.sendMessage(BukkitUtil.mm("<gold>---------------------"));
    }
}
