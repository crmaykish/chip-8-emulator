package com.cmaykish.chip8.emu;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.util.Scanner;
import java.util.Stack;
import java.util.Timer;

public class Emulator {
    private static final String ROM_PATH = "C:/chip8/";
    private static final String ROM_NAME = "ibm.ch8";

    private boolean RUNNING = true;

    // Registers
    private int[] V = new int[16];  // 16 8-bit V registers
    private int I = 0;          // One 16-bit Index register
    private int PC = 512;           // One 8-bit Program Counter

    // Memory
    private int[] M = new int[4096];   // CHIP-8 has 4Kb of RAM

    // Timer registers
    private int sound = 0;
    private int delay = 0;

    // Stack
    private Stack<Integer> stack = new Stack<>();

    // Current Opcode
    int op = 0;

    // Display
    private Display display;

    // Timing to control CPU speed and rendering framerate
    private long lastCycleTime = 0;
    private long lastDrawTime = 0;

    public Emulator() {
        display = new Display();
        loadFont();
        loadRom();
    }

    // Load font into memory. Font starts at M[0]
    private void loadFont(){
        for (int i = 0; i < Font.FONT.length; i++) {
            M[i] = Font.FONT[i];
        }
    }

    // Load ROM file into memory. Return true if successful, False if the file can't be found.
    private boolean loadRom(){
        try {
            byte[] rom = Files.readAllBytes(Paths.get(ROM_PATH + ROM_NAME));
            for (int i = 0; i < rom.length; i++) {
                M[i + 512] = rom[i];    // ROM file loads into memory starting at M[512]
            }
            return true;
        } catch (IOException e) {
            System.err.println("Invalid rom path.");
            return false;
        }
    }

    public void emulate() {
        System.err.println("Starting emulation...");

        while (RUNNING) {
            long time = (System.nanoTime() - lastCycleTime) / 10000;
            if (time > 50) { // cycle at 10000 ops/second
                cycle();
                lastCycleTime = System.nanoTime();
            }

            long drawTime = (System.nanoTime() - lastDrawTime) / 10000;
            if (drawTime > 10000){
                display.redraw();
                lastDrawTime = System.nanoTime();
            }
        }

        System.err.println("Stopped emulation.");
    }

    // Read and execute one opcode
    private void cycle(){
        readOpcode();
        executeOpcode();
    }

    // Read the Opcode located at the place in memory indicated by the program counter
    private void readOpcode() {
        int a = M[PC] & 0x000000FF;
        int b = M[PC + 1] & 0x000000FF;
        op = ((a << 8) | b);
    }

    // Execute the current opcode logic
    private void executeOpcode() {
        // Flag to indicate whether program counter should be incremented after opcode or not
        // Default to yes, opcode handlers need to specify if there should be no increment
        boolean inc = true;

        int P = (op & 0xF000) >> 12;
        int X = (op & 0x0F00) >> 8;
        int Y = (op & 0x00F0) >> 4;
        int N = op & 0x000F;
        int NN = op & 0x00FF;
        int NNN = op & 0x0FFF;

        switch (P) {
            case 0x0:
                switch (NN) {
                    case 0xE0:  // 00E0: Clear the screen
                        display.clearScreen();
                        break;
                    case 0xEE:  // 00EE: Return from a subroutine
                        PC = stack.pop();
                        inc = false;
                        break;
                }
                break;

            case 0x1:    // 1NNN: Jump to NNN
                PC = NNN;
                inc = false;
                break;

            case 0x2:    // 2NNN: Push current counter to stack, jump to NNN
                stack.push(PC);
                PC = NNN;
                inc = false;
                break;

            case 0x3:    // 3XNN: Skip next instruction if VX equals NN
                if (V[X] == NN) {
                    PC += 2;
                }
                break;

            case 0x4:   // 4XNN: Skip next instruction if VX is not equal to NN
                if (V[X] != NN) {
                    PC += 2;
                }
                break;

            case 0x5:   // 5XY0: Skip the next instruction if VX equals VY
                if (V[X] == V[Y]) {
                    PC += 2;
                }
                break;

            case 0x6:    // 6XNN: Set VX to NN
                V[X] = NN;
                break;

            case 0x7:    // 7XNN: Add NN to VX
                V[X] += NN;
                break;

            case 0x8:
                switch (N) {
                    case 0x0:   // 8XY0: Set VX to value of VY
                        V[X] = V[Y];
                        break;
                    case 0x1:   // 8XY1: Set VX to VX OR VY
                        V[X] = V[X] | V[Y];
                        break;
                    case 0x2:   // 8XY2: Sets VX to VX AND VY
                        V[X] = V[X] & V[Y];
                        break;
                    case 0x3:   // 8XY3: Sets VX to VX XOR VY
                        V[X] = V[X] ^ V[Y];
                        break;
                    case 0x4:   // 8XY4: Add VY to VX. Set VF to 1 if there's a carry, 0 when there's not
                        int temp = V[X] + V[Y];
                        V[X] = temp & 0xFFFF;
                        if (temp > 0xFFFF){
                            V[0xF] = 0x1;
                        }
                        break;
                    case 0x5:   // 8XY5: Subtract VY from VX. Set VF to 0 if there's a borrow, 1 when there's not
                        V[X] -= V[Y];
                        if (V[X] > V[Y]){
                            V[0xF] = 0x1;
                        }
                        break;
                    case 0x6:   // 8XY6: Shift VX right by 1. VF is set to least significant bit before shift
                        V[0xF] = V[X] & 0x01;
                        V[X] = V[X] >> 1;
                        break;
                    case 0x7:   // 8XY7: Set VX to VY - VX. VF is set to 0 when there's a borrow, 1 when there's not
                        V[X] = V[Y] - V[X];
                        if (V[Y] > V[X]){
                            V[0xF] = 0x1;
                        }
                        break;
                    case 0xE:   // 8XYE: Shifts VX left by 1. VF is set to the most significant bit before shift.
                        V[0xF] = V[X] & 0x80;
                        V[X] = V[X] << 1;
                        break;
                }
                break;

            case 0x9:   // 9XY0: Skip the next instruction if VX doesn't equal VY
                if (V[X] != V[Y]) {
                    PC += 2;
                }
                break;

            case 0xA:    // ANNN: Set I to NNN
                I = NNN;
                break;

            case 0xB:   // BNNN: Jump to NNN plus V0
                PC = NNN + V[0];
                inc = false;
                break;

            case 0xC:   // CXNN: Set VX to bitwise AND operation on a random number and NN.
                V[X] = NN & new Random().nextInt(0xFF);
                break;

            case 0xD:
                int[] sprite = new int[N]; // read N bytes from mem
                for (int i = 0; i < N; i++) {
                    sprite[i] = M[I + i] & 0xFF;
                }

                int x = V[X];
                int y = V[Y];

                for (int i = 0; i < N; i++) {   // each byte/row
                    int row = sprite[i];
                    for (int j = 0; j < 8; j++) { // each pixel in the row
                        int pixel = row & (0x80 >> j);
                        if (pixel > 0){
                            display.pixels[x+j][y+i] ^= 1;
                            V[0xF] = 1;
                        }
                    }
                }

                break;

            case 0xE:
                switch (NN) {
                    case 0x9E:  // EX9E: Skip the next instruction if the key stored in VX is pressed.
                        // TODO
                        System.err.println("Opcode not implemented: EX9E");
                        break;
                    case 0xA1:  // EXA1: Skips the next instruction if the key stored in VX isn't pressed.
                        // TODO
                        System.err.println("Opcode not implemented: EXA1");

                        break;
                }
                break;

            case 0xF:
                switch (NN) {
                    case 0x07:    // FX07: Set VX to value of delay timer.
                        V[X] = delay;
                        break;
                    case 0x0A:    // FX0A: Wait for key press, store key value in VX.
                        // TODO
                        System.err.println("Opcode not implemented: FX0A");
                        break;
                    case 0x15:  // FX15: Sets the delay timer to VX
                        delay = V[X];
                        break;
                    case 0x18:  // FX18: Sets the sound timer to VX
                        sound = V[X];
                        break;
                    case 0x1E:  // FX1E: Add VX to I
                        I += V[X];
                        break;
                    case 0x29:  // FX29: Set I to the location of the sprite for the character in VX
                        I = V[X] * 5;
                    case 0x33:    // Store binary-coded decimal of VX at I, I+1, I+2
                        M[I] = V[(op & 0x0F00 >> 8)] / 100;
                        M[I + 1] = (V[(op & 0x0F00 >> 8)] / 100) % 10;
                        M[I + 2] = (V[(op & 0x0F00) >> 8] % 100) % 10;
                    case 0x55:    // FX55: Store V0 to VX in memory starting at I
                        for (int n = 0; n <= X; n++) {
                            M[I + n] = V[n];
                        }
                    case 0x65:    // FX65: Fill V0 to VX with memory values starting at address I (I)
                        for (int n = 0; n <= X; n++) {
                            V[n] = M[I + n];
                        }
                    default:
                        break;
                }
                break;

            default:
                System.err.println("Bad Opcode!");
                RUNNING = false;
                break;
        }

        if (inc) {
            PC += 2;
        }
    }

    private void debug(){
        System.err.print(hex(op) + " > ");
        System.err.print("PC: " + hex(PC) + " | ");
        System.err.print("I: " + hex(I) + " | ");
        for (int i = 0; i < V.length; i++) {
            System.err.print("V" + hex(i) + ": " + hex(V[i]) + " | ");
        }
        System.err.println("\n");
    }

    // Convert an integer to a Hexadecimal display string
    private String hex(int o) {
        return Integer.toHexString(o).toUpperCase();
    }

}

