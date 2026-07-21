package org.yuemi.magicauction.plugin.commands.magicauction;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.yuemi.magicauction.plugin.commands.SubCommand;
import org.yuemi.magicauction.plugin.commands.matchmaking.JoinSubCommand;
import org.yuemi.magicauction.plugin.commands.matchmaking.LeaveSubCommand;
import org.yuemi.magicauction.plugin.game.AuctionManager;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.*;

/**
 * Subcommand of {@code /magicauction} that exposes matchmaking operations.
 * <p>
 * Usage: {@code /magicauction matchmaking join <arena>}
 *        {@code /magicauction matchmaking leave [arena]}
 */
public final class MatchmakingSubCommand implements SubCommand {

    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public MatchmakingSubCommand(@NotNull AuctionManager auctionManager) {
        registerSubCommand(new JoinSubCommand(auctionManager));
        registerSubCommand(new LeaveSubCommand(auctionManager));
    }

    private void registerSubCommand(@NotNull SubCommand command) {
        subCommands.put(command.getName().toLowerCase(), command);
    }

    @Override
    public String getName() {
        return "matchmaking";
    }

    @Override
    public String getDescription() {
        return "Matchmaking queue commands.";
    }

    @Override
    public String getSyntax() {
        return "/magicauction matchmaking <join|leave>";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        var mm = MiniMessage.miniMessage();
        if (args.length == 0) {
            sender.sendMessage(mm.deserialize("<gold>Matchmaking subcommands:</gold>"));
            for (SubCommand sub : subCommands.values()) {
                String subSyntax = sub.getSyntax().replaceFirst("/matchmaking ", "");
                sender.sendMessage(mm.deserialize("<yellow>/magicauction matchmaking " + subSyntax + " <gray>- " + sub.getDescription()));
            }
            return;
        }

        String subName = args[0].toLowerCase();
        SubCommand sub = subCommands.get(subName);
        if (sub == null) {
            sender.sendMessage(mm.deserialize("<red>Unknown matchmaking subcommand: " + subName));
            return;
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        sub.execute(sender, subArgs);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String search = args[0].toLowerCase();
            for (String subName : subCommands.keySet()) {
                if (subName.startsWith(search)) {
                    completions.add(subName);
                }
            }
            return completions;
        }

        String subName = args[0].toLowerCase();
        SubCommand sub = subCommands.get(subName);
        if (sub != null) {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            return sub.tabComplete(sender, subArgs);
        }

        return Collections.emptyList();
    }
}
