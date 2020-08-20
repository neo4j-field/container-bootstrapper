package io.sisu.neo4j.io.sisu.neo4j.cloud;

import org.neo4j.org.neo4j.configuration.AssetUri;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;

/**
 * Simple abstraction for local filesystem acccess, primarily testing.
 */
public class LocalFSProvider implements AssetProvider {

    public static final String NAME = "file";

    static {
        if (!AssetUri.registerProvider(NAME, new LocalFSProvider())) {
            System.err.println("Failed to register LocalFSProvider");
        }
    }

    @Override
    public String getProviderName() {
        return NAME;
    }

    @Override
    public ReadableByteChannel open(URI uri) throws IOException {
        return FileChannel.open(Path.of(uri));
    }
}
