DATA1:  .WORD 10       ; Define a memory location with value 10
DATA2:  .WORD 20       ; Define another memory location with value 20
RESULT: .WORD 0        ; Define memory to store the result (initially 0)
CHAR1: .BYTE 'A'       ; Define a memory location with value 'A'
SPACE:  .SPACE 10      ; Define 100 bytes of space

        MVI R1 1 ; Load the value of 1 into R1
        MVI R2 2    ; Load the value of 2 into R1
        ADD R0 R1 R2 ; Add R1 and R2, store the result in R0
        SUB R4 R1 R2 ; Subtract R2 from R1, store the result in R4
