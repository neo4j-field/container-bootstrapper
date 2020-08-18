package main

import (
	"os"
	"os/exec"
	"strings"
	"syscall"
)

func main() {
	bin, lookErr := exec.LookPath("java")
	if lookErr != nil {
		panic(lookErr)
	}

	// Java expects the first arg to be "java" or the like...it technically
	// doesn't matter, but let's keep this looking "normal" just in case
	args := []string{"java"}

	// Search for some extra JVM settings that we want to toggle at runtime
	for _, arg := range os.Environ() {
		val := strings.SplitN(arg, "=", 2)
		if len(val) != 2 {
			continue
		}

		switch val[0] {
		case "JVM_INITIAL_HEAP":
			args = append(args, "-Xms"+val[1])
		case "JVM_MAXIMUM_HEAP":
			args = append(args, "-Xmx"+val[1])
		default:
			continue
		}
	}
	args = append(args, os.Args[1:]...)

	execErr := syscall.Exec(bin, args, os.Environ())
	if execErr != nil {
		panic(execErr)
	}
}
