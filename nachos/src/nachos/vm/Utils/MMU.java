package nachos.vm.Utils;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.TranslationEntry;
import nachos.threads.Lock;


public class MMU {

    public MMU() {
        associativeMemoryManager = new AssociativeMemoryManager();
        pageTableManager = new PageTableManager();
    }

    public void init() {
        associativeMemoryManager.init();
        pageTableManager.init();
    }

    public void clearSwapArea() {
        pageTableManager.clearSwapSpace();
    }

    public AssociativeMemoryManager getAssociativeMemoryManager() {
        return associativeMemoryManager;
    }

    public PageTableManager getPageTableManager() {
        return pageTableManager;
    }

    // A page scheduler class
    public static class PageTableManager {

        public PageTableManager() {
            this.swapSpace = new SwapSpace();
            this.pageTable = new InvertedPageTable();
        }

        public void init() {
            int numPhyPage = Machine.processor().getNumPhysPages();
            lruReplacement = new long[numPhyPage];
            for(int i = 0; i < numPhyPage; ++i) {
                lruReplacement[i] = Machine.timer().getTime();
            }

            this.swapSpace.init();
            this.faultLock = new Lock();
        }

        // LRU page eviction policy
        private int findRemovableIndex() {
            Lib.assertTrue(lruReplacement.length > 0, "number of physical pages is <= 0 ?");
            int iMin = 0;
            for(int i = 0; i < lruReplacement.length; ++i) {
                if(lruReplacement[i] < lruReplacement[iMin])
                    iMin = i;
            }
            return iMin;
        }

        // unload the pages acquired from page table & swap area
        // when unloadSections() is invoked
        public void clearPages(int pid) {
            pageTable.removeProcess(pid);
            swapSpace.clearPages(pid);
        }

        // write an entry into the inverted page table
        public void writeInPageTable(int pid, TranslationEntry translationEntry) {
            pageTable.insert(pid, translationEntry);
        }


        // TODO : this method could be updated (works for now ...)
        public TranslationEntry readFromPageTable(int pid, int vpn, Loader loader) {
            TranslationEntry translationEntry = pageTable.getTranslationEntry(pid, vpn);
            if(translationEntry == null) {
                boolean status = handlePageFaultHardMiss(pid, vpn, loader);
                if(!status) {
                    System.out.println("Can't resolve (hard miss) page fault!\nShould we kill the process ?");
                    //Lib.assertTrue(status, "Can't resolve (hard miss) page fault!");
                } else {
                    translationEntry = pageTable.getTranslationEntry(pid, vpn);
                    if(translationEntry != null) {
                        lruReplacement[translationEntry.ppn] = Machine.timer().getTime(); // page invoked recently
                    }
                }
            }
            return translationEntry;
        }

        // handles a hard miss(not in Page Table; but may be in swap area or disk)
        public boolean handlePageFaultHardMiss(int pid, int vpn, Loader loader) {

            faultLock.acquire();

            int removable_ppn = findRemovableIndex();
            int removable_vpn = pageTable.getVPN(removable_ppn);
            int removable_pid = pageTable.getPID(removable_ppn);
            TranslationEntry removable_entry = pageTable.getTranslationEntry(removable_ppn);

            // do a swap out operation with a removable page
            {
                if(removable_pid == pid) {
                    //clear the corresponding TLB entry (consumed by this process's vpn)

                    associativeMemoryManager.eraseTLBEntry(removable_pid, removable_vpn);
                }

                if(removable_entry != null) {
                    int writtenBytes = swapSpace.swapOut(removable_pid, removable_vpn, removable_entry);
                    //Lib.assertTrue(writtenBytes != Machine.processor().pageSize, "writing error during swap out!");
                }
                pageTable.removePage(removable_pid, removable_vpn);
            }

            // do a swap in operation with the required page
            {
                // first find the page in the swap area
                removable_entry = swapSpace.swapIn(pid, vpn, removable_ppn); // ppn unchanged
                // page not in swap area so load it from program(disk)
                if(removable_entry == null) {
                    // load appropriate section from disk(file) & update entry using loader
                    removable_entry = new TranslationEntry(vpn, removable_ppn, true,
                            loader.loadSection(vpn, removable_ppn).readOnly, false, false);
                }
                pageTable.insert(pid, removable_entry);
                lruReplacement[removable_ppn] = Machine.timer().getTime(); // update page load time
            }

            faultLock.release();

            return true;
        }

        // release the Swap area when the kernel terminates
        // called by VM kernel
        public void clearSwapSpace() {
            this.swapSpace.close();
        }

        private final SwapSpace swapSpace;
        private final InvertedPageTable pageTable;
        private Lock faultLock;

        private long[] lruReplacement;
    }

    // A scheduler class for the TLB
    public static class AssociativeMemoryManager {
        public AssociativeMemoryManager() {
            tlb = new TLB();
        }

        public void init() {
            tlb.init();
        }

        // write an entry into the given index of TLB
        public void writeTLBEntry(int number, TranslationEntry entry) {
            boolean intStatus = Machine.interrupt().disable();

            Machine.processor().writeTLBEntry(number, entry);

            Machine.interrupt().restore(intStatus);
        }

        // read an entry and write back to the page table (in case dirty)
        public void writeBackTLBEntry(int number, int pid) {
            boolean intStatus = Machine.interrupt().disable();

            TranslationEntry translationEntry = Machine.processor().readTLBEntry(number);
            if(translationEntry.dirty) {
                //pageTableManager.writeInPageTable(pid, translationEntry);
                pageTableManager.writeInPageTable(pid, translationEntry); //fixme
            }

            Machine.interrupt().restore(intStatus);
        }

        // erase an entry from TLB, push the new one to TLB and PageTable
        public void addTLBEntry(int pid, TranslationEntry entry) {
            boolean intStatus = Machine.interrupt().disable();

            int removableIndex = tlb.findRemovableIndex();
            writeBackTLBEntry(removableIndex, pid);

            //pageTableManager.writeInPageTable(pid, entry);
            pageTableManager.writeInPageTable(pid, entry); //fixme
            writeTLBEntry(removableIndex, entry);

            tlb.setProcessIds(removableIndex, pid);
            tlb.setLruCache(removableIndex, Machine.timer().getTime());

            Machine.interrupt().restore(intStatus);
        }

        // remove a particular TLB entry
        public void eraseTLBEntry(int pid, int vpn) {
            boolean intStatus = Machine.interrupt().disable();

            int remCnt = 0;
            TranslationEntry invalidEntry = new TranslationEntry();
            for(int i = 0; i < TLB.getTlbSize(); ++i) {
                if(pid == tlb.getProcessId(i) &&
                        vpn == Machine.processor().readTLBEntry(i).vpn) {
                    writeBackTLBEntry(i, pid); // move to page table (if needed)
                    writeTLBEntry(i, invalidEntry); // clear the entry (assign an invalid one)
                    ++remCnt;
                    //break;
                }
            }
            Lib.assertTrue(remCnt < 2, "Same (pid, vpn) maps to multiple TLB entry ?");

            Machine.interrupt().restore(intStatus);

        }

        // clear the entire TLB with respect to a particular process
        public void clearTLB(int pid) {
            boolean intStatus = Machine.interrupt().disable();

            TranslationEntry invalidEntry = new TranslationEntry();
            for(int i = 0; i < TLB.getTlbSize(); ++i) {
                if(pid == tlb.getProcessId(i)) {
                    writeBackTLBEntry(i, pid); // move to page table (if dirty)
                    writeTLBEntry(i, invalidEntry); // clear the entry (assign an invalid one)
                    tlb.setProcessIds(i, -1); //fixme: set invalid pid
                }
            }
            //Lib.assertTrue(remCnt != TLB.getTlbSize(), "Invalid pid invoked in TLB?");

            Machine.interrupt().restore(intStatus);
        }

        // return true if it can handle a TLB miss(maybe preceded by a resolved PageFault)
        public boolean handleTLBMiss(int pid, int vpn, Loader loader) {
            //TranslationEntry pageEntry = pageTableManager.readFromPageTable(pid, vpn, loader);
            TranslationEntry pageEntry = pageTableManager.readFromPageTable(pid, vpn, loader);//fixme
            if(pageEntry == null) {
                // an unresolved hard miss followed by a soft miss
                return false;
            }
            // resolve the soft miss
            this.addTLBEntry(pid, pageEntry); // put into TLB from pageTable
            return true;
        }

        private final TLB tlb;
    }


    private static AssociativeMemoryManager associativeMemoryManager;
    private static PageTableManager pageTableManager;
}