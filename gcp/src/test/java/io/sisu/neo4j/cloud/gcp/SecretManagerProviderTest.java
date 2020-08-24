package io.sisu.neo4j.cloud.gcp;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static io.sisu.neo4j.cloud.gcp.SecretManagerProvider.SecretConfig;
public class SecretManagerProviderTest {
    @Test
    public void canParseUriWithoutVersion() throws URISyntaxException, IOException {
        URI uri = new URI("gsm://project/secret");
        SecretConfig config = new SecretConfig(uri);
        Assertions.assertEquals("project", config.project);
        Assertions.assertEquals("secret", config.secretId);
        Assertions.assertEquals("latest", config.versionId);
    }

    @Test
    public void canParseUriWithVersion() throws URISyntaxException, IOException {
        URI uri = new URI("gsm://project/secret?version=myversion");
        SecretConfig config = new SecretConfig(uri);
        Assertions.assertEquals("project", config.project);
        Assertions.assertEquals("secret", config.secretId);
        Assertions.assertEquals("myversion", config.versionId);
    }

}
