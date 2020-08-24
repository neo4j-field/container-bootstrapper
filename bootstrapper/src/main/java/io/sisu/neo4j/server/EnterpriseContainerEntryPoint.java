package io.sisu.neo4j.server;


import static org.neo4j.internal.unsafe.UnsafeUtil.disableIllegalAccessLogger;

public class EnterpriseContainerEntryPoint {
    public static void main(String[] args) {
        disableIllegalAccessLogger();

        if (!System.getenv().getOrDefault("NEO4J_ACCEPT_LICENSE_AGREEMENT", "no").equalsIgnoreCase("yes")) {
            System.err.println(
                    "In order to use Neo4j Enterprise Edition you must accept the license agreement.\n" +
                    "(c) Neo4j Sweden AB.  2019.  All Rights Reserved.\n" +
                    "Use of this Software without a proper commercial license with Neo4j,\n" +
                    "Inc. or its affiliates is prohibited.\n" +
                    "Email inquiries can be directed to: licensing@neo4j.com\n" +
                    "More information is also available at: https://neo4j.com/licensing/\n" +
                    "To accept the license agreement set the environment variable\n" +
                    "NEO4J_ACCEPT_LICENSE_AGREEMENT=yes\n" +
                    "To do this you can use the following docker argument:\n" +
                    "        --env=NEO4J_ACCEPT_LICENSE_AGREEMENT=yes\n");
            System.exit(1);
        }

        int status = ContainerBootstrapper.start(new EnterpriseContainerBootstrapper(), args);
        if (status != 0) {
            System.exit(status);
        }
    }
}
