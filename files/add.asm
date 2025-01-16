DATA1:  .WORD 10       ; Define a memory location with value 10
DATA2:  .WORD 20       ; Define another memory location with value 20
RESULT: .WORD 0        ; Define memory to store the result (initially 0)
CHAR1: .BYTE 'A'       ; Define a memory location with value 'A'
SPACE:  .SPACE 100     ; Define 100 bytes of space

        ADD R3 R1 R2 ; Add R1 and R2, store the result in R3
