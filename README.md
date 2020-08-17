# contain(or die tryin')

Neo4j kinda sucks in a cloud-centric world and the first step is
making it suck-less in a Linux container world.

TL;DR: `make test`

## Problem 1: The official Docker images are bloated
Having all sorts of pomp and circumstance in shell scripts calling
shell scripts calling Java makes for a very bad time:

* container images bloat with a LOT of unneeded binaries and libraries
  - bloated images wasting disk space
  - far too much userland bloat providing attack surface
* lots of sub/child-process crap requiring tini (or Docker --init)
  - matryoshka doll of complexity makes it hard to reason about
  - too much implicit magic (e.g. NEO4JLABS_PLUGINS) that scares me

### Experiment 1.1: Use Google's "distroless" Java image as base
The official Neo4j Dockerfile builds off the openjdk base images,
which are built off Debian. They ship with a full (albeit minimal)
Debian userland:

```bash
root@45f9c3baa7fc:/# find /bin /sbin /usr/bin /usr/sbin | wc -l
396
```

```bash
root@45f9c3baa7fc:/# find /lib /usr/lib | wc -l
1254
```

As Gwen Stefani says: [this is bananas](https://youtu.be/Kgjkth6BRRY?t=150)

My original idea was to base the new image off Alpine with its
simplified musl-based busybox userland. But still...why do we need a
shell?

Google has apparently lead the charge in the idea of "distroless"
container images, which aren't always the easiest to build, especially
for Java apps since the JRE is a pretty large fish to fry. The best
way to look at the idea of "distroless" is basically "FROM SCRATCH"
but for the more challenging JIT-based languages like Java, Ruby, and
Python.

Luckily, they provide a prebuild Java 11 image:

  https://github.com/GoogleContainerTools/distroless/tree/master/java

The resulting [Dockerfile](./Dockerfile) must use a 2-stage approach,
first staging the Neo4j files in an Alpine image and then COPY'ing
them into the Google "distroless" Java image. This provides:

1. A very minimal container surface, basically just the JDK and
   required GNU libc dynamic libs + OpenSSL
2. A non-root user called `nonroot` we can use instead of `neo4j`

### Experiment 1.2: Call Java directly!
Since our base image now no longer contains typical Debian userland
cruft (like a shell), we need to call `java` directly!

## Problem 2: Neo4j itself relies too heavily on local java.io.File's
Want to configure TLS for securing Bolt or HTTPS? You need a private
key...ok, so what? Well, Neo4j's `SslPolicyLoader` uses
`java.nio.file.Path` references to load the private key file [1][1].

"But, can't you use something like _Docker Secrets Manager [2][2] or
Kubernetes Secrets?_ [3][3]

Yes...you can. And that might work just great. Plus services like GKE
now integrate with things like Google's KMS to make sure the secret is
stored encrypted in `etcd` [4][4].

BUT! This means **you still need the private key file somewhere on
runtime filesystem of the container!** (Not sure about you, but I'd
take my chances with forcing an attacker to scan memory for a key
vs. plucking it from a filesystem if they got a shell.)

A lot of CSPs now provide managed secrets services and all provide
some form of object storage (S3, GCS, etc.) Some organizations use
HashiCorp Vault.

To make Neo4j even more cloud-friendly, supporting pluggable secrets
services gives downline users more choice and flexibility.

### Experiment 2.1: Support defining files (certs, keys, etc.) as URIs
Simple as that...instead of a local filesystem path, parse a uri like
`file:///neo4j/certificates/private.key` or more interestingly
`gs:///my_bucket/some_file.txt`.

Why uri's? We can map the scheme to the backing client for retrieving
the object.

> This part is currently _a work in progress_...see the `CloudUri` class
> I'm toying with.

### Experiment 2.2: Hot-wire in a better SslPolicyLoader
There's so much nastiness going on in the bootstrapping code that it's
looking to be far far easier to just preload a custom/hacked
SslPolicyLoader class. I'm going to do that to save on copy-pasta.

For now, I'm implemented this by:

1. Writing an new `org.neo4j.ssl.config.SslPolicyLoader`
2. Creating a `${NEO4J_HOME}/lib-override` directory in the image
3. Adding `${NEO4J_HOME}/lib-override` before `/lib` in the classpath

Sometimes Java is fun.

## Problem 3: Further hardening of Neo4j is HARD
Because of Problem 1 and 2, hardening Neo4j is much more difficult
than it could be. Linux containers support a tremendous amount of
knobs for hardening based on namespaces and seccomp sandboxing these
days.

But, when you're container is basically the equivalent of a VM, it's
going to be tough to tighten things tup.

### Experiment 3.1: TBD

## Additional Reading
* NCC Group's whitepaper is a great tome, even if from 2016!
  - _"Understanding and Hardening Linux Containers"_, June 29, 2016
  - Short link: https://bit.ly/3angTyy (See [5][5] if it doesn't work.)
* Containers, Security, and Echo Chambers (Jess Frazelle, 20 May 2018)
  - https://blog.jessfraz.com/post/containers-security-and-echo-chambers/
* Running a JVM in a Container without Getting Killed (v2)
  - https://blog.csanchez.org/2018/06/21/running-a-jvm-in-a-container-without-getting-killed-ii/
* Distroless Docker: Containerizing Apps, not VMs - Matthew Moore
  - https://www.youtube.com/watch?v=lviLZFciDv4

### footnotes
(these might to render on github.com)

[1]: https://github.com/neo4j/neo4j/blob/6d961e5e638e48e91ea58a603f76f2429e569e1d/community/ssl/src/main/java/org/neo4j/ssl/PkiUtils.java#L87

[2]: https://docs.docker.com/engine/swarm/secrets/

[3]: https://kubernetes.io/docs/concepts/configuration/secret/

[4]: https://cloud.google.com/kubernetes-engine/docs/how-to/encrypting-secrets

[5]: https://www.nccgroup.trust/globalassets/our-research/us/whitepapers/2016/april/ncc_group_understanding_hardening_linux_containers-1-1.pdf
