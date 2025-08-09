package github.moriyoshi.orebroker;

import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.plugin.java.JavaPlugin;

public class OreBroker extends JavaPlugin {
    @Override
    public void onEnable() {
        initWorld();
    }

    @Override
    public void onDisable() {

    }

    private void initWorld() {
        new WorldCreator("lobby")
                .environment(Environment.NORMAL)
                .type(WorldType.FLAT)
                .generateStructures(false)
                .createWorld();
    }
}
