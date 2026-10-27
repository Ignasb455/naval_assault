package gunmayhem.core.audio;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Shows that {@link AudioManager#getInstance()} is thread safe: several threads
 * are released at the same moment, all ask for the instance before it exists,
 * and every one of them gets the same object. Run with
 * {@code java -cp out gunmayhem.Main singleton}.
 */
public final class SingletonDemo {
    private static final int THREADS = 8;

    private SingletonDemo() { }

    public static void run() throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);
        Map<String, AudioManager> seen = new ConcurrentHashMap<>();

        for (int i = 0; i < THREADS; i++) {
            Thread t = new Thread(() -> {
                try {
                    start.await(); // every thread races for the first getInstance()
                    seen.put(Thread.currentThread().getName(), AudioManager.getInstance());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }, "thread-" + i);
            t.start();
        }
        start.countDown();
        done.await();

        System.out.println("thread       instance address");
        seen.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> System.out.printf("%-12s @%x%n", e.getKey(),
                System.identityHashCode(e.getValue())));
        long distinct = seen.values().stream().distinct().count();
        System.out.println("distinct instances: " + distinct
            + (distinct == 1 ? "  (thread safe)" : "  <-- more than one instance!"));
    }
}
