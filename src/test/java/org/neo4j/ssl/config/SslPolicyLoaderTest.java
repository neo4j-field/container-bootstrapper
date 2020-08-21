package org.neo4j.ssl.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.neo4j.configuration.Config;
import org.neo4j.configuration.ssl.ClientAuth;
import org.neo4j.configuration.ssl.ContainerSslPolicyConfig;
import org.neo4j.configuration.ssl.SslPolicyScope;
import org.neo4j.logging.FormattedLogProvider;
import org.neo4j.configuration.AssetUri;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class SslPolicyLoaderTest {

    @Test
    public void canLoadPrivateKeyFromLocalFSProvider(@TempDir Path tempDir) throws Exception {
        Files.createDirectory(tempDir.resolve("trusted"));
        Files.createDirectory(tempDir.resolve("revoked"));

        ContainerSslPolicyConfig policy = ContainerSslPolicyConfig.forScope(SslPolicyScope.TESTING);
        URI keyUri = this.getClass().getResource("/key.pem").toURI();
        URI certUri = this.getClass().getResource("/cert.pem").toURI();

        Config config = Config.newBuilder()
                .set(policy.enabled, Boolean.TRUE)
                .set(policy.base_directory, tempDir)
                .set(policy.private_key, AssetUri.parseUri(keyUri.toASCIIString()))
                .set(policy.public_certificate, AssetUri.parseUri(certUri.toASCIIString()))
                .set(policy.client_auth, ClientAuth.NONE)
                .build();

        Assertions.assertTrue(config.get(policy.enabled));

        SslPolicyLoader loader = SslPolicyLoader.create(config, FormattedLogProvider.toOutputStream(System.out));
        Assertions.assertTrue(loader.hasPolicyForSource(SslPolicyScope.TESTING));

        PrivateKey key = loader.getPolicy(SslPolicyScope.TESTING).privateKey();
        X509Certificate[] chain = loader.getPolicy(SslPolicyScope.TESTING).certificateChain();

        Assertions.assertEquals("PKCS#8", key.getFormat());
        Assertions.assertEquals(1, chain.length);
        Assertions.assertEquals("CN=localhost", chain[0].getIssuerDN().getName());
    }
}
