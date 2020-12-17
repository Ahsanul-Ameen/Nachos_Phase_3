package nachos.vm.Utils;

import nachos.machine.Lib;
import nachos.machine.Machine;

public class TLB {
    TLB() {}

    void init() {
        tlbSize = Machine.processor().getTLBSize();
        processIds = new int[tlbSize];
        lruCache = new long[tlbSize];

        for(int i = 0; i < tlbSize; ++i) {
            processIds[i] = -1;
            lruCache[i] = Machine.timer().getTime();
        }
    }

    void setLruCache(int tlbIndex, long time) {
        Lib.assertTrue(tlbIndex >= 0 && tlbIndex < tlbSize, "tlb index out of bound");
        lruCache[tlbIndex] = time;
    }

    void setProcessIds(int tlbIndex, int pid) {
        Lib.assertTrue(tlbIndex >= 0 && tlbIndex < tlbSize, "tlb index out of bound");
        processIds[tlbIndex] = pid;
    }

    int getProcessId(int tlbIndex) {
        Lib.assertTrue(tlbIndex >= 0 && tlbIndex < tlbSize, "tlb index out of bound");
        return processIds[tlbIndex];
    }

    int findRemovableIndex() {
        Lib.assertTrue(lruCache.length > 0, "number of TLB entry is <= 0 ?");
        int iMin = 0;
        for(int i = 0; i < lruCache.length; ++i) {
            if(lruCache[i] < lruCache[iMin])
                iMin = i;
        }
        return iMin;
    }

    public static int getTlbSize() {
        return tlbSize;
    }

    private int[] processIds;
    private long[] lruCache;
    private static int tlbSize;
}
