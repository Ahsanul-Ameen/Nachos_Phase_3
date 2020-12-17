package nachos.vm;

import nachos.userprog.UserKernel;
import nachos.vm.Utils.MMU;


/**
 * FIXME : just change the MMU part
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
    /**
     * Allocate a new VM kernel.
     */
    public VMKernel() {
        super();
        mmu = new MMU();
    }

    /**
     * Initialize this kernel.
     */
    public void initialize(String[] args) {
        super.initialize(args);
        mmu.init();
    }

    /**
     * Test this kernel.
     */
    public void selfTest() {
        super.selfTest();
    }

    /**
     * Start running user programs.
     */
    public void run() {
        super.run();
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
        System.out.println("\n\nTotal TLB hits : " + MMU.AssociativeMemoryManager.tlbHits);
        mmu.clearSwapArea();
        super.terminate();
    }

    protected static MMU mmu;

    // dummy variables to make javac smarter
    private static VMProcess dummy1 = null;
    private static final char dbgVM = 'v';
}
