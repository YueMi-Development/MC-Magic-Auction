package org.yuemi.magicauction.plugin.commands;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.yuemi.magicauction.plugin.commands.magicauction.MagicAuctionCommand;
import org.yuemi.magicauction.plugin.commands.matchmaking.MatchmakingCommand;
import org.yuemi.magicauction.plugin.game.AuctionManager;

public final class CommandRegistry {

    private CommandRegistry() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static void registerCommands(@NotNull JavaPlugin plugin, @NotNull AuctionManager auctionManager) {
        var rootCommand = plugin.getCommand("magicauction");
        if (rootCommand != null) {
            MagicAuctionCommand executor = new MagicAuctionCommand(auctionManager);
            rootCommand.setExecutor(executor);
            rootCommand.setTabCompleter(executor);
        }

        var matchmakingCommand = plugin.getCommand("matchmaking");
        if (matchmakingCommand != null) {
            MatchmakingCommand executor = new MatchmakingCommand(auctionManager);
            matchmakingCommand.setExecutor(executor);
            matchmakingCommand.setTabCompleter(executor);
        }
    }
}
