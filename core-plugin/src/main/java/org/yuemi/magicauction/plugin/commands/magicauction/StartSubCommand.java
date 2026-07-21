package org.yuemi.magicauction.plugin.commands.magicauction;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.yuemi.magicauction.plugin.commands.SubCommand;
import org.yuemi.magicauction.plugin.config.ArenaConfig;
import org.yuemi.magicauction.plugin.game.AuctionManager;
import org.yuemi.magicauction.bot.BotHandler;
import org.yuemi.magicauction.seed.RandomSeedGenerator;
import org.yuemi.magicauction.seed.SeedGenerator;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StartSubCommand implements SubCommand {

    private final AuctionManager auctionManager;
    private final SeedGenerator seedGenerator;

    public StartSubCommand(@NotNull AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
        this.seedGenerator = new RandomSeedGenerator();
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
            seed = -1;
            playerNames.add(args[1]);
            playerNames.add(args[2]);
            playerNames.add(args[3]);
            playerNames.add(args[4]);
        } else if (args.length >= 6) {
            try {
                seed = Long.parseLong(args[1]);
                playerNames.add(args[2]);
                playerNames.add(args[3]);
                playerNames.add(args[4]);
                playerNames.add(args[5]);
            } catch (NumberFormatException e) {
                sender.sendMessage(mm.deserialize("<red>Invalid seed or incorrect number of players! Seed must be an integer."));
                sender.sendMessage(mm.deserialize("<red>Usage: " + getSyntax()));
                return;
            }
        }

        seed = seedGenerator.resolve(seed);

        List<Player> targetPlayers = new ArrayList<>();
        int botCount = 0;
        for (String name : playerNames) {
            if ("_BOT_".equalsIgnoreCase(name)) {
                var botHandler = auctionManager.getBotHandler();
                if (botHandler == null) {
                    sender.sendMessage(mm.deserialize("<red>Bot handler is not registered! Cannot spawn bots. Make sure MagicAuctionBot is enabled."));
                    return;
                }
                botCount++;
                targetPlayers.add(botHandler.createBot("Bot_" + botCount));
            } else {
                Player p = Bukkit.getPlayerExact(name);
                if (p == null) {
                    sender.sendMessage(mm.deserialize("<red>Player not online: " + name));
                    return;
                }
                targetPlayers.add(p);
            }
        }

        if (targetPlayers.size() != 4) {
            sender.sendMessage(mm.deserialize("<red>An auction requires exactly 4 players!"));
            return;
        }

        auctionManager.startSession(arena, seed, targetPlayers);
        sender.sendMessage(mm.deserialize("<green>Auction session started successfully with seed: <yellow>" + seed));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            for (ArenaConfig config : auctionManager.getArenas()) {
                completions.add(config.getId());
            }
            return completions;
        }
        if (args.length >= 2) {
            List<String> completions = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
            return completions;
        }
        return Collections.emptyList();
    }
}
