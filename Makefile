.PHONY: build test

build: jar
	@docker build -t neo4j-docker:latest .

jar: build/libs/container-bootstrapper.jar
	./gradlew jar

test: build
	@exec docker run --rm -it --read-only \
		-p 7474:7474/tcp -p 7687/tcp \
		neo4j-docker:latest
