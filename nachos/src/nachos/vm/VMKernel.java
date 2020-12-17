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
        System.out.println("\n\n______________STATISTICS_____________");
        System.out.println("\nTotal TLB hits: " + MMU.AssociativeMemoryManager.tlbHits);
        System.out.println("Total TLB miss: " + MMU.AssociativeMemoryManager.tlbMiss);
        System.out.println("\nTotal Page Fault: " + MMU.PageTableManager.pageFaultCount);
        for(int ppn = 0; ppn < MMU.PageTableManager.faultCountPerPPN.length; ++ppn) {
            System.out.println("PPN: " + ppn + " witnessed " + (int)MMU.PageTableManager.faultCountPerPPN[ppn]
                    + " page faults. " + "\tfault rate: " +
                    (MMU.PageTableManager.faultCountPerPPN[ppn] * 100.0) / MMU.PageTableManager.pageFaultCount + "%");
        }
        System.out.println("-----------------------------------");

        mmu.clearSwapArea();
        super.terminate();

    }

    protected static MMU mmu;

    // dummy variables to make javac smarter
    private static VMProcess dummy1 = null;
    private static final char dbgVM = 'v';
}
