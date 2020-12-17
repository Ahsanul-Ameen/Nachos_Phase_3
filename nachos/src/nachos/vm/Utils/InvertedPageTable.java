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
        ppnToProcessPrivate = new HashMap<>();
        invertedTranslator = new HashMap<>();
    }

    public void insert(Integer pid, TranslationEntry translationEntry) {
        ppnToProcessPrivate.put(translationEntry.ppn, new Pair<>(new Pair<>(pid, translationEntry.vpn), translationEntry));
        invertedTranslator.put(new Pair<>(pid, translationEntry.vpn), translationEntry.ppn);
    }

    public void removePage(Integer pid, Integer vpn) {
        Pair<Integer, Integer> key = new Pair(pid, vpn);
        if(invertedTranslator.containsKey(key)) {
            Integer ppn = invertedTranslator.get(key);
            Lib.assertTrue(ppnToProcessPrivate.containsKey(ppn), "Corrupted Page Table!");
            ppnToProcessPrivate.remove(ppn);
            invertedTranslator.remove(key);
        }
    }

    public void removeProcess(Integer pid) {
        List<Pair<Integer, Integer>> removableKeys = new ArrayList<>();
        for(Pair<Integer, Integer> key : invertedTranslator.keySet()) {
            if(key.first == pid) {
                removableKeys.add(key);
            }
        }

        for(Pair<Integer, Integer> pid_vpn : removableKeys) {
            removePage(pid_vpn.first, pid_vpn.second);
        }
    }

    public TranslationEntry getTranslationEntry(Integer ppn) {
        if(ppn == null || ppn < 0 || ppn >= Machine.processor().getNumPhysPages()) return null;
        return (ppnToProcessPrivate.containsKey(ppn)) ? ppnToProcessPrivate.get(ppn).second : null;
    }

    public TranslationEntry getTranslationEntry(Integer pid, Integer vpn) {
        return getTranslationEntry(invertedTranslator.get(new Pair<>(pid, vpn)));
    }

    public Integer getPID(Integer ppn) {
        if(ppn == null || ppn < 0 || ppn >= Machine.processor().getNumPhysPages()) return -1;
        return ppnToProcessPrivate.containsKey(ppn) ? ppnToProcessPrivate.get(ppn).first.first : -1;
    }

    public Integer getVPN(Integer ppn) {
        if(ppn == null || ppn < 0 || ppn >= Machine.processor().getNumPhysPages()) return -1;
        return ppnToProcessPrivate.containsKey(ppn) ? ppnToProcessPrivate.get(ppn).first.second : -1;
    }



    // ppn -> {{pid, vpn}, TranslationEntry}
    private final HashMap<Integer, Pair<Pair<Integer, Integer>, TranslationEntry>> ppnToProcessPrivate;
    // {pid, vpn} -> ppn
    private final HashMap<Pair<Integer, Integer>, Integer> invertedTranslator;

}