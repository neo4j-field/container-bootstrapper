package io.sisu.neo4j.server;

import org.neo4j.configuration.Config;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.graphdb.facade.GraphDatabaseDependencies;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Copy-pasta from "com.neo4j.server.enterprise.EnterpriseBootstrapper", albeit with reflection for now
 * so this will compile with public community api stuff.
 */
public class EnterpriseContainerBootstrapper extends ContainerBootstrapper {
    @Override
    protected DatabaseManagementService createNeo(Config config, GraphDatabaseDependencies dependencies) {
        try {
            Class<?> clazz = Class.forName("com.neo4j.server.enterprise.EnterpriseManagementServiceFactory");
            Method method = clazz.getMethod("createManagementService", Config.class, GraphDatabaseDependencies.class);
            Object object = method.invoke(null, config, dependencies);
            if (object instanceof DatabaseManagementService) {
                return (DatabaseManagementService) object;
            }
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        System.err.println("XXX: sorry, but reflection issues exist in EnterpriseContainerBootstrapper!");
        System.exit(1);

        // never reached
        return null;
    }
}
