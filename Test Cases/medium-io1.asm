START   MVI R1 10   ; Load value 10 into R1
        MVI R2 20   ; Load value 20 into R2
        MVI R3 5    ; Load value 5 into R3
        ADD R0 R1 R2 ; R0 = R1 + R2
        SWI 4       ; I/O operation
        SUB R4 R2 R1 ; R4 = R2 - R1
        SWI 4       ; I/O operation
        MUL R5 R1 R3 ; R5 = R1 * R3
        SWI 4       ; I/O operation
        DIV R6 R2 R3 ; R6 = R2 / R3
        SWI 4       ; I/O operation
        ADD R7 R5 R6 ; R7 = R5 + R6
        SWI 4       ; I/O operation
        SUB R0 R7 R4 ; R0 = R7 - R4