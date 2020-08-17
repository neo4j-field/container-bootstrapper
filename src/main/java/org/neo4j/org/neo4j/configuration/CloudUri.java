package org.neo4j.org.neo4j.configuration;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.ReadableByteChannel;

public class CloudUri {
    private final URI uri;

    public CloudUri(URI uri) {
        this.uri = uri;
    }

    public static CloudUri parseUri(String value) throws URISyntaxException {
        try {
            return new CloudUri(new URI(value));
        } catch (URISyntaxException e) {
            throw e;
        }
    }

    public ReadableByteChannel getChannel() {
        // TODO: finish wiring up a basic channel
        return null;
    }

    public URI getUri() {
        return uri;
    }
}
