package io.sisu.neo4j.cloud.gcp;

import com.google.cloud.storage.contrib.nio.CloudStorageFileSystem;
import io.sisu.neo4j.cloud.AssetProvider;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class CloudStorageProvider implements AssetProvider {
    public static final String NAME = "gcs";
    public static final String SCHEME = "gs";

    @Override
    public String getProviderName() {
        return NAME;
    }

    @Override
    public String getProviderScheme() {
        return SCHEME;
    }

    @Override
    public ReadableByteChannel open(URI uri) throws IOException {
        System.out.println("GCSProvider: opening uri: " + uri);
        String bucketName = uri.getHost();
        String objectName = uri.getPath();

        try (CloudStorageFileSystem fs = CloudStorageFileSystem.forBucket(bucketName)) {
            Path path = fs.getPath( objectName);
            return Channels.newChannel(Files.newInputStream(path));
        }
    }
}
