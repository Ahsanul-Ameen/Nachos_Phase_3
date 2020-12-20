package nachos.vm.Utils;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.TranslationEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//FIXME: pretty much complete
public class InvertedPageTable {

    public InvertedPageTable() {
        coreMap = new HashMap<>();
        invertedTranslator = new HashMap<>();
    }

    public void insert(Integer pid, TranslationEntry translationEntry) {
        coreMap.put(translationEntry.ppn, new Pair<>(new Pair<>(pid, translationEntry.vpn), translationEntry));
        invertedTranslator.put(new Pair<>(pid, translationEntry.vpn), translationEntry.ppn);
    }

    public void removePage(Integer pid, Integer vpn) {
        Pair<Integer, Integer> key = new Pair<>(pid, vpn);
        if(invertedTranslator.containsKey(key)) {
            Integer ppn = invertedTranslator.get(key);
            Lib.assertTrue(coreMap.containsKey(ppn), "Corrupted Page Table!");
            coreMap.remove(ppn);
            invertedTranslator.remove(key);
        }
    }

    public void removeProcess(Integer pid) {
        List<Pair<Integer, Integer>> removableKeys = new ArrayList<>();
        for(Pair<Integer, Integer> key : invertedTranslator.keySet()) {
            if(key.first.equals(pid)) {
                removableKeys.add(key);
            }
        }

        for(Pair<Integer, Integer> pid_vpn : removableKeys) {
            removePage(pid_vpn.first, pid_vpn.second);
        }
    }

    public TranslationEntry getTranslationEntry(Integer ppn) {
        if(ppn == null || ppn < 0 || ppn >= Machine.processor().getNumPhysPages()) return null;
        return (coreMap.containsKey(ppn)) ? coreMap.get(ppn).second : null;
    }

    public TranslationEntry getTranslationEntry(Integer pid, Integer vpn) {
        return getTranslationEntry(invertedTranslator.get(new Pair<>(pid, vpn)));
    }

    public Integer getPID(Integer ppn) {
        if(ppn == null || ppn < 0 || ppn >= Machine.processor().getNumPhysPages()) return -1;
        return coreMap.containsKey(ppn) ? coreMap.get(ppn).first.first : -1;
    }

    public Integer getVPN(Integer ppn) {
        if(ppn == null || ppn < 0 || ppn >= Machine.processor().getNumPhysPages()) return -1;
        return coreMap.containsKey(ppn) ? coreMap.get(ppn).first.second : -1;
    }

    public boolean isRAMFree() {
        return this.coreMap.size() < Machine.processor().getNumPhysPages();
    }

    public Integer nextFreePPN() {
        Lib.assertTrue(isRAMFree(), "No free ppn, needs to swap out!");
        for(int ppn = 0, MAX_PPN = Machine.processor().getNumPhysPages(); ppn < MAX_PPN; ++ppn) {
            if(!coreMap.containsKey(ppn)) {
                return ppn;
            }
        }
        return -1;
    }

    // ppn -> {{pid, vpn}, TranslationEntry}
    private final HashMap<Integer, Pair<Pair<Integer, Integer>, TranslationEntry>> coreMap;
    // {pid, vpn} -> ppn
    private final HashMap<Pair<Integer, Integer>, Integer> invertedTranslator;

}