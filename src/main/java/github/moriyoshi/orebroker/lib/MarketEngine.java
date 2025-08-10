package github.moriyoshi.orebroker.lib;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import github.moriyoshi.orebroker.util.BukkitUtil;
import lombok.val;

import java.security.SecureRandom;
import java.util.*;

public final class MarketEngine {

    // ===== 公開API =====

    public static void init(Plugin plugin) {
        INSTANCE = new MarketEngine(plugin);
        INSTANCE.startScheduler();
    }

    public static int getPrice(Ore ore) {
        return INSTANCE.stateMap.get(ore).price;
    }

    public static void buyOre(Player player, Ore ore, int qty) {
        INSTANCE.handleBuy(player, ore, qty);
    }

    public static void sellOre(Player player, Ore ore, int qty) {
        INSTANCE.handleSell(player, ore, qty);
    }

    public static Map<Ore, Integer> getAllPrices() {
        Map<Ore, Integer> map = new EnumMap<>(Ore.class);
        for (var e : INSTANCE.stateMap.entrySet())
            map.put(e.getKey(), e.getValue().price);
        return map;
    }

    // --- お金/インベントリAPI ---
    public static boolean takeMoney(Player p, long amount) {
        return Money.take(p, amount);
    }

    public static void giveMoney(Player p, long amount) {
        Money.give(p, amount);
    }

    public static void setMoney(Player p, long amount) {
        Money.set(p, amount);
    }

    public static long getMoney(Player p) {
        return Money.get(p);
    }

    public static int countItems(Player p, Material m) {
        return InvUtil.count(p, m);
    }

    public static void removeItems(Player p, Material m, int qty) {
        InvUtil.remove(p, m, qty);
    }

    // ===== 実装 =====

    private static MarketEngine INSTANCE;

    private final Plugin plugin;
    private final Random rng = new SecureRandom(); // 乱数
    private final Map<Ore, PriceState> stateMap = new EnumMap<>(Ore.class);

    // 売買手数料＆価格インパクト
    private static final double FEE = 0.01; // 1%
    private static final double SELL_IMPACT = 0.006; // 売り後：価格×0.6%×数量
    private static final double BUY_IMPACT = 0.001; // 買い後：価格×0.1%×数量
    private static final long TRADE_COOLDOWN_TICKS = 20L; // 1Tick=20t（=1秒）なので 20t=1秒, ここでは2秒更新に合わせ1秒CDでもOK
    private static final long SELL_CD_ON_COAL_REVOLUTION = 60L; // 革命直後 3秒（=60t）CDの例

    // 天変地異
    private boolean disasterActive = false;
    private int disasterTicksLeft = 0;
    private double disasterMultiplier = 1.0;
    private Set<Ore> disasterTargets = EnumSet.noneOf(Ore.class);

    // 天変地異パラメータ
    private static final double P_DISASTER_PER_TICK = 1.0 / 60.0; // だいたい120秒に1回（2秒更新×60回）
    private static final int DISASTER_DURATION_TICKS = 10; // 2秒更新×10=約20秒
    private static final double DISASTER_MIN = 0.6;
    private static final double DISASTER_MAX = 1.4;

    private MarketEngine(Plugin plugin) {
        this.plugin = plugin;
        // 初期化：各鉱石の初期価格を平均帯からサンプリング
        for (Ore ore : Ore.values()) {
            int p0 = sampleUniform(ore.avgMin, ore.avgMax);
            stateMap.put(ore, new PriceState(p0, ore));
        }
    }

    private void startScheduler() {
        // 2秒ごと（40t）更新
        new BukkitRunnable() {
            @Override
            public void run() {
                tickUpdate();
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    private void tickUpdate() {
        // 天変地異フェーズ更新/開始判定
        updateDisasterPhase();

        for (Ore ore : Ore.values()) {
            PriceState st = stateMap.get(ore);

            // レッドストーンのモード更新
            if (ore == Ore.REDSTONE)
                st.updateRegime(rng);

            // 平均回帰 + ノイズ（正規乱数）
            double sigma = st.getSigma();
            double eps = gaussian(0.0, sigma);
            double pBase = st.price + st.ore.kappa * (st.ore.mu - st.price) + eps;

            // ジャンプ
            double J = 0;
            if (rng.nextDouble() < st.ore.pJumpUp)
                J += uniform(st.ore.jumpUpMin, st.ore.jumpUpMax);
            if (rng.nextDouble() < st.ore.pJumpDown)
                J -= uniform(st.ore.jumpDnMin, st.ore.jumpDnMax);

            double pNew = pBase + J;

            // 石炭：革命（超高騰）
            if (ore == Ore.COAL && rng.nextDouble() < Ore.COAL.pRevolution) {
                double mult = uniform(1.5, 3.0);
                double cap = Math.min(st.ore.pMax, Math.floor(st.price * 5.0));
                pNew = Math.min(pNew * mult, cap);
                // 3秒間は石炭売買CD（通知）
                BukkitUtil.broadcast("<dark_gray>【革命】 <gray>石炭の価格が革命的高騰！売買に短時間のクールダウンが入ります。");
                st.addExtraCooldownForAll(SELL_CD_ON_COAL_REVOLUTION);
            }

            // 天変地異の倍率適用
            if (disasterActive && disasterTargets.contains(ore)) {
                pNew *= disasterMultiplier;
            }

            // 丸め＆レンジクランプ
            int rounded = clamp((int) Math.round(pNew), st.ore.pMin, st.ore.pMax);
            st.price = rounded;
        }
    }

    private void updateDisasterPhase() {
        if (disasterActive) {
            disasterTicksLeft--;
            if (disasterTicksLeft <= 0) {
                disasterActive = false;
                disasterTargets.clear();
                BukkitUtil.broadcast("<yellow>天変地異の影響が収まりました。</yellow>");
            }
            return;
        }
        // 非アクティブ時に一定確率で開始
        if (rng.nextDouble() < P_DISASTER_PER_TICK) {
            disasterActive = true;
            disasterTicksLeft = DISASTER_DURATION_TICKS;
            disasterMultiplier = uniform(DISASTER_MIN, DISASTER_MAX);

            // 全銘柄 or ランダム2銘柄
            if (rng.nextBoolean()) {
                disasterTargets = EnumSet.allOf(Ore.class);
            } else {
                disasterTargets = EnumSet.noneOf(Ore.class);
                List<Ore> list = new ArrayList<>(Arrays.asList(Ore.values()));
                Collections.shuffle(list, rng);
                disasterTargets.add(list.get(0));
                disasterTargets.add(list.get(1));
            }
            BukkitUtil.broadcast(
                    "<gold>【天変地異】<white> 市場全体に激震！倍率: " + String.format(Locale.JAPAN, "%.2f", disasterMultiplier));
        }
    }

    // === 売買 ===

    private void handleBuy(Player p, Ore ore, int qty) {
        if (qty <= 0)
            return;
        PriceState st = stateMap.get(ore);
        if (!st.canTrade(p)) {
            BukkitUtil.send(p, "<red>クールダウン中です。少し待ってから取引してください。</red>");
            return;
        }

        int price = st.price;
        long cost = (long) price * qty;
        if (!Money.take(p, cost)) { // 所持金不足
            BukkitUtil.send(p, "<red>所持金が足りません。必要: " + cost + "G</red>");
            return;
        }

        // アイテム付与（必要に応じてMaterialを調整）
        p.getInventory().addItem(ore.toItemStack(qty));

        // 軽い価格インパクト
        int impact = Math.max(0, (int) Math.round(price * BUY_IMPACT * qty));
        st.price = Math.min(st.ore.pMax, st.price + impact);

        st.setCooldown(p, TRADE_COOLDOWN_TICKS);
        BukkitUtil.send(p, "<aqua>購入: " + ore.display + " ×" + qty + " @" + price + "G  (計 " + cost + "G)</aqua>");
    }

    private void handleSell(Player p, Ore ore, int qty) {
        if (qty <= 0)
            return;
        PriceState st = stateMap.get(ore);
        if (!st.canTrade(p)) {
            BukkitUtil.send(p, "<red>クールダウン中です。少し待ってから取引してください。</red>");
            return;
        }
        // インベントリ確認
        int invCount = InvUtil.count(p, ore.material);
        if (invCount < qty) {
            BukkitUtil.send(p, "<red>売却数が手持ち数を超えています。所持: " + invCount + "</red>");
            return;
        }
        InvUtil.remove(p, ore.material, qty);

        int price = st.price;
        long gross = (long) price * qty;
        long net = Math.max(0L, Math.round(gross * (1.0 - FEE)));
        Money.give(p, net);

        // 価格インパクト（下げ）
        int drop = Math.max(1, (int) Math.round(price * SELL_IMPACT * qty));
        st.price = Math.max(st.ore.pMin, st.price - drop);

        st.setCooldown(p, TRADE_COOLDOWN_TICKS);
        BukkitUtil.send(p, "<green>売却: " + ore.display + " ×" + qty + " @" + price + "G  手取り " + net + "G（手数料"
                + (int) (FEE * 100) + "%）</green>");
    }

    // ====== 内部クラス/ユーティリティ ======

    private static class PriceState {
        int price;
        final Ore ore;

        // プレイヤー別CD（サーバTick時刻）
        final Map<UUID, Long> nextTradableTick = new HashMap<>();
        // 石炭革命などで追加CDを課すためのグローバル加算
        long globalExtraCooldownTicks = 0L;

        // レッドストーン用：停滞/活発レジーム
        boolean redstoneStagnant = false;
        int redstoneRegimeTicksLeft = 0;

        PriceState(int price, Ore ore) {
            this.price = price;
            this.ore = ore;
        }

        void updateRegime(Random rng) {
            if (ore != Ore.REDSTONE)
                return;
            if (redstoneRegimeTicksLeft > 0) {
                redstoneRegimeTicksLeft--;
                if (redstoneRegimeTicksLeft == 0)
                    redstoneStagnant = false;
                return;
            }
            // 20%で停滞モードへ（10Tick継続）
            if (rng.nextDouble() < 0.20) {
                redstoneStagnant = true;
                redstoneRegimeTicksLeft = 10;
            }
        }

        double getSigma() {
            if (ore == Ore.REDSTONE && redstoneStagnant)
                return 8.0;
            return ore.sigma;
        }

        boolean canTrade(Player p) {
            long now = p.getWorld().getFullTime();
            long base = nextTradableTick.getOrDefault(p.getUniqueId(), 0L);
            return now >= base + globalExtraCooldownTicks;
        }

        void setCooldown(Player p, long cdTicks) {
            long now = p.getWorld().getFullTime();
            nextTradableTick.put(p.getUniqueId(), now + cdTicks);
        }

        void addExtraCooldownForAll(long extra) {
            globalExtraCooldownTicks = Math.max(globalExtraCooldownTicks, extra);
        }
    }

    public enum Ore {
        DIAMOND("ダイヤ", Material.DIAMOND, 250, 900, 625, 0.25, 45, 0.00, 0, 0, 0.025, 120, 260, 0.0),
        EMERALD("エメラルド", Material.EMERALD, 150, 750, 625, 0.30, 35, 0.00, 0, 0, 0.010, 80, 160, 0.0),
        AMETHYST("アメジスト", Material.AMETHYST_SHARD, 10, 600, 305, 0.05, 65, 0.040, 80, 150, 0.040, 80, 150, 0.0),
        LAPIS("ラピス", Material.LAPIS_LAZULI, 50, 500, 225, 0.40, 30, 0.030, 70, 130, 0.030, 70, 130, 0.0),
        REDSTONE("赤石", Material.REDSTONE, 25, 500, 200, 0.30, 40, 0.050, 60, 120, 0.050, 60, 120, 0.0),
        GOLD("金", Material.GOLD_INGOT, 45, 200, 88, 0.35, 18, 0.030, 18, 35, 0.030, 18, 35, 0.0),
        IRON("鉄", Material.IRON_INGOT, 25, 100, 48, 0.40, 10, 0.00, 0, 0, 0.00, 0, 0, 0.0),
        COPPER("銅", Material.COPPER_INGOT, 10, 75, 35, 0.35, 12, 0.00, 0, 0, 0.00, 0, 0, 0.0),
        COAL("石炭", Material.COAL, 5, 100, 30, 0.30, 14, 0.00, 0, 0, 0.00, 0, 0, 0.0004); // 革命確率（Tickあたり）

        public final String display;
        public final Material material;
        public final int pMin, pMax;
        public final int avgMin, avgMax;
        public final int mu;
        public final double kappa;
        public final double sigma;
        public final double pJumpUp, pJumpDown;
        public final int jumpUpMin, jumpUpMax, jumpDnMin, jumpDnMax;
        public final double pRevolution; // 石炭専用

        private static final Map<Material, Ore> MATERIAL_MAP = new HashMap<>();

        static {
            for (Ore ore : values()) {
                MATERIAL_MAP.put(ore.material, ore);
                // Deepslate variants
                switch (ore.material) {
                    case COAL:
                        MATERIAL_MAP.put(Material.DEEPSLATE_COAL_ORE, ore);
                        break;
                    case COPPER_INGOT:
                        MATERIAL_MAP.put(Material.DEEPSLATE_COPPER_ORE, ore);
                        break;
                    case IRON_INGOT:
                        MATERIAL_MAP.put(Material.DEEPSLATE_IRON_ORE, ore);
                        break;
                    case GOLD_INGOT:
                        MATERIAL_MAP.put(Material.DEEPSLATE_GOLD_ORE, ore);
                        break;
                    case REDSTONE:
                        MATERIAL_MAP.put(Material.DEEPSLATE_REDSTONE_ORE, ore);
                        break;
                    case LAPIS_LAZULI:
                        MATERIAL_MAP.put(Material.DEEPSLATE_LAPIS_ORE, ore);
                        break;
                    case DIAMOND:
                        MATERIAL_MAP.put(Material.DEEPSLATE_DIAMOND_ORE, ore);
                        break;
                    case EMERALD:
                        MATERIAL_MAP.put(Material.DEEPSLATE_EMERALD_ORE, ore);
                        break;
                    default:
                        break;
                }
            }
        }

        Ore(String display, Material material, int pMin, int pMax, int mu, double kappa, double sigma,
                double pJumpUp, int jumpUpMin, int jumpUpMax,
                double pJumpDown, int jumpDnMin, int jumpDnMax,
                double pRevolution) {
            this.display = display;
            this.material = material;
            this.pMin = pMin;
            this.pMax = pMax;
            this.mu = mu;
            this.kappa = kappa;
            this.sigma = sigma;
            this.pJumpUp = pJumpUp;
            this.jumpUpMin = jumpUpMin;
            this.jumpUpMax = jumpUpMax;
            this.pJumpDown = pJumpDown;
            this.jumpDnMin = jumpDnMin;
            this.jumpDnMax = jumpDnMax;
            this.pRevolution = pRevolution;
            int span = Math.max(10, (pMax - pMin) / 4);
            this.avgMin = Math.max(pMin, mu - span);
            this.avgMax = Math.min(pMax, mu + span);
        }

        public ItemStack toItemStack(int amount) {
            val item = new ItemStack(this.material);
            item.setAmount(amount);
            return item;
        }

        public static Optional<Ore> fromMaterial(Material material) {
            return Optional.ofNullable(MATERIAL_MAP.get(material));
        }
    }

    // ====== ユーティリティ群 ======

    private int sampleUniform(int a, int b) {
        if (a >= b)
            return a;
        return a + rng.nextInt(b - a + 1);
    }

    private double uniform(double a, double b) {
        return a + rng.nextDouble() * (b - a);
    }

    private double gaussian(double mean, double stddev) {
        double u1 = Math.max(1e-9, rng.nextDouble());
        double u2 = rng.nextDouble();
        double z = Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
        return mean + stddev * z;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ====== お金＆インベントリのダミー ======

    private static final class Money {
        private static final Map<UUID, Long> BAL = new HashMap<>();
        private static final long START = 1000L;

        static void set(Player p, long amount) {
            BAL.put(p.getUniqueId(), amount);
            checkMilestones(p, amount);
        }

        static boolean take(Player p, long amount) {
            long cur = BAL.getOrDefault(p.getUniqueId(), START);
            if (cur < amount)
                return false;
            BAL.put(p.getUniqueId(), cur - amount);
            checkMilestones(p, cur - amount);
            return true;
        }

        static void give(Player p, long amount) {
            long cur = BAL.getOrDefault(p.getUniqueId(), START);
            long next = cur + amount;
            BAL.put(p.getUniqueId(), next);
            checkMilestones(p, next);
        }

        static long get(Player p) {
            return BAL.getOrDefault(p.getUniqueId(), START);
        }

        private static final Set<UUID> announced5000 = new HashSet<>();

        private static void checkMilestones(Player p, long bal) {
            if (bal >= 10000L) {
                BukkitUtil.broadcast("<light_purple>" + p.getName() + " が 10000G に到達！勝者です！");
                // ここでゲーム終了処理へフック
            } else if (bal >= 5000L && !announced5000.contains(p.getUniqueId())) {
                announced5000.add(p.getUniqueId());
                BukkitUtil.broadcast("<aqua>" + p.getName() + " が 5000G に初到達！");
            }
        }
    }

    private static final class InvUtil {
        static int count(Player p, Material m) {
            int total = 0;
            for (var it : p.getInventory().getContents()) {
                if (it != null && it.getType() == m)
                    total += it.getAmount();
            }
            return total;
        }

        static void remove(Player p, Material m, int qty) {
            int need = qty;
            var inv = p.getInventory();
            for (int i = 0; i < inv.getSize(); i++) {
                var it = inv.getItem(i);
                if (it == null || it.getType() != m)
                    continue;
                int take = Math.min(need, it.getAmount());
                it.setAmount(it.getAmount() - take);
                if (it.getAmount() <= 0)
                    inv.clear(i);
                need -= take;
                if (need <= 0)
                    break;
            }
        }
    }
}
