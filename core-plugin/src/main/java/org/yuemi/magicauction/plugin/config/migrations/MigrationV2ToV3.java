package org.yuemi.magicauction.plugin.config.migrations;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.yuemi.config.api.MigrationStep;

import java.util.List;

/**
 * Migration from config version 2 to version 3.
 *
 * <p>Adds the {@code container.reveal} configuration section with a global
 * toggle for the container reveal system and a separate toggle for the
 * flicker animation.
 */
public final class MigrationV2ToV3 implements MigrationStep {

    @Override
    public int getTargetVersion() {
        return 3;
    }

    @Override
    public void migrate(@NotNull FileConfiguration config) {
        if (!config.contains("container.reveal")) {
            config.set("container.reveal.enabled", true);
            config.set("container.reveal.animation", true);
            config.setComments("container",
                    List.of("Container preview & reveal settings"));
            config.setComments("container.reveal",
                    List.of("Settings for the item reveal system in the auction preview and winner animation"));
            config.setComments("container.reveal.enabled",
                    List.of(
                            "Whether the container reveal system is enabled globally.",
                            "When false, the winner reveal animation is skipped entirely."
                    ));
            config.setComments("container.reveal.animation",
                    List.of(
                            "Whether to play the flicker animation during item reveals.",
                            "When false, items reveal one-by-one without the flicker effect."
                    ));
        }
    }
}
