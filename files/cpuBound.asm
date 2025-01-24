RESULT  .WORD 0
ONE     .WORD 1
MAX     .WORD 10000
MIN     .WORD 0
; This is a CPU bound program, it will increment (0...10,000) then 
; decrement (10,000...0) a register in an infinite loop
      ADR R3 RESULT ; Get address of result
      ADR R5 MAX    ; Get Address of max
	  LDR R5 [R5]   ; Load R5 with 10,000 (MVI R5 10000 could have been used)
      ADR R0 MIN    ; Get address of min
	  LDR R0 [R0]   ; Load R0 with 0 (MVI R0 0 could have been used)
	  MVI R2 0      ; Clear Result register
BEGIN ADR R1 ONE    ; Loop to incrementing from 0 to 10,000
INC   ADD R2 R2 R1  ; Increment result by 1
      STR R2 [R3]   ; Store value in result
	  CMP R2 R5     ; Check if result == max (reg Z has the compare result)
	  BNE INC       ; Loop if not result != max
DEC   SUB R2 R2 R1  ; Decrement result by 1
      STR R2 [R3]   ; Store value in result
	  CMP R2 R0     ; Check if result == min (reg Z has the compare result)
	  BNE DEC       ; Loop if not result != min
      B BEGIN       ; Loop to Beginning (infinite loop)