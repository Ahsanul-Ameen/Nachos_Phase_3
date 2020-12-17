package nachos.threads;

import nachos.machine.Lib;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
        lock = new Lock();
        speakerPresent = listenerPresent = false;
        waitingSpeaker = new Condition2(lock);
        waitingListener = new Condition2(lock);
        waitingForListener = new Condition2(lock);
        waitingForSpeaker = new Condition2(lock);
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param word the integer to transfer.
     */
    public void speak(int word) {
        lock.acquire();
        if (speakerPresent) waitingSpeaker.sleep();
        Lib.assertTrue(!speakerPresent);
        speakerPresent = true;

        if (!listenerPresent) waitingForListener.sleep();
        System.out.println(KThread.currentThread().getName() + " " + word);
        this.word = word;
        waitingForSpeaker.wake();
        waitingForListener.sleep();

        speakerPresent = false;
        waitingSpeaker.wake();
        lock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return the integer transferred.
     */
    public int listen() {
        lock.acquire();
        if (listenerPresent) waitingListener.sleep();
        Lib.assertTrue(!listenerPresent);
        listenerPresent = true;

        waitingForListener.wake();
        waitingForSpeaker.sleep();
        int ret = this.word;
        System.out.println(KThread.currentThread().getName() + " " + ret);
        waitingForListener.wake();

        listenerPresent = false;
        waitingListener.wake();
        lock.release();
        return ret;
    }

    public static void selfTest() {
        System.out.println("\n--------------Testing Communicator initiated------------------\n");

        Communicator communicator = new Communicator();

        new KThread(() -> communicator.speak(1)).setName("speak1").fork();
        new KThread(() -> communicator.speak(2)).setName("speak2").fork();
        new KThread(communicator::listen).setName("listen1").fork();
        new KThread(() -> communicator.speak(3)).setName("speak3").fork();
        new KThread(communicator::listen).setName("listen2").fork();
        new KThread(communicator::listen).setName("listen3").fork();
        new KThread(communicator::listen).setName("listen4").fork();
        KThread listen5 = new KThread(communicator::listen).setName("listen5");
        listen5.fork();
        new KThread(() -> communicator.speak(4)).setName("speak4").fork();
        KThread speak5 = new KThread(() -> communicator.speak(5)).setName("speak5");
        speak5.fork();

        speak5.join();
        listen5.join();
        System.out.println("\n--------------Testing Communicator finished------------------\n");
    }

    private int word;
    private final Lock lock;
    private boolean speakerPresent, listenerPresent;
    private final Condition2 waitingSpeaker, waitingListener, waitingForListener, waitingForSpeaker;
    // waitingSpeaker/Listener -> prevents multiple Speaker/Listener entering the Communicator zone
    // watingForListener/Speaker -> ensures speak -> listen synchronization, also balance the absence of any two
}
