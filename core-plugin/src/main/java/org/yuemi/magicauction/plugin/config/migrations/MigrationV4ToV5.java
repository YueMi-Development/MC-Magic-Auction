package org.yuemi.magicauction.plugin.config.migrations;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.yuemi.config.api.MigrationStep;

import java.util.List;

/**
 * Migration from config version 4 to version 5.
 *
 * <p>Adds the {@code container.bid.sounds} configuration section with
 * sound events for successful bids and bid cancellations.
 */
public final class MigrationV4ToV5 implements MigrationStep {

    @Override
    public int getTargetVersion() {
        return 5;
    }

    @Override
    public void migrate(@NotNull FileConfiguration config) {
        if (!config.contains("container.bid")) {
            config.set("container.bid.sounds.success", "minecraft:entity.villager.trade");
            config.set("container.bid.sounds.cancel", "minecraft:entity.villager.no");
            config.set("container.bid.sounds.invalid", "minecraft:entity.villager.angry");
            config.setComments("container.bid",
                    List.of("Settings for the bidding anvil GUI"));
            config.setComments("container.bid.sounds",
                    List.of("Sound events played during bidding"));
            config.setComments("container.bid.sounds.success",
                    List.of(
                            "Played to the bidder when they successfully place a bid.",
                            "Accepts a Minecraft namespaced sound key."
                    ));
            config.setComments("container.bid.sounds.cancel",
                    List.of(
                            "Played when a player closes the anvil without placing a bid.",
                            "Accepts a Minecraft namespaced sound key."
                    ));
            config.setComments("container.bid.sounds.invalid",
                    List.of(
                            "Played when a player's bid is invalid (format error, zero, or exceeds balance).",
                            "Accepts a Minecraft namespaced sound key."
                    ));
        }
    }
}
