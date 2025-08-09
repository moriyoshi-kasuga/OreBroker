
package github.moriyoshi.orebroker.util;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class BukkitUtil {
    private BukkitUtil() {
        // Utility class, prevent instantiation
    }

    public static Component mm(@NotNull final Object message) {
        if (message instanceof Component) {
            return (Component) message;
        }

        final String str = String.valueOf(message);
        return MiniMessage.miniMessage()
                .deserialize(str)
                .decoration(TextDecoration.ITALIC, false);
    }

    public static List<Component> mmList(@NotNull final Object... list) {
        return mmList(List.of(list));
    }

    public static List<Component> mmList(@NotNull final List<?> list) {
        return list.stream().map(BukkitUtil::mm).collect(Collectors.toList());
    }

    public static Component mmConcat(@NotNull final Object... messages) {
        return mmConcat(List.of(messages));
    }

    public static Component mmConcat(@NotNull final List<?> messages) {
        return mmList(messages).stream()
                .reduce(Component.empty(), Component::append);
    }

    public static void consoleCommand(final String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    public static void broadcast(final Object... messages) {
        Bukkit.broadcast(mmConcat(messages));
    }

    public static void send(Player player, final Object... messages) {
        player.sendMessage(mmConcat(messages));
    }

    public static void debug(final Object... messages) {
        Component combinedComponent = mm("<dark_gray>[DEBUG]</dark_gray>");
        for (int i = 0; i < messages.length; i++) {
            combinedComponent = combinedComponent.append(mm(messages[i]));
            if (i < messages.length - 1) {
                combinedComponent = combinedComponent.append(mm(","));
            }
        }
        Bukkit.broadcast(combinedComponent);
    }
}
