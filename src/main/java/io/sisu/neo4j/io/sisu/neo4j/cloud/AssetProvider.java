package io.sisu.neo4j.io.sisu.neo4j.cloud;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.ReadableByteChannel;

public interface AssetProvider {
    public String getProviderName();

    public ReadableByteChannel open(URI uri) throws IOException;
}
