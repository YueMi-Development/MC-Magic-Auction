package org.yuemi.magicauction.bot;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

public final class BotPlayerProxy implements InvocationHandler {

    private final String name;
    private final UUID uuid;

    public BotPlayerProxy(@NotNull String name, @NotNull UUID uuid) {
        this.name = name;
        this.uuid = uuid;
    }

    @NotNull
    public static Player create(@NotNull String name, @NotNull UUID uuid) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                new BotPlayerProxy(name, uuid)
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        switch (method.getName()) {
            case "getName":
            case "getDisplayName":
                return name;
            case "getUniqueId":
                return uuid;
            case "sendMessage":
            case "sendRichMessage":
                return null;
            case "isOnline":
                return true;
            case "isValid":
                return true;
            case "hashCode":
                return uuid.hashCode();
            case "equals":
                if (args == null || args.length != 1) return false;
                if (args[0] instanceof Player other) {
                    return uuid.equals(other.getUniqueId());
                }
                return false;
            case "toString":
                return "BotPlayer{name=" + name + ", uuid=" + uuid + "}";
            case "getWorld":
                return getFallbackWorld();
            case "getLocation":
                return getFallbackLocation();
            case "closeInventory":
                return null;
            default:
                Class<?> returnType = method.getReturnType();
                if (returnType.equals(boolean.class)) {
                    return false;
                } else if (returnType.equals(int.class)) {
                    return 0;
                } else if (returnType.equals(double.class)) {
                    return 0.0;
                } else if (returnType.equals(long.class)) {
                    return 0L;
                }
                return null;
        }
    }

    private World getFallbackWorld() {
        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
    }

    private Location getFallbackLocation() {
        World w = getFallbackWorld();
        return w != null ? w.getSpawnLocation() : null;
    }
}
