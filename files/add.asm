DATA1:  .WORD 10       ; Define a memory location with value 10
DATA2:  .WORD 20       ; Define another memory location with value 20
RESULT: .WORD 0        ; Define memory to store the result (initially 0)
CHAR1: .BYTE 'A'       ; Define a memory location with value 'A'
SPACE:  .SPACE 10      ; Define 100 bytes of space

        MVI R1 10 ; Load the address of DATA1 into R1
        MVI R2 11    ; Load the value of 11 into R1
        ADD R3 R1 R2 ; Add R1 and R2, store the result in R3
        SUB R4 R1 R2 ; Subtract R2 from R1, store the result in R4
