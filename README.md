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
## add.asm
```
Adds two numbers: 1 and 2. They should end up in register 0 as 3.
It also calls SWI 0 to print the value of register 0.
```

## sub.asm
```
Subtracts two numbers: 1 and 2. They should end up in register 0 as -1.
It also calls SWI 0 to print the value of register 0.
``` 
## vfork.asm
```
This test case is intended to test the vfork functionality.
```
## child.asm
```
This test case is just running the add program in a child process.
```

