package github.moriyoshi.orebroker.constant;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public final class OreBrokerWorld {
    @NotNull
    public static final World LOBBY = Objects.requireNonNull(Bukkit.getWorld("lobby"));

    @NotNull
    public static final Location LOBBY_LOC = new Location(LOBBY, 0.5, 1.0, 0.5);

    @NotNull
    public static final World GAME = Objects.requireNonNull(Bukkit.getWorld("game"));

    @NotNull
    public static final Location GAME_SPAWN_LOC = new Location(GAME, 0.5, 1.0, 0.5);

}
