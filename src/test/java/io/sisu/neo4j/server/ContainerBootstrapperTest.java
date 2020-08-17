package io.sisu.neo4j.server;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neo4j.server.Bootstrapper;
import org.neo4j.server.ServerStartupException;

import java.io.File;
import java.util.Map;

public class ContainerBootstrapperTest {

    private static class FakeBootstrapper implements Bootstrapper {

        @Override
        public int start(File homeDir, File configFile, Map<String, String> configOverrides) {
            return 0;
        }

        @Override
        public int stop() {
            return 0;
        }
    }

    @Test
    public void testEnvironmentToPropertyConversion() {
        String env = "NEO4J_dbms_memory_heap_initial__size";
        Assertions.assertEquals("dbms.memory.heap.initial_size",
                ContainerBootstrapper.convertEnvToProp(env));
    }

    @Test
    public void containerBootstrapperObeysVersionFlag() {
        Assertions.assertEquals(0,
                ContainerBootstrapper.start(new FakeBootstrapper(), "--version"));
    }

    @Test
    public void containerBootstrapperRequiresHomeDir() {
        Assertions.assertThrows(ServerStartupException.class,
                () -> {
                    ContainerBootstrapper.start(new FakeBootstrapper());
                });
    }
}
