package org.yuemi.magicauction.plugin.config.migrations;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.yuemi.config.api.MigrationStep;

import java.util.List;

/**
 * Migration from config version 3 to version 4.
 *
 * <p>Adds the {@code container.preview.placeholder_pane} and
 * {@code container.preview.placeholder_block} configuration options
 * for customising the placeholder materials used during the preview
 * phase of the auction.
 */
public final class MigrationV3ToV4 implements MigrationStep {

    @Override
    public int getTargetVersion() {
        return 4;
    }

    @Override
    public void migrate(@NotNull FileConfiguration config) {
        if (!config.contains("container.preview")) {
            config.set("container.preview.placeholder_pane", "IRON_BARS");
            config.set("container.preview.placeholder_block", "GLASS");
            config.setComments("container.preview",
                    List.of("Settings for the container preview GUI"));
            config.setComments("container.preview.placeholder_pane",
                    List.of(
                            "Material used for the full-size placeholder when an item's size is known",
                            "but nothing else has been revealed.",
                            "Accepts any Bukkit Material name."
                    ));
            config.setComments("container.preview.placeholder_block",
                    List.of(
                            "Material used for the 1x1 placeholder when rarity is known",
                            "but the item's size is still unknown.",
                            "Accepts any Bukkit Material name."
                    ));
        }
    }
}
