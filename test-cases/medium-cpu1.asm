START   MVI R1 8    ; Load value 8 into R1
        MVI R2 16   ; Load value 16 into R2
        MVI R3 4    ; Load value 4 into R3
        MVI R4 2    ; Load value 2 into R4
        ADD R5 R1 R2 ; R5 = R1 + R2
        SUB R6 R2 R1 ; R6 = R2 - R1
        MUL R7 R1 R3 ; R7 = R1 * R3
        DIV R8 R2 R3 ; R8 = R2 / R3
        ADD R9 R7 R8 ; R9 = R7 + R8
        SWI 4       ; I/O operation (only one)
        SUB R10 R5 R6 ; R10 = R5 - R6
        MUL R11 R9 R4 ; R11 = R9 * R4
        DIV R12 R10 R4 ; R12 = R10 / R4
        ADD R13 R11 R12 ; R13 = R11 + R12
        SUB R14 R13 R1 ; R14 = R13 - R1
        MUL R15 R14 R3 ; R15 = R14 * R3
        DIV R0 R15 R2 ; R0 = R15 / R2