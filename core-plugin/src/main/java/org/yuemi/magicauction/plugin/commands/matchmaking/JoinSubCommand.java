package org.yuemi.magicauction.plugin.commands.matchmaking;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.yuemi.magicauction.matchmaking.MatchmakingService;
import org.yuemi.magicauction.plugin.commands.SubCommand;
import org.yuemi.magicauction.plugin.config.ArenaConfig;
import org.yuemi.magicauction.plugin.game.AuctionManager;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@code /matchmaking join <arena>} — joins the matchmaking queue for a specific arena.
 */
public final class JoinSubCommand implements SubCommand {

    private final AuctionManager auctionManager;

    public JoinSubCommand(@NotNull AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public String getDescription() {
        return "Join the matchmaking queue for an arena.";
    }

    @Override
    public String getSyntax() {
        return "/matchmaking join <arena>";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        var mm = MiniMessage.miniMessage();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>Only players can use the matchmaking queue."));
            return;
        }

        // Global toggle check
        if (!auctionManager.isMatchmakingGloballyEnabled()) {
            sender.sendMessage(mm.deserialize("<red>Matchmaking is currently disabled."));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(mm.deserialize("<red>Usage: " + getSyntax()));
            return;
        }

        String arenaId = args[0];
        ArenaConfig arena = auctionManager.getArenaConfig(arenaId);
        if (arena == null) {
            sender.sendMessage(mm.deserialize("<red>Arena not found: " + arenaId));
            return;
        }

        // Check if already in an active auction session
        if (auctionManager.isPlayerInSession(player)) {
            sender.sendMessage(mm.deserialize("<red>You are already in an active auction session."));
            return;
        }

        MatchmakingService matchmakingService = auctionManager.getMatchmakingService();
        if (matchmakingService == null) {
            sender.sendMessage(mm.deserialize("<red>Matchmaking service is not available."));
            return;
        }

        // Check if already in a queue
        String currentQueue = matchmakingService.getPlayerQueue(player);
        if (currentQueue != null) {
            if (currentQueue.equalsIgnoreCase(arenaId)) {
                sender.sendMessage(mm.deserialize("<yellow>You are already in the queue for <aqua>" + arena.getName() + "</aqua>."));
                return;
            }
            // Leave the previous queue first (auto-switch)
            matchmakingService.leaveQueue(player, currentQueue);
        }

        // Read matchmaking config from global config.yml
        int minPlayers = auctionManager.getMatchmakingMinPlayers();
        int timeoutSeconds = auctionManager.getMatchmakingTimeoutSeconds();
        boolean allowBots = auctionManager.isMatchmakingAllowBots();

        boolean joined = matchmakingService.joinQueue(
                player,
                arenaId,
                minPlayers,
                timeoutSeconds,
                allowBots
        );

        if (joined) {
            int queueSize = matchmakingService.getQueueSize(arenaId);
            sender.sendMessage(mm.deserialize(
                    "<green>You joined the matchmaking queue for <aqua>" + arena.getName()
                    + "</aqua>. <gray>(" + queueSize + "/" + minPlayers + " players)</gray>"));
        } else {
            sender.sendMessage(mm.deserialize("<red>Failed to join the matchmaking queue for " + arena.getName() + "."));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();
            for (ArenaConfig config : auctionManager.getArenas()) {
                if (config.getId().toLowerCase().startsWith(partial)) {
                    completions.add(config.getId());
                }
            }
            return completions;
        }
        return Collections.emptyList();
    }
}
