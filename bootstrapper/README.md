# bootstrapper
This is the main module for making Neo4j container-friendly.

It contains three key pieces:

## 1. Container-friendly Entrypoint
New entry points are provided that make it easier to start up the database:

- [CommunityContainerEntryPoint](./src/main/java/io/sisu/neo4j/server/CommunityContainerEntryPoint.java)
- [EnterpriseContainerEntryPoint](./src/main/java/io/sisu/neo4j/server/EnterpriseContainerEntryPoint.java)

They both behave similarity in calling into new container-friendly Bootstrapper classes, however the Enterprise flavor now does its own check for the magic `NEO4J_ACCEPT_LICENSE_AGREEMENT` environment variable.

The bootstrappers mimic the original ones, however the Enterprise bootstrapper uses reflection to properly act like the one shipping with Neo4j EE as that code isn't accessible publicly.

Both bootstrappers ultimately trigger some net-new logic (and copy-pasta) in the [ContainerBootstrapper](./src/main/java/io/sisu/neo4j/server/ContainerBootstrapper.java) class.

Specifically, this new logic mimics the magic configuration available via the official Neo4j Docker images that takes environment vars starting with "NEO4J_" and dynamically updates the `neo4j.conf` file before startup. The only exception being that in this case we do not mimic the fairydust that is `NEO4JLABS_PLUGINS`...that's a no-no. 

> You want plugins? Plug them in yourself. 😝

## 2. Overridden SslPolicyLoader framework
We need to hijack the SslPolicyLoader so it will play nice with the next part. Thankfully, Java classloaders are easily manipulated and we'll just provide a new `org.neo4j.ssl.config.SslPolicyLoader` implementation that does our bidding.

What is that bidding, you ask? Well, it's loading key ssl assets (primary key, x.509 public certificate, etc.) from some places other than the local filesystem.

## 3. AssetProvider SPI (service provider interface)
New hotness:
```properties
dbms.container.ssl.policy.https.private_key=gs://mah-bucket/mykey.pem
dbms.container.ssl.policy.https.public_certificate=gs://mah-bucket/mycert.pem
```

Old and busted:
```properties
dbms.ssl.policy.https.private_key=some-local-file.pem
dbms.ssl.policy.https.public_certificate=some-other-local-file.pem
```

> Yes, putting private keys in GCS/S3/etc. is a recipe for pain, but this is an example to show functionality. Replace Google Cloud Storage with a secrets manager and voila.



