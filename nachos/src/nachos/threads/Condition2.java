package nachos.threads;

import nachos.machine.*;

import java.util.LinkedList;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param conditionLock the lock associated with this condition
     *                      variable. The current thread must hold this
     *                      lock whenever it uses <tt>sleep()</tt>,
     *                      <tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
        this.conditionLock = conditionLock;
        waitQueue = new LinkedList<>();
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        waitQueue.add(KThread.currentThread()); // block list of threads 

        boolean intStatus = Machine.interrupt().disable();
        conditionLock.release();
        KThread.sleep(); // block the thread
        conditionLock.acquire();
        Machine.interrupt().restore(intStatus);
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        boolean intStatus = Machine.interrupt().disable();
        if (!waitQueue.isEmpty()) waitQueue.removeFirst().ready(); // pass a blocked thread to ready queue
        Machine.interrupt().restore(intStatus);
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        while (!waitQueue.isEmpty()) wake();
    }

    public static void selfTest() {
        System.out.println("\n--------------Testing Condition2 initiated------------------\n");

        Lock lock = new Lock();
        Condition2 con = new Condition2(lock);

        KThread sleep = new KThread(() -> {
            lock.acquire();
            System.out.println("Test 1:\n...Going to sleep.....\n");
            con.sleep();
            System.out.println("Test 2 Complete: Woke up!\n");
            lock.release();
        });
        sleep.fork();

        new KThread(() -> {
            lock.acquire();
            System.out.println("Test 2:\n...Waking a thread...\n");
            con.wake();
            lock.release();
        }).fork();

        sleep.join();

        System.out.println("\nTest 3: sleep AND wakeAll\n");
        KThread sleep1 = new KThread(() -> {
            lock.acquire();
            System.out.println("...Sleep1 going to sleep...");
            con.sleep();
            System.out.println("Sleep1 waking up!");
            lock.release();
        });
        sleep1.fork();

        KThread sleep2 = new KThread(() -> {
            lock.acquire();
            System.out.println("...Sleep2 going to sleep...");
            con.sleep();
            System.out.println("Sleep2 waking up!");
            lock.release();
        });
        sleep2.fork();

        new KThread(() -> {
            lock.acquire();
            System.out.println("\n...Waking all sleeping threads...\n");
            con.wakeAll();
            lock.release();
        }).fork();

        sleep1.join();
        sleep2.join();

        System.out.println("\nTest 3 Complete: Everyone is awake!");
        System.out.println("\n--------------Testing Condition2 finished------------------\n");
    }

    private final Lock conditionLock;
    private final LinkedList<KThread> waitQueue; // linked list of KThreads
}
