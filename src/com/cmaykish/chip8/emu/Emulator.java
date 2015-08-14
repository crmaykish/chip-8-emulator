package com.cmaykish.chip8.emu;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.util.Scanner;
import java.util.Stack;

public class Emulator {
    private static final int[] FONT = {
            0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
            0x20, 0x60, 0x20, 0x20, 0x70, // 1
            0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
            0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
            0x90, 0x90, 0xF0, 0x10, 0x10, // 4
            0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
            0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
            0xF0, 0x10, 0x20, 0x40, 0x40, // 7
            0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
            0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
            0xF0, 0x90, 0xF0, 0x90, 0x90, // A
            0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
            0xF0, 0x80, 0x80, 0x80, 0xF0, // C
            0xE0, 0x90, 0x90, 0x90, 0xE0, // D
            0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
            0xF0, 0x80, 0xF0, 0x80, 0x80  // F
    };

    private static final String ROM_PATH = "C:/chip8/";
    private static final String ROM_NAME = "INVADERS";

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

    public Emulator(Display display) {
        // Instantiate local display object
        this.display = display;

        // Load font sprites into memory
        for (int i = 0; i < FONT.length; i++) {
            M[i] = FONT[i];
        }

        // Load ROM into memory from local file
        try {
            byte[] rom = Files.readAllBytes(Paths.get(ROM_PATH + ROM_NAME));
            for (int i = 0; i < rom.length; i++) {
                M[i + 512] = rom[i];
            }
        } catch (IOException e) {
            System.err.println("Invalid rom path.");
            System.exit(0);
        }

    }

    public void emulate() {
        System.err.println("Starting emulation...");

        while (RUNNING) {
            // Debugging output
            System.err.print("PC: " + PC + " | ");
            System.err.print("I: " + hex(I));
            for (int i = 0; i < V.length; i++) {
                System.err.print("V" + i + ": " + Integer.toHexString(V[i]) + " | ");
            }

            // Wait for user input to continue to next step
            new Scanner(System.in).nextLine();

            cycle();
        }

        System.err.println("Stopped emulation.");
    }

    // Read and execute one opcode
    private void cycle(){
        readOpcode();
        executeOpcode();
        draw();
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

        switch (tNibble(op)) {
            case 0x0:
                switch (bByte(op)) {
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
                PC = op & 0x0FFF;
                inc = false;
                break;

            case 0x2:    // 2NNN: Push current counter to stack, jump to NNN
                stack.push(PC);
                PC = op & 0x0FFF;
                inc = false;
                break;

            case 0x3:    // 3XNN: Skip next instruction if VX equals NN
                if (V[opX(op)] == bByte(op)) {
                    PC += 2;
                }
                break;

            case 0x4:   // 4XNN: Skip next instruction if VX is not equal to NN
                if (V[opX(op)] != bByte(op)) {
                    PC += 2;
                }
                break;

            case 0x5:   // 5XY0: Skip the next instruction if VX equals VY
                if (V[opX(op)] == opY(op)) {
                    PC += 2;
                }
                break;

            case 0x6:    // 6XNN: Set VX to NN
                V[opX(op)] = bByte(op);
                break;

            case 0x7:    // 7XNN: Add NN to VX
                V[opX(op)] += bByte(op);
                break;

            case 0x8:
                switch (opY(op)) {
                    case 0x0:   // 8XY0: Set VX to value of VY
                        V[opX(op)] = V[opY(op)];
                        break;
                    case 0x1:   // 8XY1: Set VX to VX OR VY
                        V[opX(op)] = V[opX(op)] | V[opY(op)];
                        break;
                    case 0x2:   // 8XY2: Sets VX to VX AND VY
                        V[opX(op)] = V[opX(op)] & V[opY(op)];
                        break;
                    case 0x3:   // 8XY3: Sets VX to VX XOR VY
                        V[opX(op)] = V[opX(op)] ^ V[opY(op)];
                        break;
                    case 0x4:   // 8XY4: Add VY to VX. Set VF to 1 if there's a carry, 0 when there's not
                        int temp = V[opX(op)] + V[opY(op)];
                        V[opX(op)] = temp & 0xFFFF;
                        if (temp > 0xFFFF){
                            V[0xF] = 0x1;
                        }
                        break;
                    case 0x5:   // 8XY5: Subtract VY from VX. Set VF to 0 if there's a borrow, 1 when there's not
                        V[opX(op)] -= V[opY(op)];
                        if (V[opX(op)] > V[opY(op)]){
                            V[0xF] = 0x1;
                        }
                        break;
                    case 0x6:   // 8XY6: Shift VX right by 1. VF is set to least significant bit before shift
                        V[0xF] = V[opX(op)] & 0x01;
                        V[opX(op)] = V[opX(op)] >> 1;
                        break;
                    case 0x7:   // 8XY7: Set VX to VY - VX. VF is set to 0 when there's a borrow, 1 when there's not
                        V[opX(op)] = V[opY(op)] - V[opX(op)];
                        if (V[opY(op)] > V[opX(op)]){
                            V[0xF] = 0x1;
                        }
                        break;
                    case 0xE:   // 8XYE: Shifts VX left by 1. VF is set to the most significant bit before shift.
                        V[0xF] = V[opX(op)] & 0x80;
                        V[opX(op)] = V[opX(op)] << 1;
                        break;
                }
                break;

            case 0x9:   // 9XY0: Skip the next instruction if VX doesn't equal VY
                if (V[opX(op)] != opY(op)) {
                    PC += 2;
                }
                break;

            case 0xA:    // ANNN: Set I to NNN
                I = op & 0x0FFF;
                break;

            case 0xB:   // BNNN: Jump to NNN plus V0
                PC = (op & 0x0FFF) + V[0];
                inc = false;
                break;

            case 0xC:   // CXNN: Set VX to bitwise AND operation on a random number and NN.
                V[opX(op)] = bByte(op) & new Random().nextInt(0xFF);
                break;

            case 0xD:    // DXYN: Draw...
                // TODO
                break;

            case 0xE:
                switch (bByte(op)) {
                    case 0x9E:  // EX9E: Skip the next instruction if the key stored in VX is pressed.
                        // TODO
                        break;
                    case 0xA1:  // EXA1: Skips the next instruction if the key stored in VX isn't pressed.
                        // TODO
                        break;
                }
                break;

            case 0xF:
                switch (bByte(op)) {
                    case 0x07:    // FX07: Set VX to value of delay timer.
                        V[opX(op)] = delay;
                        break;
                    case 0x0A:    // FX0A: Wait for key press, store key value in VX.
                        // TODO
                        break;
                    case 0x15:  // FX15: Sets the delay timer to VX
                        delay = V[opX(op)];
                        break;
                    case 0x18:  // FX18: Sets the sound timer to VX
                        sound = V[opX(op)];
                        break;
                    case 0x1E:  // FX1E: Add VX to I
                        I += V[opX(op)];
                        break;
                    case 0x29:  // FX29: Set I to the location of the sprite for the character in VX
                        // TODO
                    case 0x33:    // Store binary-coded decimal of VX at I, I+1, I+2
                        M[I] = V[(op & 0x0F00 >> 8)] / 100;
                        M[I + 1] = (V[(op & 0x0F00 >> 8)] / 100) % 10;
                        M[I + 2] = (V[(op & 0x0F00) >> 8] % 100) % 10;
                    case 0x55:    // FX55: Store V0 to VX in memory starting at I
                        for (int n = 0; n < opX(op); n++) { // TODO: < or <=?
                            M[I + n] = V[n];
                        }
                    case 0x65:    // FX65: Fill V0 to VX with memory values starting at address I (I)
                        for (int n = 0; n < opX(op); n++) { // TODO: < or <=?
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

    // Draw the screen
    private void draw() {
        // TODO
    }

    // Return the top nibble (most significant 4 bits) of a 2-byte number
    private int tNibble(int o) {
        return (o & 0xF000) >> 12;
    }

    // Return the second nibble (corresponding to X in many Opcodes) of a 2-byte number
    private int opX(int o) {
        return (o & 0x0F00) >> 8;
    }

    // Return the third nibble (corresponding to Y in many Opcodes) of a 2-byte number
    private int opY(int o){
        return (o & 0x00F0) >> 4;
    }

    // Return the least significant byte of a 2-byte number
    private int bByte(int o) {
        return (o & 0x00FF);
    }

    // Convert an integer to a Hexadecimal display string
    private String hex(int o) {
        return Integer.toHexString(o).toUpperCase();
    }

}