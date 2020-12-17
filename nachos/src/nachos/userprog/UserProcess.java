package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    // FIXME : code added (pid) and removed (translation entries)
    public UserProcess() {
        boolean intStatus = Machine.interrupt().disable();
        pid = ++processCount;
        Machine.interrupt().restore(intStatus);
    }

    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
        return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param name the name of the file containing the executable.
     * @param args the arguments to pass to the executable.
     * @return <tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
        if (!load(name, args))
            return false;

        thread = new UThread(this).setName(name);
        thread.fork();

        return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
        Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param vaddr     the starting virtual address of the null-terminated
     *                  string.
     * @param maxLength the maximum number of characters in the string,
     *                  not including the null terminator.
     * @return the string read, or <tt>null</tt> if no null terminator was
     * found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
        Lib.assertTrue(maxLength >= 0);

        byte[] bytes = new byte[maxLength + 1];

        int bytesRead = readVirtualMemory(vaddr, bytes);

        for (int length = 0; length < bytesRead; length++) {
            if (bytes[length] == 0)
                return new String(bytes, 0, length);
        }

        return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param vaddr the first byte of virtual memory to read.
     * @param data  the array where the data will be stored.
     * @return the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
        return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param vaddr  the first byte of virtual memory to read.
     * @param data   the array where the data will be stored.
     * @param offset the first byte to write in the array.
     * @param length the number of bytes to transfer from virtual memory to
     *               the array.
     * @return the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
                                 int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);

        byte[] memory = Machine.processor().getMemory();

        if (vaddr < 0 || vaddr >= memory.length)
            return 0;

        // FIXME : code added
        // for i in range vpn_of(vaddr) : vpn_of(vaddr + length)
        int amount = 0;
        //traverse through the pages of memory
        for (int i = Processor.pageFromAddress(vaddr); i <= Processor.pageFromAddress(vaddr + length - 1); ++i) {
            if (vaddr + amount >= numPages * pageSize) break; // prevent address overflow
            int toRead = Math.min(length - amount, Processor.makeAddress(i, 0) + Processor.pageSize - vaddr);
            System.arraycopy(memory, virtualToPhysical(vaddr + amount), data, offset + amount, toRead);
            amount += toRead;
        }

        return amount;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param vaddr the first byte of virtual memory to write.
     * @param data  the array containing the data to transfer.
     * @return the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
        return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param vaddr  the first byte of virtual memory to write.
     * @param data   the array containing the data to transfer.
     * @param offset the first byte to transfer from the array.
     * @param length the number of bytes to transfer from the array to
     *               virtual memory.
     * @return the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
                                  int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);

        byte[] memory = Machine.processor().getMemory();

        if (vaddr < 0 || vaddr >= memory.length)
            return 0;

        // FIXME : code added
        // for i in range vpn_of(vaddr) : vpn_of(vaddr + length)
        int amount = 0;
        //traverse through the pages of memory
        for (int i = Processor.pageFromAddress(vaddr); i <= Processor.pageFromAddress(vaddr + length - 1); ++i) {
            if (vaddr + amount >= numPages * pageSize) break;
            int toWrite = Math.min(length - amount, Processor.makeAddress(i, 0) + Processor.pageSize - vaddr);
            System.arraycopy(data, offset + amount, memory, virtualToPhysical(vaddr + amount), toWrite);
            amount += toWrite;
        }

        return amount;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param name the name of the file containing the executable.
     * @param args the arguments to pass to the executable.
     * @return <tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
        Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

        OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
        if (executable == null) {
            Lib.debug(dbgProcess, "\topen failed");
            return false;
        }

        try {
            coff = new Coff(executable);
        } catch (EOFException e) {
            executable.close();
            Lib.debug(dbgProcess, "\tcoff load failed");
            return false;
        }

        // make sure the sections are contiguous and start at page 0
        numPages = 0;
        for (int s = 0; s < coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);
            if (section.getFirstVPN() != numPages) {
                coff.close();
                Lib.debug(dbgProcess, "\tfragmented executable");
                return false;
            }
            numPages += section.getLength();
        }

        // make sure the argv array will fit in one page
        byte[][] argv = new byte[args.length][];
        int argsSize = 0;
        for (int i = 0; i < args.length; i++) {
            argv[i] = args[i].getBytes();
            // 4 bytes for argv[] pointer; then string plus one for null byte
            argsSize += 4 + argv[i].length + 1;
        }
        if (argsSize > pageSize) {
            coff.close();
            Lib.debug(dbgProcess, "\targuments too long");
            return false;
        }

        // program counter initially points at the program entry point
        initialPC = coff.getEntryPoint();

        // next comes the stack; stack pointer initially points to top of it
        numPages += stackPages;
        initialSP = numPages * pageSize;

        // and finally reserve 1 page for arguments
        numPages++;

        if (!loadSections())
            return false;

        // store arguments in last page
        int entryOffset = (numPages - 1) * pageSize;
        int stringOffset = entryOffset + args.length * 4;

        this.argc = args.length;
        this.argv = entryOffset;

        for (int i = 0; i < argv.length; i++) {
            byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
            Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
            stringOffsetBytes = new byte[4];
            readVirtualMemory(entryOffset, stringOffsetBytes);
            entryOffset += 4;
            Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
                    argv[i].length);
            stringOffset += argv[i].length;
            Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[]{0}) == 1);
            stringOffset += 1;
        }

        return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return <tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
        if (numPages > Machine.processor().getNumPhysPages()) {
            coff.close();
            Lib.debug(dbgProcess, "\tinsufficient physical memory");
            return false;
        }
        // FIXME : code added to load info into physical pages
        physicalPagesAcquired = UserKernel.acquirePages(numPages);
        pageTable = new TranslationEntry[numPages];
        for (int i = 0; i < numPages; i++)
            pageTable[i] = new TranslationEntry(i, physicalPagesAcquired.get(i), true, false, false, false);
        // upto this

        // load sections
        for (int s = 0; s < coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);

            Lib.debug(dbgProcess, "\tinitializing " + section.getName()
                    + " section (" + section.getLength() + " pages)");

            for (int i = 0; i < section.getLength(); i++) {
                int vpn = section.getFirstVPN() + i;
                // FIXME : code added
                pageTable[vpn].readOnly = section.isReadOnly(); // resolve rea-donly here
                int ppn = pageTable[vpn].ppn;
                section.loadPage(i, ppn); // now ppn passed
                // upto this
            }
        }

        return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
        // FIXME : release the physical pages acquired by this process; this process is going to exit
        UserKernel.returnPages(physicalPagesAcquired);
        physicalPagesAcquired.clear();
    }

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
        Processor processor = Machine.processor();

        // by default, everything's 0
        for (int i = 0; i < processor.numUserRegisters; i++)
            processor.writeRegister(i, 0);

        // initialize PC and SP according
        processor.writeRegister(Processor.regPC, initialPC);
        processor.writeRegister(Processor.regSP, initialSP);

        // initialize the first two argument registers to argc and argv
        processor.writeRegister(Processor.regA0, argc);
        processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call.
     */
    private int handleHalt() {
        if (pid != 1) return -1; // FIXME : code added
        Machine.halt();

        Lib.assertNotReached("Machine.halt() did not halt machine!");
        return 0;
    }

    // FIXME : int write(int fileDescriptor, void *buffer, int count);
    private int handleWrite(int fd, int buffer, int count) {
        if (fd != 1) return -1;
        if (buffer < 0 || count < 0) return -1;
        OpenFile openFile = UserKernel.console.openForWriting();
        int writtenByte = 0;
        while (writtenByte < count) {
            int bufSize = 1 << 10, bytesToRead = Math.min(bufSize, count - writtenByte);
            byte[] outputBuffer = new byte[bufSize];
            int readByte = readVirtualMemory(buffer + writtenByte, outputBuffer, 0, bytesToRead);
            if (readByte != bytesToRead) return -1;
            Lib.assertTrue(openFile.write(outputBuffer, 0, readByte) != -1);
            writtenByte += readByte;
        }
        openFile.close();
        return writtenByte;
    }

    // FIXME : int read(int fileDescriptor, void *buffer, int count);
    private int handleRead(int fd, int buffer, int count) {
        if (!(fd == 0 && count == 1)) return -1;
        if (buffer < 0) return -1;
        OpenFile openFile = UserKernel.console.openForReading();
        byte[] b = new byte[1];
        openFile.read(b, 0, 1);
        return writeVirtualMemory(buffer, b) == 1 ? 1 : -1;
    }

    // FIXME : int exec(char *file, int argc, char *argv[]);
    /*In handling exec, we extracted virtual address of ith argument by readVirtualMemory(argv+i*4,..)
    and extracted the argument from the virtual address using readVirtualMemoryString.*/
    private int handleExec(int file, int argc, int argv) {
        String fileName = readVirtualMemoryString(file, 256);
        if (argc < 0 || fileName == null || fileName.length() <= 5 || !fileName.endsWith(".coff")) {
            return -1;
        }

        String[] args = new String[argc];
        for (int i = 0; i < argc; i++) {
            byte[] bytes = new byte[4];
            readVirtualMemory(argv + i * 4, bytes); // each address is 4 bytes
            int vaddr = Lib.bytesToInt(bytes, 0);
            args[i] = readVirtualMemoryString(vaddr, 256);
            if (args[i] == null) return -1;
        }
        UserProcess child = newUserProcess();
        child.parent = this;
        pidToChild.put(child.pid, child);
        return child.execute(fileName, args) ? child.pid : -1;
    }

    // FIXME :  int join(int processID, int *status);
    private int handleJoin(int processID, int status) {
        UserProcess child = pidToChild.get(processID);
        if (child == null) return -1;
        child.thread.join(); // call KThread's join on child
        pidToChild.put(processID, null); // disowns the child
        if (childStatus == null) return 0; // child exited as a result of unhandled exception
        writeVirtualMemory(status, Lib.bytesFromInt(childStatus));
        childStatus = null;
        return 1;
    }

    // FIXME : void exit(int status);
    protected int handleExit(int status) {
        unloadSections(); // the process memory is freed on exit immediately

        boolean intStatus = Machine.interrupt().disable();
        if (parent != null) parent.childStatus = status;    // update child's exit status[by user program] (collected via join)
        for (UserProcess child :                    // children loss their parent
                pidToChild.values()) {
            if (child != null) child.parent = null;
        }
        if (++exitedProcessCount == processCount) { // terminate the kernel in case of the last process
            Kernel.kernel.terminate();
        }
        Machine.interrupt().restore(intStatus);

        UThread.finish(); // direct call to finish the KThread[finish the thread and schedule it to be destroyed]
        return 0;
    }


    private static final int
            syscallHalt = 0,
            syscallExit = 1,
            syscallExec = 2,
            syscallJoin = 3,
            syscallCreate = 4,
            syscallOpen = 5,
            syscallRead = 6,
            syscallWrite = 7,
            syscallClose = 8,
            syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     * 								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     * 								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     *
     * @param syscall the syscall number.
     * @param a0      the first syscall argument.
     * @param a1      the second syscall argument.
     * @param a2      the third syscall argument.
     * @param a3      the fourth syscall argument.
     * @return the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
        switch (syscall) {
            case syscallHalt:
                return handleHalt();
            case syscallExit:
                return handleExit(a0);
            case syscallExec:
                return handleExec(a0, a1, a2);
            case syscallJoin:
                return handleJoin(a0, a1);
            case syscallRead:
                return handleRead(a0, a1, a2);
            case syscallWrite:
                return handleWrite(a0, a1, a2);
            default:
                Lib.debug(dbgProcess, "Unknown syscall " + syscall);
                Lib.assertNotReached("Unknown system call!");
        }
        return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param cause the user exception that occurred.
     */
    public void handleException(int cause) {
        Processor processor = Machine.processor();

        switch (cause) {
            case Processor.exceptionSyscall:
                int result = handleSyscall(processor.readRegister(Processor.regV0),
                        processor.readRegister(Processor.regA0),
                        processor.readRegister(Processor.regA1),
                        processor.readRegister(Processor.regA2),
                        processor.readRegister(Processor.regA3)
                );
                processor.writeRegister(Processor.regV0, result);
                processor.advancePC();
                break;

            default:
                Lib.debug(dbgProcess, "Unexpected exception: " +
                        Processor.exceptionNames[cause]);
                Lib.assertNotReached("Unexpected exception");
        }
    }

    // FIXME : returns the associated physical page address of a virtual address
    protected int virtualToPhysical(int vaddr) {
        // Processor.pageFromAddress(vaddr) -> gives the associated vpn
        // pageTable[Processor.pageFromAddress(vaddr)].ppn -> gives the associated ppn
        // Processor.offsetFromAddress(vaddr) -> gives the offset component of the associated vp
        return Processor.makeAddress(pageTable[Processor.pageFromAddress(vaddr)].ppn, Processor.offsetFromAddress(vaddr));
    }

    /**
     * The program being run by this process.
     */
    protected Coff coff;

    /**
     * This process's page table.
     */
    private TranslationEntry[] pageTable;
    /**
     * The number of contiguous pages occupied by the program.
     */
    protected int numPages;

    /**
     * The number of pages in the program's stack.
     */
    protected final int stackPages = 8;

    private int initialPC, initialSP;
    private int argc, argv;

    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';

    private KThread thread; // save the thread which runs this process
    private static int processCount = 0, exitedProcessCount = 0;
    protected final int pid;
    private ArrayList<Integer> physicalPagesAcquired;
    private UserProcess parent = null;
    HashMap<Integer, UserProcess> pidToChild = new HashMap<>();
    private Integer childStatus = null;
}
