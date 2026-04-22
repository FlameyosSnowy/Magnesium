package net.magnesiumbackend.core.runtime.config;

import net.magnesiumbackend.core.runtime.input.InputSource;
import net.magnesiumbackend.core.runtime.lifecycle.LifecyclePolicy;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Configuration for the runtime kernel.
 *
 * <pre>{@code
 * RuntimeConfig config = RuntimeConfig.builder()
 *     .inputSource(new ShellInputSource())
 *     .inputSource(new HttpInputSource(8080))
 *     .lifecyclePolicy(new LatchLifecyclePolicy())
 *     .devToolsEnabled(true)
 *     .build();
 * }</pre>
 */
public final class RuntimeConfig {

    private final List<InputSource> inputSources;
    private final LifecyclePolicy lifecyclePolicy;
    private final boolean devToolsEnabled;

    private RuntimeConfig(Builder builder) {
        this.inputSources = List.copyOf(builder.inputSources);
        this.lifecyclePolicy = Objects.requireNonNull(builder.lifecyclePolicy, "lifecyclePolicy");
        this.devToolsEnabled = builder.devToolsEnabled;
    }

    /**
     * Creates a builder with sensible defaults.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a default configuration.
     *
     * @return default configuration
     */
    public static RuntimeConfig defaults() {
        return new Builder().build();
    }

    public @NotNull List<InputSource> inputSources() {
        return inputSources;
    }

    public @NotNull LifecyclePolicy lifecyclePolicy() {
        return lifecyclePolicy;
    }

    public boolean devToolsEnabled() {
        return devToolsEnabled;
    }

    /**
     * Builder for RuntimeConfig.
     */
    public static final class Builder {
        private final List<InputSource> inputSources = new ArrayList<>();
        private LifecyclePolicy lifecyclePolicy;
        private boolean devToolsEnabled = false;

        /**
         * Adds an input source.
         *
         * @param source the input source
         * @return this builder
         */
        public Builder inputSource(@NotNull InputSource source) {
            inputSources.add(Objects.requireNonNull(source, "source"));
            return this;
        }

        /**
         * Sets the lifecycle policy.
         *
         * @param policy the lifecycle policy
         * @return this builder
         */
        public Builder lifecyclePolicy(@NotNull LifecyclePolicy policy) {
            this.lifecyclePolicy = Objects.requireNonNull(policy, "policy");
            return this;
        }

        /**
         * Enables or disables devtools.
         *
         * @param enabled true to enable
         * @return this builder
         */
        public Builder devToolsEnabled(boolean enabled) {
            this.devToolsEnabled = enabled;
            return this;
        }

        public RuntimeConfig build() {
            if (lifecyclePolicy == null) {
                lifecyclePolicy = new net.magnesiumbackend.core.runtime.lifecycle.LatchLifecyclePolicy();
            }
            return new RuntimeConfig(this);
        }
    }
}
