START   MVI R1 5    ; Load value 5 into R1
        MVI R2 10   ; Load value 10 into R2
        ADD R0 R1 R2 ; R0 = R1 + R2
        SWI 4       ; I/O operation
        SUB R0 R2 R1 ; R0 = R2 - R1
        SWI 4       ; I/O operation
