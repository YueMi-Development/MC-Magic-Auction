package org.yuemi.magicauction.plugin.config.migrations;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.yuemi.config.api.MigrationStep;

import java.util.List;

/**
 * Migration from config version 1 to version 2.
 *
 * <p>Adds the {@code matchmaking} configuration section with global toggle
 * and timeout action setting.
 */
public final class MigrationV1ToV2 implements MigrationStep {

    @Override
    public int getTargetVersion() {
        return 2;
    }

    @Override
    public void migrate(@NotNull FileConfiguration config) {
        if (!config.contains("matchmaking")) {
            config.set("matchmaking.enabled", true);
            config.set("matchmaking.min-players", 4);
            config.set("matchmaking.timeout-seconds", 120);
            config.set("matchmaking.allow-bots", true);
            config.set("matchmaking.on-timeout", "fill_bots");
            config.setComments("matchmaking",
                    List.of("Matchmaking settings"));
            config.setComments("matchmaking.enabled",
                    List.of(
                            "Whether the matchmaking system is enabled globally.",
                            "When false, players cannot use /matchmaking join."
                    ));
            config.setComments("matchmaking.min-players",
                    List.of("Minimum number of real players required before a queue can start."));
            config.setComments("matchmaking.timeout-seconds",
                    List.of("Seconds to wait for the queue to fill before taking action (see on-timeout)."));
            config.setComments("matchmaking.allow-bots",
                    List.of(
                            "Whether to fill remaining slots with bots when the timeout is reached.",
                            "Only used when on-timeout is \"fill_bots\"."
                    ));
            config.setComments("matchmaking.on-timeout",
                    List.of(
                            "What action to take when the queue timeout is reached without enough players.",
                            "\"fill_bots\" — fill remaining slots with bots and start the auction",
                            "\"cancel\"   — cancel the queue and notify waiting players"
                    ));
        }
    }
}
