package org.neo4j.org.neo4j.configuration;

import io.sisu.neo4j.io.sisu.neo4j.cloud.UnsupportedProviderException;
import org.neo4j.configuration.SettingValueParser;

import java.net.URISyntaxException;

public class ContainerSettingValueParsers {
    public static final SettingValueParser<AssetUri> ASSET_URI = new SettingValueParser<>() {

        @Override
        public AssetUri parse(String value) {
            try {
                return AssetUri.parseUri(value);
            } catch (URISyntaxException | UnsupportedProviderException e) {
                throw new IllegalArgumentException(String.format("'%s' is not a valid asset uri", value), e);
            }
        }

        @Override
        public String getDescription() {
            return "a remote asset uri";
        }

        @Override
        public Class<AssetUri> getType() {
            return AssetUri.class;
        }
    };
}
