package io.sisu.neo4j.cloud;

import com.google.cloud.storage.contrib.nio.CloudStorageFileSystem;
import com.google.cloud.storage.contrib.nio.CloudStorageFileSystemProvider;
import com.google.cloud.storage.contrib.nio.testing.LocalStorageHelper;
import org.bouncycastle.util.Strings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.configuration.AssetUri;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class GCSProviderTest {

    private static final String BUCKET = "testBucket";
    private static final String OBJECT = "testObject";

    @BeforeAll
    public static void setup() throws IOException {
        CloudStorageFileSystemProvider.setStorageOptions(LocalStorageHelper.getOptions());
        try (CloudStorageFileSystem fs = CloudStorageFileSystem.forBucket(BUCKET)) {
            Path path = fs.getPath(fs.getSeparator() + OBJECT);
            try (OutputStream os = Files.newOutputStream(path)) {
                os.write(Strings.toByteArray("Test Message"));
            }
        }
    }

    @Test
    public void testCanFetchFileFromByUri() throws Exception {
        AssetUri assetUri = AssetUri.parseUri("gs://" + BUCKET + "/" + OBJECT);

        try (BufferedReader reader = new BufferedReader(Channels.newReader(assetUri.getChannel(), StandardCharsets.UTF_8))) {
            Assertions.assertEquals("Test Message", reader.readLine());
        }
    }
}
