SHELL = /bin/sh
DIR := $(shell pwd)
VERSION = $(shell ./gradlew -q printVersion)
.PHONY: build test test-ee jars

build: jars
	@docker build --build-arg=VERSION=${VERSION} -t neo4j-docker:latest -t neo4j-docker:4.1 -f Dockerfile .
	@docker build --build-arg=VERSION=${VERSION} -t neo4j-docker:enterprise -t neo4j-docker:4.1-enterprise -f Dockerfile.enterprise .

jars:
	@echo Building subprojects with version=${VERSION}
	@./gradlew dist

test: build
	@exec docker run --rm -it --read-only \
		-p 7473:7473/tcp -p 7687:7687/tcp \
		--mount type=bind,source=${DIR}/bootstrapper/src/test/resources,destination=/certs \
		-e NEO4J_dbms_connector_https_enabled=true \
		-e NEO4J_dbms_connector_https_listen__address=:7473 \
		-e NEO4J_dbms_container_ssl_policy_https_enabled=true \
		-e NEO4J_dbms_container_ssl_policy_https_base__directory=/certs \
		-e NEO4J_dbms_container_ssl_policy_https_private__key=file:/certs/key.pem \
		-e NEO4J_dbms_container_ssl_policy_https_public__certificate=file:/certs/cert.pem \
		-e NEO4J_dbms_container_ssl_policy_https_client__auth=NONE \
		neo4j-docker:latest

test-ee: build
	@exec docker run --rm -it --read-only \
		-p 7473:7473/tcp -p 7687:7687/tcp \
		--mount type=bind,source=${DIR}/bootstrapper/src/test/resources,destination=/certs \
		-e NEO4J_ACCEPT_LICENSE_AGREEMENT=${NEO4J_ACCEPT_LICENSE_AGREEMENT} \
		-e NEO4J_dbms_connector_https_enabled=true \
		-e NEO4J_dbms_connector_https_listen__address=:7473 \
		-e NEO4J_dbms_container_ssl_policy_https_enabled=true \
		-e NEO4J_dbms_container_ssl_policy_https_base__directory=/certs \
		-e NEO4J_dbms_container_ssl_policy_https_private__key=file:/certs/key.pem \
		-e NEO4J_dbms_container_ssl_policy_https_public__certificate=file:/certs/cert.pem \
		-e NEO4J_dbms_container_ssl_policy_https_client__auth=NONE \
		neo4j-docker:enterprise
