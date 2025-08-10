package github.moriyoshi.orebroker.lib;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import github.moriyoshi.orebroker.constant.OreBrokerWorld;
import github.moriyoshi.orebroker.util.BukkitUtil;
import lombok.val;

public final class OreBrokerListener implements Listener {
    private static OreBrokerListener instance;

    private final GameManager gameManager = GameManager.getInstance();

    private OreBrokerListener() {
    }

    public static OreBrokerListener getInstance() {
        if (instance == null) {
            instance = new OreBrokerListener();
        }
        return instance;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().teleport(OreBrokerWorld.LOBBY_LOC);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        gameManager.removePlayer(event.getPlayer());
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        val player = (Player) event.getEntity();
        if (gameManager.isPlayerInGame(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (gameManager.isPlayerInGame(event.getPlayer())) {
            // TODO: ゲームワールド内のランダムな位置にリスポーンさせる
            event.setRespawnLocation(OreBrokerWorld.LOBBY_LOC);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        val player = event.getPlayer();
        if (!gameManager.isPlayerInGame(player)) {
            return;
        }

        val material = event.getBlock().getType();
        val ore = switch (material) {
            case COAL_ORE, DEEPSLATE_COAL_ORE -> MarketEngine.Ore.COAL;
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> MarketEngine.Ore.COPPER;
            case IRON_ORE, DEEPSLATE_IRON_ORE -> MarketEngine.Ore.IRON;
            case GOLD_ORE, DEEPSLATE_GOLD_ORE -> MarketEngine.Ore.GOLD;
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> MarketEngine.Ore.REDSTONE;
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> MarketEngine.Ore.LAPIS;
            case SMALL_AMETHYST_BUD, MEDIUM_AMETHYST_BUD, LARGE_AMETHYST_BUD -> MarketEngine.Ore.AMETHYST;
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> MarketEngine.Ore.EMERALD;
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> MarketEngine.Ore.DIAMOND;
            default -> null;
        };
        if (ore == null) {
            return;
        }
        val price = MarketEngine.getPrice(ore);

        if (!MarketEngine.takeMoney(player, price)) {
            player.sendMessage(
                    BukkitUtil.mm("<red>所持金が足りないため、<white>" + ore.display + "</white>を掘れませんでした。（必要: " + price + "G）"));
            event.setCancelled(true);
        } else {
            event.setDropItems(false);
            player.sendActionBar(BukkitUtil.mm("<yellow>採掘コスト: " + price + "G"));
            player.getInventory().addItem(ore.toItemStack(1));
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        val player = event.getPlayer();
        if (!gameManager.isPlayerInGame(player)) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        val itemInHand = event.getItem();
        if (itemInHand == null || !itemInHand.isSimilar(GameManager.SELLER_ITEM)) {
            return;
        }

        event.setCancelled(true);

        boolean soldSomething = false;
        for (val ore : MarketEngine.Ore.values()) {
            int count = MarketEngine.countItems(player, ore.material);
            if (count > 0) {
                MarketEngine.sellOre(player, ore, count);
                soldSomething = true;
            }
        }

        if (!soldSomething) {
            player.sendMessage(BukkitUtil.mm("<yellow>売却できる鉱石を持っていません。"));
        }
    }
}
