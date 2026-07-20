package org.yuemi.magicauction.plugin.config.migrations;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.yuemi.config.api.MigrationStep;

/**
 * Example migration from config version 1 to version 2.
 *
 * <p>Add any new config keys introduced in version 2 below.
 * Remove this comment block and replace with real migration logic when needed.
 */
public final class MigrationV1ToV2 implements MigrationStep {

    @Override
    public int getTargetVersion() {
        return 2;
    }

    @Override
    public void migrate(@NotNull FileConfiguration config) {
        // Example: add a new config key with a default value
        // config.set("new-setting", true);
        // config.setComments("new-setting", List.of("Description of the new setting."));
    }
}
