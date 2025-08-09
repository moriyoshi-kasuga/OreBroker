package github.moriyoshi.orebroker.lib;

import org.bukkit.event.Listener;

import github.moriyoshi.orebroker.command.OreBrokerCommand;
import lombok.Getter;

public final class OreBrokerListener implements Listener {
    private static OreBrokerListener instance;

    private OreBrokerListener() {
    }

    public static final OreBrokerListener getInstance() {
        if (instance == null) {
            instance = new OreBrokerListener();
        }
        return instance;
    }
}
