package io.sisu.neo4j.cloud;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class HttpProvider implements AssetProvider {
    private static final String name = "http";

    @Override
    public String getProviderName() {
        return name;
    }

    @Override
    public String getProviderScheme() {
        return name;
    }

    @Override
    public ReadableByteChannel open(URI uri) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod("GET");

        // XXX: no idea if/when the connection is closed...yolo
        return Channels.newChannel(conn.getInputStream());
    }
}
