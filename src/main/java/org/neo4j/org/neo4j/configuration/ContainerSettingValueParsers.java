package org.neo4j.org.neo4j.configuration;

import org.neo4j.configuration.SettingValueParser;

import java.net.URISyntaxException;

public class ContainerSettingValueParsers {
    public static final SettingValueParser<CloudUri> CLOUD_URI = new SettingValueParser<>() {
        @Override
        public CloudUri parse(String value) {
            // TODO: implement tie ins to cloud readers
            try {
                return CloudUri.parseUri(value);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(String.format("'%s' is not a valid cloud uri", value), e);
            }
        }

        @Override
        public String getDescription() {
            return "a cloud-service uri";
        }

        @Override
        public Class<CloudUri> getType() {
            return CloudUri.class;
        }
    };
}
