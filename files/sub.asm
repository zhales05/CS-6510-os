MVI R1 1 ; Load the value of 1 into R1
MVI R2 2    ; Load the value of 2 into R1
SUB R0 R1 R2 ; Sub R1 and R2, store the result in R0
SWI 0 ; prints the value of register 0
