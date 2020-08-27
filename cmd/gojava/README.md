# gojava!

Started as a simple wrapper to `execve` syscall, but now bloated into
the realm of dealing with memory detection/recommendation. *le sigh*

Can probably be cleaned up a bit to just do the memrec directly and
skip the subprocess crap and parsing. Would simplify the code and make
it more understandable. The memrec logic isn't that complicated, and
in container-land should be far simpler without having to buffer 20%
for OS overhead.

## JVM Settings
These get pulled from the environment and set in the arg-list in the
exec call to Java:

- `JVM_INITIAL_HEAP`
- `JVM_MAXIMUM_HEAP`

It also looks for `NEO4J_EDITION`. If it's equal to `enterprise`, do
memrec stuff just like the original neo4j bootstrapping shell script
does.

> This is the logic subject to change, btw.

Otherwise, `gojava` acts as a simple wrapper to calling `java`.
