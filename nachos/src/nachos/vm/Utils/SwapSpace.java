package nachos.vm.Utils;

/**
 * Fixme : This class is done (pretty much!)
 * */

import nachos.machine.*;
import nachos.threads.Lock;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public class SwapSpace {
    public SwapSpace() {
        if ((fileName = Config.getString("swapFile")) == null) {
            fileName = "Swap_Area";
        }
        freeSwaps = new LinkedList<>();
        assignedPage = new HashMap<>();
        translationMap = new HashMap<>();
    }

    public void init() {
        swappingInCount = 0;
        swappingOutCount = 0;
        this.swapFile = Machine.stubFileSystem().open(fileName, true);
        swapLock = new Lock();
    }

    public void close() {
        this.swapFile.close();
        Machine.stubFileSystem().remove(fileName);
    }

    private int allocateNextPage() {
        if(freeSwaps.isEmpty()) {
            return pageCount++;
        }
        return freeSwaps.removeFirst();
    }

    public int swapOut(int pid, int vpn, TranslationEntry translationEntry) {
        if(translationEntry == null) return 0;
        Pair<Integer, Integer> pageKey = new Pair(pid, vpn);

        swapLock.acquire();
        int pageNo = assignedPage.containsKey(pageKey) ? assignedPage.get(pageKey) : allocateNextPage();
        assignedPage.put(pageKey, pageNo);
        translationMap.put(pageKey, translationEntry);
        ++swappingOutCount;
        int writtenBytes = swapFile.write(pageNo * Machine.processor().pageSize, Machine.processor().getMemory(),
                Processor.makeAddress(translationEntry.ppn, 0), Machine.processor().pageSize);
        swapLock.release();

        return writtenBytes;
    }

    public TranslationEntry swapIn(int pid, int vpn, int ppn) {
        Pair<Integer, Integer> pageKey = new Pair(pid, vpn);
        if(!translationMap.containsKey(pageKey))
            return null;
        swapLock.acquire();
        int pageNo = assignedPage.get(pageKey);
        int readBytes = swapFile.read(pageNo * Machine.processor().pageSize, Machine.processor().getMemory(),
                Processor.makeAddress(ppn, 0), Machine.processor().pageSize);

        if(readBytes < Machine.processor().pageSize)
            return null;

        TranslationEntry translationEntry = translationMap.get(pageKey);
        translationEntry.vpn = vpn;
        translationEntry.ppn = ppn;
        translationEntry.valid = true;
        translationEntry.dirty = false;
        translationEntry.used = false;

        ++swappingInCount;
        swapLock.release();

        return translationEntry;
    }

    public void clearPages(int pid) {
        for(Iterator<Map.Entry<Pair, Integer>> it = assignedPage.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Pair, Integer> entry = it.next();
            if(entry == null) {
                System.out.println("during clearing pages");
            }
            if(entry != null && (Integer) entry.getKey().first == pid) {
                freeSwaps.add(entry.getValue());
                it.remove();
            }
        }
    }

    OpenFile swapFile;
    private String fileName;

    public static int swappingInCount = 0, swappingOutCount = 0;
    private int pageCount = 0;

    private static Lock swapLock;

    private LinkedList<Integer> freeSwaps;
    private HashMap<Pair, Integer> assignedPage;
    private HashMap<Pair, TranslationEntry> translationMap;
}
