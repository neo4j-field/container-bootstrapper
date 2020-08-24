package io.sisu.neo4j.cloud.gcp;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;

/**
 * This test requires manual intervention since it hits a live GCP environment.
 */
public class SecretManagerProviderManualTest {

    @Test
    void canRetrieveSecretPayload() throws Exception {
        Assumptions.assumeFalse(
                System.getenv().getOrDefault("GOOGLE_APPLICATION_CREDENTIALS", "")
                        .isEmpty());
        SecretManagerProvider provider = new SecretManagerProvider();
        ReadableByteChannel channel = provider.open(new URI("gsm://neo4j-se-team-201905/test-privatekey?version=2"));

        Assertions.assertNotNull(channel);
        BufferedReader reader = new BufferedReader(Channels.newReader(channel, StandardCharsets.UTF_8));
        Assertions.assertEquals("-----BEGIN PRIVATE KEY-----", reader.readLine());
        reader.close();
    }
}
