package nachos.vm.Utils;

import nachos.machine.Coff;
import nachos.machine.CoffSection;
import nachos.machine.Lib;
import nachos.machine.TranslationEntry;

/**
 * FIXME: This class is done pretty much
* */

public class Loader {
    public Loader(Coff coff) {
        this.coffFile = coff;
        this.numPages = 0;
        for (int s = 0; s < coffFile.getNumSections(); ++s) {
            CoffSection section = coffFile.getSection(s);

            // TODO:  (it works though) this "if" part need to be checked (if really needed in demand paging)
            if (section.getFirstVPN() != numPages) {
                coff.close();
                Lib.debug(dbgProcess, "fragmented executable");
                Lib.assertTrue(false, "fragmented executable");
            }
            //-----------------------------------

            numPages += section.getLength();
        }
        sectionNums = new int[numPages];
        segmentPageNums = new int[numPages];

        for (int s = 0; s < coffFile.getNumSections(); s++) {
            CoffSection section = coffFile.getSection(s);

            Lib.debug(dbgProcess, "\tinitializing " + section.getName()
                    + " section (" + section.getLength() + " pages)");

            for (int i = 0; i < section.getLength(); i++) {
                int vpn = section.getFirstVPN() + i;
                sectionNums[vpn] = s;
                segmentPageNums[vpn] = i;
                //System.err.println(vpn);
            }
        }
    }


    public TranslationEntry loadSection(int vpn, int ppn) {
        TranslationEntry translationEntry = new TranslationEntry(vpn, ppn, true, false, false, false);
        if(vpn >= 0 && vpn < numPages) {
            translationEntry.readOnly = coffFile.getSection(sectionNums[vpn]).isReadOnly();
            coffFile.getSection(sectionNums[vpn]).loadPage(segmentPageNums[vpn], ppn);
        }
        return translationEntry;
    }



    private final Coff coffFile;
    private int numPages;
    private final int[] sectionNums;
    private final int[] segmentPageNums;

    private static final char dbgProcess = 'a';
}

