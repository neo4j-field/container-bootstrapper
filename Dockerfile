FROM alpine:3.12 AS base
RUN apk update && apk add openjdk11-jre-headless openssl

ARG NEO4J_SHA256=4f663a520bec40dfd0b1972feb3cf93af321c230b448adb6dc917717e67a1271
ARG NEO4J_TARBALL=neo4j-community-4.1.1-unix.tar.gz
ARG NEO4J_DOWNLOAD_URI=https://dist.neo4j.org
ARG NEO4J_HOME="/neo4j"

RUN addgroup -g 7474 -S neo4j \
    && adduser -u 7474 -S -H -h "${NEO4J_HOME}" -G neo4j neo4j

RUN wget -q -P /tmp "${NEO4J_DOWNLOAD_URI}/${NEO4J_TARBALL}" \
    && echo "${NEO4J_SHA256}  /tmp/${NEO4J_TARBALL}" | sha256sum -csw \
    && tar x -z -f "/tmp/${NEO4J_TARBALL}" -C /tmp \
    && rm "/tmp/${NEO4J_TARBALL}" \
    && mv /tmp/neo4j-* "${NEO4J_HOME}" \
    && chown -R neo4j:neo4j "${NEO4J_HOME}/data" "${NEO4J_HOME}/logs" \
    && mv "${NEO4J_HOME}/data" /data \
    && mv "${NEO4J_HOME}/logs" /logs \
    && mv "${NEO4J_HOME}/plugins" /plugins \
    && ln -s /data "${NEO4J_HOME}/data" \
    && ln -s /logs "${NEO4J_HOME}/logs" \
    && ln -s /plugins "${NEO4J_HOME}/plugins"

ENV PATH="${NEO4J_HOME}"/bin:${PATH} \
    CLASSPATH="${NEO4J_HOME}/plugins:${NEO4J_HOME}/plugins/*:${NEO4J_HOME}/lib:${NEO4J_HOME}/lib/*"

EXPOSE 7474 7473 7687

USER neo4j

# Check Neo4j can run
RUN ["java", "org.neo4j.server.CommunityEntryPoint", "--version"]

# Default config
ENV NEO4J_dbms_default__listen__address=0.0.0.0

ENTRYPOINT ["java", \
    "-Dfile.encoding=UTF-8", \
    "-XX:+UseG1GC", \
    "-XX:-OmitStackTraceInFastThrow", \
    "-XX:+AlwaysPreTouch", \
    "-XX:+UnlockExperimentalVMOptions", \
    "-XX:+TrustFinalNonStaticFields", \
    "-XX:+DisableExplicitGC", \
    "-XX:MaxInlineLevel=15", \
    "-Djdk.nio.maxCachedBufferSize=262144", \
    "-Dio.netty.tryReflectionSetAccessible=true", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-Djdk.tls.ephermeralDHKeySize=2048", \
    "-Djdk.tls.rejectClientInitiatedRenogotiation=true", \
    "-XX:FlightRecorderOptions=stackdepth=256", \
    "-XX:+UnlockDiagnosticVMOptions", \
    "-XX:+DebugNonSafepoints", \
    "io.sisu.neo4j.ContainerEntryPoint", \
    "--home-dir=/neo4j", \
    "--config-dir=/neo4j/conf"]

COPY ./build/libs/container-bootstrapper.jar "${NEO4J_HOME}/plugins"
