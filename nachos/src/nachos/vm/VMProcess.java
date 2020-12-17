package nachos.vm;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.Processor;
import nachos.machine.TranslationEntry;
import nachos.userprog.UserProcess;
import nachos.vm.Utils.Loader;
import nachos.vm.Utils.SwapSpace;

// FIXME : This class seems okay

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
    /**
     * Allocate a new process.
     */
    public VMProcess() {
        super();
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
        // clear the TLB & writeBack all dirty pages
        VMKernel.mmu.getAssociativeMemoryManager().clearTLB(pid);
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
        //super.restoreState();
    }

    /**
     * Initializes page tables for this process so that the executable can be
     * demand-paged.
     *
     * @return    <tt>true</tt> if successful.
     */
    @Override
    protected boolean loadSections() {
        loader = new Loader(coff);
        return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
        VMKernel.mmu.getAssociativeMemoryManager().clearTLB(pid);
        VMKernel.mmu.getPageTableManager().clearPages(pid); //fixme
        coff.close();
    }

    public static VMProcess newUserProcess() {
        return (VMProcess) Lib.constructObject(Machine.getProcessClassName());
    }

    @Override
    public String readVirtualMemoryString(int virtualAddr, int maxLength) {
        Lib.assertTrue(maxLength >= 0, "Invalid length of reader string!");

        byte[] bytes_ = new byte[maxLength + 1];

        int bytesRead = readVirtualMemory(virtualAddr, bytes_);

        for (int length = 0; length < bytesRead; length++) {
            if (bytes_[length] == 0)
                return new String(bytes_, 0, length);
        }

        return null;
    }

    @Override
    public int readVirtualMemory(int virtualAddr, byte[] data) {
        return readVirtualMemory(virtualAddr, data, 0, data.length);
    }

    @Override
    public int readVirtualMemory(int virtualAddr, byte[] data, int offset, int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);

        byte[] memory = Machine.processor().getMemory();


        // for i in range vpn_of(virtualAddr) : vpn_of(virtualAddr + length)
        int amount = 0;
        //traverse through the pages of memory
        for (int i = Processor.pageFromAddress(virtualAddr), physicalAddr; i <= Processor.pageFromAddress(virtualAddr + length - 1); ++i) {
            if (virtualAddr + amount >= numPages * pageSize) // prevent address overflow
                break;

            int toRead = Math.min(length - amount, Processor.makeAddress(i, 0) + Processor.pageSize - virtualAddr);

            physicalAddr = virtualToPhysical(virtualAddr + amount);
            TranslationEntry translationEntry = this.getEntry(virtualAddr + amount);

            if(physicalAddr < 0 || physicalAddr >= memory.length) {
                return 0;
            }

            translationEntry.used = true;
            System.arraycopy(memory, physicalAddr, data, offset + amount, toRead);
            amount += toRead;
        }

        return amount;
    }

    @Override
    public int writeVirtualMemory(int virtualAddr, byte[] data) {
        return writeVirtualMemory(virtualAddr, data, 0, data.length);
    }

    @Override
    public int writeVirtualMemory(int virtualAddr, byte[] data, int offset, int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);

        byte[] memory = Machine.processor().getMemory();

        // for i in range vpn_of(virtualAddr) : vpn_of(virtualAddr + length)
        int amount = 0;
        //traverse through the pages of memory
        for (int i = Processor.pageFromAddress(virtualAddr), physicalAddr; i <= Processor.pageFromAddress(virtualAddr + length - 1); ++i) {
            if (virtualAddr + amount >= numPages * pageSize) // prevent address overflow
                break;

            int toWrite = Math.min(length - amount, Processor.makeAddress(i, 0) + Processor.pageSize - virtualAddr);

            physicalAddr = virtualToPhysical(virtualAddr + amount);
            TranslationEntry translationEntry = this.getEntry(virtualAddr + amount);

            if(physicalAddr < 0 || physicalAddr >= memory.length || translationEntry.readOnly) {
                return 0;
            }

            translationEntry.used = true;
            translationEntry.dirty = true;
            System.arraycopy(data, offset + amount, memory, physicalAddr, toWrite);
            amount += toWrite;
        }

        return amount;
    }

    @Override
    protected int virtualToPhysical(int virtualAddr) {
        // Processor.pageFromAddress(v) -> gives the associated vpn
        int vpn = Processor.pageFromAddress(virtualAddr);
        // Processor.offsetFromAddress(virtualAddr) -> gives the offset component of the associated vpn
        int vOffset = Processor.offsetFromAddress(virtualAddr);
        // find the associated entry
        // return corresponding physical address
        TranslationEntry entry = VMKernel.mmu.getPageTableManager().readFromPageTable(pid, vpn, loader);
        if(entry == null || !entry.valid) {
            return -1;
        }
        return Processor.makeAddress(entry.ppn, vOffset);
    }

    private TranslationEntry getEntry(int virtualAddr) {
        int vpn = Processor.pageFromAddress(virtualAddr);
        return VMKernel.mmu.getPageTableManager().readFromPageTable(pid, vpn, loader);
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param    cause    the user exception that occurred.
     */
    @Override
    public void handleException(int cause) {
        Processor processor = Machine.processor();

        switch (cause) {
            // we've to write new cases
            case Processor.exceptionTLBMiss:
                int bad_vpn = Processor.pageFromAddress(processor.readRegister(Processor.regBadVAddr));
                boolean resolved = VMKernel.mmu.getAssociativeMemoryManager().handleTLBMiss(pid, bad_vpn, loader);
                Lib.assertTrue(resolved, "Can't handle TLB miss!");
                break;
            default:
                super.handleException(cause);
                break;
        }
    }

    @Override
    // just show swapping count after termination of each process
    protected int handleExit(int status) {
        System.out.println("Swap count(" + pid + "): "+ SwapSpace.swappingCount);
        return super.handleExit(status);
    }

    private Loader loader;

    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    private static final char dbgVM = 'v';
}
