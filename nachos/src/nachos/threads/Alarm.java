package nachos.threads;

import nachos.machine.*;
import java.util.TreeSet;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
        pending = new TreeSet<>();
        Machine.timer().setInterruptHandler(() -> {
            boolean origState = Machine.interrupt().disable();
            if(pending.size() > 0 && pending.first().time <= Machine.timer().getTime()) {
                PendingAlarm pendingAlarm = pending.first();
                pendingAlarm.getHandler().ready();
                pending.remove(pendingAlarm);
            }
            Machine.interrupt().restore(origState);
            timerInterrupt(); // aside this extra code is added here
        });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
        KThread.currentThread().yield();
    }


    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param x the minimum number of clock ticks to wait.
     * @see nachos.machine.Timer#getTime()
     */

    public void waitUntil(long x) {
        KThread waitingThread = KThread.currentThread();
        boolean origState = Machine.interrupt().disable();
        schedule(x, waitingThread); // pass to scheduler method
        KThread.sleep(); // and make it sleep / blocked
        Machine.interrupt().restore(origState);
    }

    /**
     * <h1>Part1 :: Task3</h1>
     *  Implementation is similar to {@link nachos.machine.Interrupt}
     */
    // FIXME : suspend the current thread with appropriate waking time
    private void schedule(long when, KThread handler) {
        Lib.assertTrue(when > 0);
        long time = Machine.timer().getTime() + when;
        Alarm.PendingAlarm toOccur = new Alarm.PendingAlarm(time, handler);
        pending.add(toOccur);
    }

    // no extra KThread is created in this class, just reference of various threads
    // those threads called waitUntil(some_time)
    private class PendingAlarm implements Comparable<PendingAlarm> {
        PendingAlarm(long time, KThread handler) {
            this.time = time;
            this.handler = handler;
            this.id = numPendingAlarmsCreated++;
        }

        public int compareTo(PendingAlarm toOccur) {
            // can't return 0 for unequal objects, so check all fields
            if (time < toOccur.time)
                return -1;
            else if (time > toOccur.time)
                return 1;
            else return Long.compare(id, toOccur.id);
        }

        long time;
        KThread handler;
        private final long id;

        public KThread getHandler() {
            return handler;
        }
    }

    private static class PingTest implements Runnable {
        PingTest(int which, long duration) {
            this.which = which;
            this.duration = duration;
        }

        public void run() {
            long start = Machine.timer().getTime();
            boolean origState = Machine.interrupt().disable();
            ThreadedKernel.alarm.waitUntil(duration);
            Machine.interrupt().restore(origState);
            long end = Machine.timer().getTime();
            System.out.println("Alarm test" + which
                    + " Start: " + start
                    + " End: " + Machine.timer().getTime()
                    + " Difference: " + (end - start));
        }

        private final int which;
        private final long duration;
    }

    /**
     * Tests whether this module is working.
     */
    public static void selfTest() {
        System.out.println("===Alarm Test===");
        KThread thread1 = new KThread(new Alarm.PingTest(1, 500)).setName("alarm thread");
        KThread thread2 = new KThread(new Alarm.PingTest(2, 200)).setName("alarm thread");
        KThread thread3 = new KThread(new Alarm.PingTest(3, 1000)).setName("alarm thread");
        KThread thread4 = new KThread(new Alarm.PingTest(4, 100)).setName("alarm thread");
        thread1.fork();
        thread2.fork();
        thread3.fork();
        thread4.fork();
        thread1.join();
        thread2.join();
        thread3.join();
        thread4.join();
    }

    int numPendingAlarmsCreated = 0;    // assign waiting threads a unique ID
    private final TreeSet<Alarm.PendingAlarm> pending; // contains all threads (currently blocked)

}
