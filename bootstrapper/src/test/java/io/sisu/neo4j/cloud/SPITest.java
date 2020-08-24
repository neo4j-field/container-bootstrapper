package io.sisu.neo4j.cloud;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

public class SPITest {
    @Test
    public void testWeCanFindServiceProviders() throws ClassNotFoundException {
        final ClassLoader currentCL = SPITest.class.getClassLoader();
        final ClassLoader contextCL = Thread.currentThread().getContextClassLoader();

        Class<?> clazz = currentCL.loadClass("io.sisu.neo4j.cloud.LocalFSProvider");

        System.out.println(":: " + clazz.getCanonicalName());

        ServiceLoader<AssetProvider> loader = ServiceLoader.load(AssetProvider.class, contextCL);
        Assertions.assertTrue(loader.findFirst().isPresent());
    }
}
