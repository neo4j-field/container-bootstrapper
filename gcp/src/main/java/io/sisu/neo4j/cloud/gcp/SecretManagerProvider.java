package io.sisu.neo4j.cloud.gcp;

import com.google.cloud.secretmanager.v1.*;
import io.sisu.neo4j.cloud.AssetProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * Implementation of {@link AssetProvider} for Google Secret Manager.
 */
public class SecretManagerProvider implements AssetProvider {

    private static final String SCHEME = "gsm";
    private static final String NAME = "google-secret-manager";

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
        // XXX: we assume a uri of the format "gsm://projectName/secretId
        final SecretConfig config = new SecretConfig(uri);
        byte[] bytes = null;

        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            AccessSecretVersionResponse response = client.accessSecretVersion(config.getSecretVersionName());
            if (response.hasPayload()) {
                bytes = response.getPayload().getData().toByteArray();
            }
        }

        if (bytes == null) {
            throw new IOException("unable to get payload for secret " + uri);
        }

        return Channels.newChannel(new ByteArrayInputStream(bytes));
    }

    protected static class SecretConfig {
        public final String project;
        public final String secretId;
        public final String versionId;

        public SecretConfig(URI uri) throws IOException {
            this.project = uri.getHost();
            this.secretId = uri.getPath().replaceFirst("/", "");

            if (uri.getQuery() != null) {
                for (String pair : uri.getQuery().split(",")) {
                    String parts[] = pair.split("=");
                    if (parts[0].equalsIgnoreCase("version")) {
                        this.versionId = parts[1];
                        return;
                    }
                }
            }
            this.versionId = "latest";
        }

        public SecretVersionName getSecretVersionName() {
            return SecretVersionName.of(project, secretId, versionId);
        }

        @Override
        public String toString() {
            return String.format("%s://%s/%s?version=%s", SCHEME, project, secretId, versionId);
        }
    }
}
