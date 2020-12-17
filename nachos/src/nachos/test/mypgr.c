/* mypgr.c
 *	Simple program to test whether our assignment tasks are correct.
 *
 * 	NOTE: for some reason, user programs with global data structures 
 *	sometimes haven't worked in the Nachos environment.  So be careful
 *	out there!  One option is to allocate data structures as 
 * 	automatics within a procedure, but if you do this, you have to
 *	be careful to allocate a big enough stack to hold the automatics!
 */

#include "syscall.h"

int main()
{
    char *execArgs[256];
    int processID1, processID2, status1, status2;

    printf("\n\n********************************** mypgr Program Loading-test **********************************\n\n");

    printf("mypgr forking halt.coff and joining... \n");
    processID1 = exec("halt.coff", 1,  execArgs);
    int k = join(processID1, &status1);
    printf("********* Join On Process %d Finished\nStatus Value:  %d    ***************\n", processID1, status1);

    printf("mypgr forking halt.coff, halt.coff and joining... \n");
    processID1 = exec("halt.coff", 1,  execArgs);
    processID2 = exec("halt.coff", 1,  execArgs);
    join(processID1, &status1);
    join(processID2, &status2);
    printf("********* Join On Process %d Finished\nStatus Value:  %d    ***************\n", processID1, status1);
    printf("********* Join On Process %d Finished\nStatus Value:  %d    ***************\n", processID2, status2);

    printf("mypgr forking test_args_status.coff and joining... \n");
    execArgs[0] = "test_args_status";
    execArgs[1] = "whatever";
    execArgs[2] = "ok!";
    processID1 = exec("test_args_status.coff", 3, execArgs);
    join(processID1, &status1);
    printf("********* Join On Process %d Finished\nStatus Value:  %d    ***************\n", processID1, status1);


    printf("mypgr forking echo.coff... \n");
    processID1 = exec("echo.coff", 1,  execArgs);
    join(processID1, &status1);
    printf("********* Join On Process %d Finished\nStatus Value:  %d    ***************\n", processID1, status1);

    printf("\nmypgr forking matmult.coff... \n");
    processID1 = exec("matmult.coff", 1, execArgs);
//    join(processID1, &status1);
//    printf("********* Join On Process %d Finished\nStatus Value:  %d    ***************\n", processID1, status1);
//
//    printf("mypgr tries to halt Nachos\n");
//    halt();
//    /* not reached */
//    printf("mypgr failed to halt Nachos!\n");
    return 0;
}
