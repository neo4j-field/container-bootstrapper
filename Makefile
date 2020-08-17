.PHONY: build test

build: jar
	@docker build -t neo4j-docker:latest -t neo4j-docker:4.1 -f Dockerfile .
	@docker build -t neo4j-docker:enterprise -t neo4j-docker:4.1-enterprise -f Dockerfile.enterprise .

jar: build/libs/container-bootstrapper.jar
	./gradlew jar

test: build
	@exec docker run --rm -it --read-only \
		-p 7474:7474/tcp -p 7687/tcp \
		neo4j-docker:latest
