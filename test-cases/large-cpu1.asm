START   MVI R1 12   ; Load value 12 into R1
        MVI R2 24   ; Load value 24 into R2
        MVI R3 6    ; Load value 6 into R3
        MVI R4 3    ; Load value 3 into R4
        MVI R5 2    ; Load value 2 into R5
        ADD R6 R1 R2 ; R6 = R1 + R2
        SUB R7 R2 R1 ; R7 = R2 - R1
        MUL R8 R1 R3 ; R8 = R1 * R3
        DIV R9 R2 R3 ; R9 = R2 / R3
        ADD R10 R8 R9 ; R10 = R8 + R9
        SUB R11 R6 R7 ; R11 = R6 - R7
        SWI 2       ; I/O operation (first of two)
        MUL R12 R10 R5 ; R12 = R10 * R5
        DIV R13 R11 R5 ; R13 = R11 / R5
        ADD R14 R12 R13 ; R14 = R12 + R13
        SUB R15 R14 R1 ; R15 = R14 - R1
        MUL R16 R15 R4 ; R16 = R15 * R4
        DIV R17 R16 R4 ; R17 = R16 / R4
        ADD R18 R17 R3 ; R18 = R17 + R3
        SUB R19 R18 R2 ; R19 = R18 - R2
        MUL R20 R19 R5 ; R20 = R19 * R5
        DIV R21 R20 R3 ; R21 = R20 / R3
        ADD R22 R21 R2 ; R22 = R21 + R2
        SUB R23 R22 R1 ; R23 = R22 - R1
        MUL R24 R23 R4 ; R24 = R23 * R4
        SWI 2       ; I/O operation (second of two)
        DIV R25 R24 R5 ; R25 = R24 / R5
        ADD R26 R25 R3 ; R26 = R25 + R3
        SUB R27 R26 R4 ; R27 = R26 - R4
        MUL R0 R27 R1 ; R0 = R27 * R1
        SWI 1       ; Terminate