# JEMalloc Java Deadlock test

This test code was created to reprocuce a JVM bug when running with Jemalloc 5.1+ that results in a thread permanently hodling onto an object monitor even though the code has exited the synchronized block. Defects have been filed with both [jemalloc](https://github.com/jemalloc/jemalloc/issues/1392) and the [JVM](https://bugs.openjdk.java.net/browse/JDK-8215355).

### How to build
`mvn package`

### How to run
```
export LD_PRELOAD=/path/to/jemalloc.so
for i in $(seq -w 200); do 
	echo "Test ${i}/200"
	java -cp jemalloc-java-deadlock-0.0.1-SNAPSHOT.jar -DnumTasks=50000 com.example.DeadlockTest 2>&1 > ${i}.log || break
done
```

The loop will exit if a deadlock was detected.  Check the numbered log file for the last test to run.
