package io.sisu.neo4j.configuration;

import org.neo4j.configuration.Config;

import java.nio.channels.ReadableByteChannel;

public class ContainerConfig extends Config {

    public Builder fromReadableByteChannel(ReadableByteChannel channel) {
        return Config.emptyBuilder();
    }
}
