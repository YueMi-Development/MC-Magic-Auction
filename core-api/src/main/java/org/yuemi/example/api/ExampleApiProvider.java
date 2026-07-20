package org.yuemi.example.api;

import org.jetbrains.annotations.NotNull;

/**
 * Entry point for accessing the ExamplePlugin API.
 *
 * Consumers should depend on this interface, not implementation details.
 */
public interface ExampleApiProvider {

    /**
     * @return active API instance
     */
    @NotNull
    ExampleApi getApi();
}
