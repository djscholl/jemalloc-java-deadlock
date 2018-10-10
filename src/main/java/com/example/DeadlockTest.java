package com.example;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DeadlockTest {

  public static void main(final String[] args) throws InterruptedException {
    final int numThreads = Integer.parseInt(System.getProperty("numThreads", "1000"));
    final int numTasks = Integer.parseInt(System.getProperty("numTasks", "100000"));
    final int numIterations = Integer.parseInt(System.getProperty("numIterations", "1000"));
    final boolean waitOnDeadlock = Boolean.getBoolean("waitOnDeadlock");
    final String preload = System.getenv("LD_PRELOAD");

    System.out.println("Running test at " + new Date() + " with "
        + "\n\tnumThreads=" + numThreads
        + "\n\tnumTasks=" + numTasks
        + "\n\tnumIterations=" + numIterations
        + "\n\twaitOnDeadlock=" + waitOnDeadlock
        + "\n\tLD_PRELOAD=" + preload);

    final Map<String, LongAdder> completedTasksPerThread = new HashMap<>();
    IntStream.range(1, 1001).forEach(i -> completedTasksPerThread.put("pool-1-thread-" + i, new LongAdder()));

    final ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(numThreads);
    final AtomicInteger completedTasks = new AtomicInteger(0);
    IntStream.range(0, numTasks)
        .mapToObj(i -> threadPool.submit(() -> {
          final LongAdder myAdder = completedTasksPerThread.get(Thread.currentThread().getName());
          for (int j = 0; j < numIterations; j++) {
            try {
              MessageDigest.getInstance("SHA1");
              myAdder.increment();
            } catch (final NoSuchAlgorithmException e) {
              throw new RuntimeException(e);
            }
          }
          completedTasks.incrementAndGet();
        }))
        .collect(Collectors.toList());

    final long start = System.nanoTime();
    Map<String, Long> snapshot = null;
    while (true) {
      final int completedTasksNow = completedTasks.get();
      if (completedTasksNow == numTasks) {
        System.out.println("Test completed without any timeouts");
        threadPool.shutdown();
        // dumpTaskCounts(completedTasksPerThread);
        break;
      } else if (onlyOneThreadIsMakingProgress(completedTasksPerThread, snapshot)) {
        System.out.println("Timeout waiting for tasks to complete.");
        dumpThreads();
        dumpTaskCounts(completedTasksPerThread);
        if (!waitOnDeadlock) {
          System.exit(1);
        } else {
          break;
        }
      } else {
        System.out.println(completedTasksNow + " / " + numTasks + " tasks completed");
        snapshot = completedTasksPerThread.entrySet().stream()
            .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().longValue()));
        Thread.sleep(1000);
      }
    }
  }

  private static boolean onlyOneThreadIsMakingProgress(
      final Map<String, LongAdder> completedTasksPerThread,
      final Map<String, Long> snapshot) {
    if (snapshot == null) {
      return false;
    }
    final long threadsMakingProgress = completedTasksPerThread.keySet().stream()
        .filter(threadName -> !snapshot.get(threadName).equals(completedTasksPerThread.get(threadName).longValue()))
        .count();
    System.out.println(threadsMakingProgress + " threads made progress");
    return threadsMakingProgress <= 1;
  }

  private static void dumpTaskCounts(final Map<String, LongAdder> completedTasksPerThread) {
    completedTasksPerThread.forEach((threadName, taskCounter) -> {
      System.out.println(threadName + " completed " + taskCounter.longValue() + " calls");
    });

  }

  public static void dumpThreads() {
    for (final ThreadInfo ti : ManagementFactory.getThreadMXBean().dumpAllThreads(true, true)) {
      System.out.print(ti);
    }
  }

}


