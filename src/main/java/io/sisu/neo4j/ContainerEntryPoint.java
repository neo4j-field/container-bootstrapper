package io.sisu.neo4j;

import org.neo4j.server.Bootstrapper;
import org.neo4j.server.NeoBootstrapper;

import static org.neo4j.internal.unsafe.UnsafeUtil.disableIllegalAccessLogger;

/**
 * Simplified entrypoint for container-based Neo4j
 */
public class ContainerEntryPoint {

    public static void main(String [] args) {
        disableIllegalAccessLogger();

        int status = ContainerBootstrapper.start(new ContainerBootstrapper(), args);
        if (status != 0) {
            System.exit(status);
        }
    }
}
