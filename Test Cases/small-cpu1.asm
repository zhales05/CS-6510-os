START   MVI R1 5    ; Load value 5 into R1
        MVI R2 10   ; Load value 10 into R2
        MVI R3 2    ; Load value 2 into R3
        ADD R4 R1 R2 ; R4 = R1 + R2
        SUB R5 R2 R1 ; R5 = R2 - R1
        MUL R6 R1 R3 ; R6 = R1 * R3
        DIV R7 R2 R3 ; R7 = R2 / R3
        ADD R8 R6 R7 ; R8 = R6 + R7
        SUB R9 R4 R5 ; R9 = R4 - R5
        MVI R0 R9    ; Move R9 to R0