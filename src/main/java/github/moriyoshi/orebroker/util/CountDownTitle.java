package github.moriyoshi.orebroker.util;

import java.time.Duration;
import java.util.List;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.Setter;
import lombok.val;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

public class CountDownTitle {

    private final int seconds;

    @Accessors(chain = true)
    @Setter
    @Nullable
    private Sound generalCountDownSound = null;

    @Accessors(chain = true)
    @Setter
    private int finalPhaceSeconds = 3;

    @Accessors(chain = true)
    @Setter
    @Nullable
    private Sound finalPhaceCountDownSound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;

    @Accessors(chain = true)
    @Setter
    @Nullable
    private Sound endedCountDownSound = Sound.ENTITY_GENERIC_EXPLODE;

    @Accessors(chain = true)
    @Setter
    @NotNull
    private Component endedCountDownMessage = BukkitUtil.mm("<red>Time's up!");

    private final Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofMinutes(1500), Duration.ZERO);

    public CountDownTitle(final int seconds) {
        this.seconds = seconds;
    }

    public final void run(final Plugin plugin, final List<Player> players) {
        run(plugin, players, () -> {
        });
    }

    public final void run(final Plugin plugin, final List<Player> players, Runnable ended) {
        new BukkitRunnable() {

            private int seconds = CountDownTitle.this.seconds;

            @Override
            public void run() {
                if (seconds <= 0) {
                    if (endedCountDownSound != null) {
                        for (val player : players) {
                            player.playSound(player.getLocation(), endedCountDownSound, 1.0f, 1.0f);
                        }
                    }
                    for (val player : players) {
                        endedCountDownTitle(player);
                    }
                    this.cancel();
                    return;
                }

                if (seconds <= finalPhaceSeconds) {
                    if (finalPhaceCountDownSound != null) {
                        for (val player : players) {
                            player.playSound(player.getLocation(), finalPhaceCountDownSound, 1.0f, 1.0f);
                        }
                    }
                    for (val player : players) {
                        finalPhaceCountDownTitle(player, seconds);
                    }
                } else {
                    if (generalCountDownSound != null) {
                        for (val player : players) {
                            player.playSound(player.getLocation(), generalCountDownSound, 1.0f, 1.0f);
                        }
                    }
                    for (val player : players) {
                        generalCountDownTitle(player, seconds);
                    }
                }

                seconds--;
            }

        }.runTaskTimer(plugin, 0, 20);

    }

    protected void generalCountDownTitle(final Player player, final int seconds) {
        val title = Title.title(BukkitUtil.mm("<yellow>" + seconds + " seconds left"),
                Component.empty(), times);

        player.showTitle(title);
    }

    protected void finalPhaceCountDownTitle(final Player player, final int seconds) {
        val title = Title.title(BukkitUtil.mm("<red>" + seconds + " seconds left"),
                Component.empty(), times);

        player.showTitle(title);
    }

    protected void endedCountDownTitle(final Player player) {
        val title = Title.title(endedCountDownMessage, Component.empty(), times);

        player.showTitle(title);
    }
}
