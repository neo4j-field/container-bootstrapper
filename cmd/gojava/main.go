package main

import (
	"bufio"
	"log"
	"os"
	"os/exec"
	"regexp"
	"strconv"
	"strings"
	"syscall"
)

type memoryConfig struct {
	heapInit, heapMax, pageCache string
}

// Checks if we've got a set memory limit via a memory cgroup. The way this
// works is fun: if there's no memory cgroup, the kernel returns basically
// max (uint64-1)-[page size]...so typically (1<<64) - 1 - 4096. We assume
// the chance of someone have 1<<62 bytes of physical memory to be very low
// and use that as a threshold for detection.
func cgroup_memlimit() (bool, uint64) {
	file, err := os.Open("/sys/fs/cgroup/memory/memory.limit_in_bytes")
	if err != nil {
		log.Fatal(err)
	}
	defer file.Close()

	reader := bufio.NewReader(file)
	str, err := reader.ReadString('\n')
	if err != nil {
		log.Fatal(err)
	}
	str = strings.Trim(str, "\n")

	val, err := strconv.ParseUint(str, 10, 64)
	if err != nil {
		log.Fatal(err)
	}

	// memory.limit_in_bytes will spit out max uint64 - size of 1 page...
	// so we just play it safe and assume no machine has anywhere near that
	if val > uint64(1<<62) {
		// in the words of Cady Heron: "the limit does not exist!"
		return false, 0
	}
	return true, val
}

// Given an optional limit value, call out to the memrec tool that ships with
// Neo4j. If we have a known limit, we explicity pass it as the --memory arg
// since the memrec tool is ignorant of cgroups (*le sigh*)
func memrec(limit ...uint64) memoryConfig {
	var config memoryConfig
	args := []string{"org.neo4j.cli.AdminTool", "memrec"}

	if len(limit) > 0 {
		args = append(args, "--memory="+strconv.FormatUint(limit[0], 10))
	}

	log.Printf("calling admin tool: java %v\n", args)
	cmd := exec.Command("java", args...)
	cmd.Env = os.Environ()

	out, err := cmd.Output()
	if err != nil {
		log.Printf("call to AdminTool failed: %v\n", err)
		log.Fatal(cmd)
	}

	heapInitRe := regexp.MustCompile("dbms\\.memory\\.heap\\.initial_size=([\\d\\w]+)")
	heapMaxRe := regexp.MustCompile("dbms\\.memory\\.heap\\.max_size=([\\d\\w]+)")
	pagecacheRe := regexp.MustCompile("dbms\\.memory\\.pagecache\\.size=([\\d\\w]+)")

	// TODO: error handling :-(
	config.heapInit = heapInitRe.FindStringSubmatch(string(out))[1]
	config.heapMax = heapMaxRe.FindStringSubmatch(string(out))[1]
	config.pageCache = pagecacheRe.FindStringSubmatch(string(out))[1]

	log.Printf("generated memory recommendation: %v\n", config)
	return config
}

// If we need to recommend memory settings, do so intelligently and cgroup aware
func recommendMemory(pageCacheOverride string) memoryConfig {
	// We need to help the memory recommender as it's ignorant of cgroups
	hasCgroup, limit := cgroup_memlimit()

	var memory memoryConfig
	// Regardless of cgroups, we still check for recommended settings in
	// case the user didn't provide any
	if hasCgroup {
		log.Printf("detected cgroup memory limit of %d bytes\n", limit)
		memory = memrec(limit)
	} else {
		log.Println("no cgroup memory limits detected")
		memory = memrec()
	}

	// If set by the user, obey and ignore recommendations
	if pageCacheOverride != "" {
		memory.pageCache = pageCacheOverride
	}

	return memory
}

func main() {
	// Our default memory setup to be overriden
	memory := memoryConfig{heapInit: "", heapMax: "", pageCache: "512M"}

	// No java in the path? Abort early.
	bin, lookErr := exec.LookPath("java")
	if lookErr != nil {
		log.Fatal("could not find 'java' executable in PATH")
	}

	// Java expects the first arg to be "java" or the like...it technically
	// doesn't matter, but let's keep this looking "normal" just in case
	args := []string{"java"}

	// We need to see if the user set a pagecache size or if we fall back to
	// our recommended/detected size
	pageCacheOverride := ""
	enterprise := false

	// Sort of annoying approach, but our original Docker image sets this...
	// only care and try to get a memrec if we're using 'enterprise' edition

	// Search for some extra Memory settings that we want to toggle at runtime
	for _, arg := range os.Environ() {
		val := strings.SplitN(arg, "=", 2)
		if len(val) != 2 {
			continue
		}

		switch val[0] {
		case "JVM_INITIAL_HEAP":
			memory.heapInit = val[1]
		case "JVM_MAXIMUM_HEAP":
			memory.heapMax = val[1]
		case "NEO4J_EDITION":
			if val[1] == "enterprise" {
				enterprise = true
			}
		case "NEO4J_dbms_memory_pagecache_size":
			pageCacheOverride = val[1]
		default:
			continue
		}
	}

	// We only recommend if Enterprise and user hasn't set things
	if enterprise && memory.heapMax == "" {
		log.Println("generating memory recommendations...")
		recommendation := recommendMemory(pageCacheOverride)
		memory = recommendation
	}

	// JVM heap is set via cli args to the runtime
	if memory.heapInit != "" {
		args = append(args, "-Xms"+memory.heapInit)
	}
	if memory.heapMax != "" {
		args = append(args, "-Xmx"+memory.heapMax)
	}
	args = append(args, os.Args[1:]...)

	// If PageCache is set as an environment property already, don't set it again
	env := os.Environ()
	if pageCacheOverride == "" {
		env = append(env, "NEO4J_dbms_memory_pagecache_size="+memory.pageCache)
	}

	// execve(2) this baby into our Neo4j java process
	log.Println("Launching java...")
	execErr := syscall.Exec(bin, args, env)
	if execErr != nil {
		panic(execErr)
	}
}
