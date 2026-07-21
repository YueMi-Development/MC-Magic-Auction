package org.yuemi.magicauction.plugin.commands.matchmaking;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yuemi.magicauction.plugin.commands.SubCommand;
import org.yuemi.magicauction.plugin.game.AuctionManager;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.*;

/**
 * Root command dispatcher for {@code /matchmaking}.
 * <p>
 * Delegates to subcommands (join, leave) using the same pattern as
 * {@link org.yuemi.magicauction.plugin.commands.magicauction.MagicAuctionCommand}.
 */
public final class MatchmakingCommand implements CommandExecutor, TabCompleter {

    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public MatchmakingCommand(@NotNull AuctionManager auctionManager) {
        registerSubCommand(new JoinSubCommand(auctionManager));
        registerSubCommand(new LeaveSubCommand(auctionManager));
    }

    private void registerSubCommand(@NotNull SubCommand command) {
        subCommands.put(command.getName().toLowerCase(), command);
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        var mm = MiniMessage.miniMessage();
        if (args.length == 0) {
            sender.sendMessage(mm.deserialize("<gold><bold>Matchmaking Commands:</bold>"));
            for (SubCommand sub : subCommands.values()) {
                sender.sendMessage(mm.deserialize("<yellow>" + sub.getSyntax() + " <gray>- " + sub.getDescription()));
            }
            return true;
        }

        String subName = args[0].toLowerCase();
        SubCommand sub = subCommands.get(subName);
        if (sub == null) {
            sender.sendMessage(mm.deserialize("<red>Unknown subcommand: " + subName));
            sender.sendMessage(mm.deserialize("<red>Usage: /matchmaking <join|leave>"));
            return true;
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        sub.execute(sender, subArgs);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
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
