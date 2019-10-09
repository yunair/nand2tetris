// Hack Loop Demo
@i
M=1

@sum
M=0

(LOOP)
@i
D=M

@100
D=D-A   // D=i-100

@END
D;JGT   // if (i-100) > 0 goto end;

@i
D=M     // D = i
@sum
M=D+M   // sum = sum + i
@i
M=M+1  // i = i + 1
@LOOP
0;JMP

(END)
@END
0;JMP  // 无限循环

