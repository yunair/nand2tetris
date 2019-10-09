// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Fill.asm

// Runs an infinite loop that listens to the keyboard input.
// When a key is pressed (any key), the program blackens the screen,
// i.e. writes "black" in every pixel;
// the screen should remain fully black as long as the key is pressed. 
// When no key is pressed, the program clears the screen, i.e. writes
// "white" in every pixel;
// the screen should remain fully clear as long as no key is pressed.

// Put your code here.
// Hack Loop Demo



(LOOP0)
@j
M=1

@SCREEN
D=A
@address
M=D 

@KBD
D=M

@White
D;JEQ

@Black
D;JGT

    (LOOP1)
    @j
    D=M

    @8192
    D=D-A   // D=i-8192

    @LOOP0
    D;JGT 

    (Black)
    @address
    A=M
    M=-1

    @After
    0;JMP

    (White)
    @address
    A=M
    M=0

    @After
    0;JMP

    (After)
    @j
    M=M+1  // i = i + 1

    @1
    D=A
    
    @address
    M=D+M // address += 1

    @LOOP1
    0;JMP

@LOOP0
0;JMP  // 无限循环

