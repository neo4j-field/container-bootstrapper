package org.neo4j.configuration;

import io.sisu.neo4j.cloud.UnsupportedProviderException;
import io.sisu.neo4j.cloud.AssetProvider;
import org.neo4j.service.Services;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AssetUri implements Comparable<AssetUri> {
    private final URI uri;
    private final AssetProvider provider;

    private static final Map<String, AssetProvider> registeredProviders = new ConcurrentHashMap<>();

    private static boolean loaded = false;

    public synchronized static void findAndRegister() throws Exception {
        if (loaded)
            return;

        for (AssetProvider provider : Services.loadAll(AssetProvider.class)) {
            registerProvider(provider.getProviderScheme(), provider);
            System.out.println("xxx: registered AssetProvider: " + provider.getClass().getCanonicalName());
        }
        loaded = true;
    }

    protected AssetUri(URI uri) throws UnsupportedProviderException {
        if (!loaded) {
            try {
                findAndRegister();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        AssetProvider provider = registeredProviders.get(uri.getScheme());
        if (provider == null) {
            throw new UnsupportedProviderException("no provider registered for " + uri.getScheme());
        }
        this.provider = provider;
        this.uri = uri;
    }

    public static AssetUri parseUri(String value) throws URISyntaxException, UnsupportedProviderException {
        try {
            return new AssetUri(new URI(value));
        } catch (URISyntaxException | UnsupportedProviderException e) {
            throw e;
        }
    }

    public static boolean registerProvider(String scheme, AssetProvider provider) {
        try {
            registeredProviders.put(scheme, provider);
            return true;
        } catch (Exception e) {
            // nop
        }
        return false;
    }

    public ReadableByteChannel getChannel() throws IOException {
        return provider.open(this.uri);
    }

    public URI getUri() {
        return uri;
    }

    public AssetProvider getProvider() {
        return provider;
    }

    @Override
    public int compareTo(AssetUri o) {
        return uri.compareTo(o.getUri());
    }

    @Override
    public String toString() {
        return String.format("{%s, %s}", provider.getClass().getCanonicalName(), uri.toASCIIString());
    }
}
