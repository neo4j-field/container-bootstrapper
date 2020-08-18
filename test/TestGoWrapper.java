package test;

import java.lang.management.*;

/**
 * Test program to validating the gojava wrapper.
 */
class TestGoWrapper {
	public static void main(String[] argv) throws Exception {
		System.out.println("heap settings:");
		MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
		System.out.println(mbean.getHeapMemoryUsage().toString());

		System.out.println("\nargv:");
		for (int i=0; i<argv.length; i++) {
			System.out.println(String.format("[%d] %s", i, argv[i]));
		}

		System.out.println("\nenvironment");
		System.getenv().forEach((k, v) -> System.out.println("k: " + k + ", v: " + v));
	}
}
