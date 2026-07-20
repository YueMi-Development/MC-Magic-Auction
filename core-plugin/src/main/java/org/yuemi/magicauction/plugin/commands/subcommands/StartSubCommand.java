package org.yuemi.magicauction.plugin.commands.subcommands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.yuemi.magicauction.plugin.config.ArenaConfig;
import org.yuemi.magicauction.plugin.game.AuctionManager;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class StartSubCommand implements SubCommand {

    private final AuctionManager auctionManager;

    public StartSubCommand(@NotNull AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    @Override
    public String getName() {
        return "start";
    }

    @Override
    public String getDescription() {
        return "Starts an auction session in an arena with 4 players.";
    }

    @Override
    public String getSyntax() {
        return "/magicauction start <arena> [seed] <p1> <p2> <p3> <p4>";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        var mm = MiniMessage.miniMessage();

        // Needs at least 5 arguments: start <arena> <p1> <p2> <p3> <p4>
        if (args.length < 5) {
            sender.sendMessage(mm.deserialize("<red>Usage: " + getSyntax()));
            return;
        }

        String arenaId = args[0];
        ArenaConfig arena = auctionManager.getArenaConfig(arenaId);
        if (arena == null) {
            sender.sendMessage(mm.deserialize("<red>Arena not found: " + arenaId));
            return;
        }

        long seed = -1;
        List<String> playerNames = new ArrayList<>();

        if (args.length == 5) {
            // No seed specified: start <arena> <p1> <p2> <p3> <p4>
            seed = -1;
            playerNames.add(args[1]);
            playerNames.add(args[2]);
            playerNames.add(args[3]);
            playerNames.add(args[4]);
        } else if (args.length >= 6) {
            // Seed might be specified: start <arena> <seed> <p1> <p2> <p3> <p4>
            try {
                seed = Long.parseLong(args[1]);
                playerNames.add(args[2]);
                playerNames.add(args[3]);
                playerNames.add(args[4]);
                playerNames.add(args[5]);
            } catch (NumberFormatException e) {
                // Second argument is not a number, assume it's player 1: start <arena> <p1> <p2> <p3> <p4> <p5>?
                // This means syntax error or too many arguments
                sender.sendMessage(mm.deserialize("<red>Invalid seed or incorrect number of players! Seed must be an integer."));
                sender.sendMessage(mm.deserialize("<red>Usage: " + getSyntax()));
                return;
            }
        }

        // Validate and replace seed if <= 0
        if (seed <= 0) {
            seed = Math.abs(new Random().nextLong()) % 1_000_000_000L + 1;
        }

        // Resolve players
        List<Player> targetPlayers = new ArrayList<>();
        for (String name : playerNames) {
            Player p = Bukkit.getPlayerExact(name);
            if (p == null) {
                sender.sendMessage(mm.deserialize("<red>Player not online: " + name));
                return;
            }
            targetPlayers.add(p);
        }

        if (targetPlayers.size() != 4) {
            sender.sendMessage(mm.deserialize("<red>An auction requires exactly 4 players!"));
            return;
        }

        // Start session
        auctionManager.startSession(arena, seed, targetPlayers);
        sender.sendMessage(mm.deserialize("<green>Auction session started successfully with seed: <yellow>" + seed));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Complete arena names
            List<String> completions = new ArrayList<>();
            for (ArenaConfig config : auctionManager.getArenas()) {
                completions.add(config.getId());
            }
            return completions;
        }
        if (args.length >= 2) {
            // Suggest online player names
            List<String> completions = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
            return completions;
        }
        return Collections.emptyList();
    }
}
