START   MVI R1 15   ; Load value 15 into R1
        MVI R2 30   ; Load value 30 into R2
        MVI R3 5    ; Load value 5 into R3
        MVI R4 2    ; Load value 2 into R4
        ADD R0 R1 R2 ; R0 = R1 + R2
        SWI 4       ; I/O operation
        SUB R5 R2 R1 ; R5 = R2 - R1
        SWI 4       ; I/O operation
        MUL R6 R1 R3 ; R6 = R1 * R3
        SWI 4       ; I/O operation
        DIV R7 R2 R3 ; R7 = R2 / R3
        SWI 4       ; I/O operation
        ADD R8 R6 R7 ; R8 = R6 + R7
        SWI 4       ; I/O operation
        MUL R9 R8 R4 ; R9 = R8 * R4
        SWI 4       ; I/O operation
        SUB R10 R9 R5 ; R10 = R9 - R5
        SWI 4       ; I/O operation
        DIV R11 R10 R4 ; R11 = R10 / R4
        SWI 4       ; I/O operation
        ADD R12 R11 R1 ; R12 = R11 + R1
        SWI 4       ; I/O operation
        SUB R0 R12 R3 ; R0 = R12 - R3