# CS-6510-os
Operating System for CS-6510 Operating System Design

---

# Instructions

Using the terminal, **load** the `.osx` file into memory and then **run** the program.

---

# Commands

| Command | Description | Example Usage |
|:--------|:------------|:--------------|
| `load <program1> <startTime1> <program2> <startTime2> ...` | Load programs into the job queue without running them. | `load files/p1.osx 0 files/p2.osx 5` |
| `execute <program1> <startTime1> <program2> <startTime2> ...` | Load and immediately run programs through the ready queue. | `execute files/p1.osx 0 files/p2.osx 5` |
| `run` | Runs all processes currently in the ready queue. | `run` |
| `setpagesize <size>` | Set the page size for memory management. | `setpagesize 32` |
| `getpagesize` | Display the current page size setting. | `getpagesize` |
| `setpagenumber <number>` | Set the number of pages (resizes memory accordingly). | `setpagenumber 100` |
| `ps` | Display currently loaded processes and free memory frames. | `ps` |
| `ps -proc` | Display only currently active (non-terminated) processes. | `ps -proc` |
| `ps -free` | Display only free frames in memory. | `ps -free` |
| `coredump` | Display the full memory contents across all frames. | `coredump` |
| `coredump <program>` | Display only the memory contents for a specific program. | `coredump files/p1.osx` |
| `setsched <algorithm> <params>` | Set the scheduling algorithm (FCFS, RR, MFQ). | `setsched rr 5` |
| `osx <source.os> <loader address>` | Assemble a `.os` file into a `.osx` executable using the assembler tool. | `osx files/source.os 0` |
| `errordump` | Print collected error logs from execution. | `errordump` |
| `redo` | Rerun the previous command. | `redo` |
| `exit` | Exit the virtual machine shell. | `exit` |

---

# Notes

- Programs must be compiled into `.osx` format using the `osx` command before they can be loaded or executed.
- The `ps` command can be used to check memory usage and process status.
- Memory settings (`setpagesize` and `setpagenumber`) must be configured **before** loading programs if custom settings are desired.
- The `coredump` command is useful for debugging and viewing memory layouts.
- The `setsched` command must be called before `execute` if a specific scheduling algorithm is desired.

