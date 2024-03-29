package io.sisu.neo4j.cloud;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;

/**
 * Simple abstraction for local filesystem access, primarily testing.
 */
public class LocalFSProvider implements AssetProvider {

    public static final String NAME = "file";

    @Override
    public String getProviderName() {
        return NAME;
    }

    @Override
    public String getProviderScheme() {
        return NAME;
    }

    @Override
    public ReadableByteChannel open(URI uri) throws IOException {
        return FileChannel.open(Path.of(uri));
    }
}
