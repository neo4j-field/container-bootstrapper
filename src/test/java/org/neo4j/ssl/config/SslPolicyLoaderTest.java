package org.neo4j.ssl.config;

import io.sisu.neo4j.io.sisu.neo4j.cloud.LocalFSProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.org.neo4j.configuration.AssetUri;

import java.io.BufferedReader;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;

public class SslPolicyLoaderTest {

    @BeforeAll
    public static void setup() {
        AssetUri.registerProvider(LocalFSProvider.NAME, new LocalFSProvider());
    }

    @Test
    public void canLoadPrivateKeyFromLocalFSProvider() throws Exception {
        URI keyuri = this.getClass().getResource("/privatekey.pem").toURI();
        AssetUri uri = AssetUri.parseUri(keyuri.toASCIIString());

        try (BufferedReader reader = new BufferedReader(Channels.newReader(uri.getChannel(), StandardCharsets.UTF_8))) {
            Assertions.assertEquals("-----BEGIN PRIVATE KEY-----", reader.readLine());
        }
    }
}
