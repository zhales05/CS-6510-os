# CS-6510-os
Operating System for CS-6510 Operating System Design

# Instructions
Using the terminal, load the osx file into memory and then run the program.

# Assemble
Create valid assembly code file like test.asm
Place in files directory.
osx_mac is within the files directory

If it is your first time running then do the following:
* chmod +x ./osx_mac
* then go to System Settings -> Privacy and Security
* scroll down and allow osx_mac to be run

Now run:
./osx_mac test.asm <loading_address>


# Basic Commands
load files/basic_assemble.osx
load files/add.osx


# Test Cases
## test_1.osx
```
Adds two numbers: 1 and 2. They should end up in register 0 as 3.
It additionally subtracts 2 -1 and stores the result in register 4. We should see 1 in register 4.
It also calls SWI 0 to print the value of register 0 and register 4.
```

## test_2.osx
```
This test case is intended to test the error logging functionality.
This file itself is not wrong but it has a loading addess greater than the maximum capacity of the memory.
``` 
## vfork.osx
```
This test case is intended to test the vfork functionality.
Loading at index 5000 for now
```

