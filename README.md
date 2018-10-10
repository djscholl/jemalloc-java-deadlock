# JEMalloc Java Deadlock test

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