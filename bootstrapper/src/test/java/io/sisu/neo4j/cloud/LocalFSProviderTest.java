package io.sisu.neo4j.cloud;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LocalFSProviderTest {
    public static void main(String []argv) throws URISyntaxException, IOException {
        URI uri = new URI("file:/certs/key.pem");
        Path path = Paths.get(uri);
        ReadableByteChannel chan =  FileChannel.open(path);
        try (BufferedReader br = new BufferedReader(Channels.newReader(chan, StandardCharsets.UTF_8))) {
            System.out.println(br.readLine());
        }
    }
}
