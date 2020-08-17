## Use a simple Alpine container just to fetch and stage the Neo4j files
FROM alpine:3.12 AS downloader
RUN apk update && apk add openjdk11-jre-headless openssl

ARG NEO4J_SHA256=4f663a520bec40dfd0b1972feb3cf93af321c230b448adb6dc917717e67a1271
ARG NEO4J_TARBALL=neo4j-community-4.1.1-unix.tar.gz
ARG NEO4J_DOWNLOAD_URI=https://dist.neo4j.org
ARG NEO4J_HOME="/neo4j"

RUN mkdir /data /logs /plugins
RUN wget -q -P /tmp "${NEO4J_DOWNLOAD_URI}/${NEO4J_TARBALL}" \
    && echo "${NEO4J_SHA256}  /tmp/${NEO4J_TARBALL}" | sha256sum -csw \
    && tar x -z -f "/tmp/${NEO4J_TARBALL}" -C /tmp \
    && rm "/tmp/${NEO4J_TARBALL}" \
    && mv /tmp/neo4j-* "${NEO4J_HOME}" \
    && mkdir "${NEO4J_HOME}/lib-override" \
    && rm -r "${NEO4J_HOME}/data" "${NEO4J_HOME}/logs" "${NEO4J_HOME}/plugins" \
    && ln -s /data "${NEO4J_HOME}/data" \
    && ln -s /logs "${NEO4J_HOME}/logs" \
    && ln -s /plugins "${NEO4J_HOME}/plugins" \
    && mkdir /metrics && ln -s /metrics "${NEO4J_HOME}/metrics"


####################################################################################
## Use Google's "distroless" OpenJDK image...less bloat, more awesome!
FROM gcr.io/distroless/java-debian10:11
ARG NEO4J_HOME="/neo4j"
ARG NONROOT=nonroot
COPY --from=downloader "${NEO4J_HOME}" "${NEO4J_HOME}"
# Since this container is shell-less, we use COPY with --chown to make directories
COPY --from=downloader --chown=${NONROOT} /data /data
COPY --from=downloader --chown=${NONROOT} /logs /logs
COPY --from=downloader --chown=${NONROOT} /metrics /metrics
COPY --from=downloader /plugins /plugins

# /tmp is required at the moment to support --read-only thanks to JNA stuff
VOLUME ["/data", "/logs", "/plugins", "/tmp"]

ENV CLASSPATH="${NEO4J_HOME}/lib-override:${NEO4J_HOME}/lib-override/*:${NEO4J_HOME}/plugins:${NEO4J_HOME}/plugins/*:${NEO4J_HOME}/lib:${NEO4J_HOME}/lib/*"
# Check Neo4j can run
RUN ["java", "--dry-run", "org.neo4j.server.CommunityEntryPoint"]

EXPOSE 7474 7473 7687
USER nonroot

# Default config
ENV NEO4J_dbms_default__listen__address=0.0.0.0

# All the esoteric settings from the original neo4j shell can go here
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
    "-XX:+DebugNonSafepoints"]
CMD ["io.sisu.neo4j.server.CommunityContainerEntryPoint", \
        "--home-dir=/neo4j", "--config-dir=/neo4j/conf"]

COPY ./build/libs/container-bootstrapper.jar "${NEO4J_HOME}/lib-override/"
