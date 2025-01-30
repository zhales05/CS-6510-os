MVI R1 2 ; Load the value of 1 into R1
MVI R2 3    ; Load the value of 2 into R1
ADD R5 R1 R2 ; Add R1 and R2, store the result in R0
SUB R2 R2 R1 ; Subtract R2 from R1, store the result in R2
SWI 0 ; prints the value of register 0
