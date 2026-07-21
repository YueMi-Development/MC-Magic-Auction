package org.yuemi.magicauction.plugin.commands.matchmaking;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.yuemi.magicauction.matchmaking.MatchmakingService;
import org.yuemi.magicauction.plugin.commands.SubCommand;
import org.yuemi.magicauction.plugin.game.AuctionManager;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Collections;
import java.util.List;

/**
 * {@code /matchmaking leave [arena]} — leaves the matchmaking queue.
 * <p>
 * If an arena is specified, the player only leaves that arena's queue.
 * Without arguments, the player leaves whichever queue they are in.
 */
public final class LeaveSubCommand implements SubCommand {

    private final AuctionManager auctionManager;

    public LeaveSubCommand(@NotNull AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    @Override
    public String getName() {
        return "leave";
    }

    @Override
    public String getDescription() {
        return "Leave the matchmaking queue.";
    }

    @Override
    public String getSyntax() {
        return "/matchmaking leave [arena]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        var mm = MiniMessage.miniMessage();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>Only players can use the matchmaking queue."));
            return;
        }

        MatchmakingService matchmakingService = auctionManager.getMatchmakingService();
        if (matchmakingService == null) {
            sender.sendMessage(mm.deserialize("<red>Matchmaking service is not available."));
            return;
        }

        String arenaId = args.length > 0 ? args[0] : null;

        boolean left = matchmakingService.leaveQueue(player, arenaId);
        if (left) {
            if (arenaId != null) {
                sender.sendMessage(mm.deserialize("<green>You left the matchmaking queue for <aqua>" + arenaId + "</aqua>."));
            } else {
                sender.sendMessage(mm.deserialize("<green>You left the matchmaking queue."));
            }
        } else {
            if (arenaId != null) {
                sender.sendMessage(mm.deserialize("<yellow>You are not in the queue for <aqua>" + arenaId + "</aqua>."));
            } else {
                sender.sendMessage(mm.deserialize("<yellow>You are not in any matchmaking queue."));
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
