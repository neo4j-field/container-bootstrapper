.PHONY: build test test-ee

build: jar
	@docker build -t neo4j-docker:latest -t neo4j-docker:4.1 -f Dockerfile .
	@docker build -t neo4j-docker:enterprise -t neo4j-docker:4.1-enterprise -f Dockerfile.enterprise .

jar: build/container-bootstrapper/container-bootstrapper.jar

build/container-bootstrapper/container-bootstrapper.jar:
	@./gradlew unpack

test: build
	@exec docker run --rm -it --read-only \
		-p 7474:7474/tcp -p 7687:7687/tcp \
		neo4j-docker:latest

test-ee: build
	@exec docker run --rm -it --read-only \
		-e NEO4J_ACCEPT_LICENSE_AGREEMENT=yes \
		-p 7474:7474/tcp -p 7687:7687/tcp \
		neo4j-docker:enterprise
