package io.sisu.neo4j.server;


import static org.neo4j.internal.unsafe.UnsafeUtil.disableIllegalAccessLogger;

/**
 * Simplified entrypoint for container-based Neo4j
 */
public class CommunityContainerEntryPoint {

    public static void main(String [] args) {
        disableIllegalAccessLogger();

        int status = ContainerBootstrapper.start(new CommunityContainerBootstrapper(), args);
        if (status != 0) {
            System.exit(status);
        }
    }
}
