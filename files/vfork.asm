MVI R1 2 ; Load the value of 1 into R1
MVI R2 3    ; Load the value of 2 into R1
ADD R0 R1 R2 ; Add R1 and R2, store the result in R0
SWI 2 ; vfork baby
SWI 0 ; prints the value of register 0

