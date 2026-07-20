package org.yuemi.magicauction.api;

import org.jetbrains.annotations.NotNull;

/**
 * Entry point for accessing the MagicAuction API.
 *
 * Consumers should depend on this interface, not implementation details.
 */
public interface MagicAuctionApiProvider {

    /**
     * @return active API instance
     */
    @NotNull
    MagicAuctionApi getApi();
}
