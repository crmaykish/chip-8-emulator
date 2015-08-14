package com.cmaykish.chip8.emu;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.Stack;

public class Emulator {
    private boolean RUNNING = true;

    // registers
    int[] v = new int[16];
    int index = 0;
    int counter = 512;

    // memory
    int[] memory = new int[4096]; // 4k

    // stack
    Stack<Integer> stack = new Stack<Integer>();

    // current opcode
    int opcode = 0;

    // display
    // boolean[][] display = new boolean[64][32];

    public void start() {
        System.err.println("Starting emulation...");

        init();

        while (RUNNING) {

            //debugging
            for (int i = 0; i < v.length; i++) {
                System.err.print("v" + i + ": " + Integer.toHexString(v[i]) + " | ");
            }

            new Scanner(System.in).nextLine();    // step

            readOpcode();
            executeOpcode();


        }

        System.err.println("Stopped emulation.");
    }

    private void init() {
        // load font sprites
        initHexSprites();

        // load rom into memory
        try {
            byte[] rom = Files.readAllBytes(Paths.get("C:/chip8/INVADERS"));

            for (int i = 0; i < rom.length; i++) {
                memory[i + 512] = rom[i];
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void readOpcode() {
        int a = memory[counter] & 0x000000FF;
        int b = memory[counter + 1] & 0x000000FF;

        opcode = ((a << 8) | b);

        System.err.println("Opcode: " + Integer.toHexString(opcode).toUpperCase());
    }

    private void executeOpcode() {
        boolean inc = true;

        switch (getTop4Bits(opcode)) {
            case 0x0:
                switch (getBottomByte(opcode)){
                    case 0xE0:  // 00E0: Clear the screen

                        break;
                    case 0xEE:  // 00EE: Return from a subroutine

                        break;
                }
                break;

            case 0x1:    // 1NNN: Jump to NNN
                counter = opcode & 0x0FFF;
                break;

            case 0x2:    // 2NNN: Push current counter to stack, jump to NNN
                stack.push(counter);
                counter = opcode & 0x0FFF;
                break;

            case 0x3:    // 3XNN: Skip next instruction if VX equals NN
                if (v[getSecond4Bits(opcode)] == getBottomByte(opcode)) {
                    counter += 2;
                }
                break;

            case 0x4:   // 4XNN: Skip next instruction if VX is not equal to NN
                if (v[getSecond4Bits(opcode)] != getBottomByte(opcode)) {
                    counter += 2;
                }
                break;

            case 0x5:   // 5XY0: Skip the next instruction if VX equals VY
                if (v[getSecond4Bits(opcode)] == (getBottomByte(opcode) & 0xF0 >> 4)) {
                    counter += 2;   // TODO: test this!
                }
                break;

            case 0x6:    // 6XNN: Set VX to NN
                v[getSecond4Bits(opcode)] = getBottomByte(opcode);
                break;

            case 0x7:    // 7XNN: Add NN to VX
                v[getSecond4Bits(opcode)] += getBottomByte(opcode);
                break;

            case 0x8:
                switch (getBottomByte(opcode) & 0x0F >> 4){
                    case 0x0:   // 8XY0: Set VX to value of VY
                        break;
                    case 0x1:   // 8XY1: Set VX to VX or VY
                        break;
                    case 0x2:   // 8XY2: Sets VX to VX and VY
                        break;
                    case 0x3:   // 8XY3: Sets VX to VX xor VY
                        break;
                    case 0x4:   // 8XY4: Add VY to VX. Set VF to 1 if there's a carry, 0 when there's not
                        break;
                    case 0x5:   // 8XY5: Subtract VY from VX. Set VF to 0 if there's a borrow, 1 when there's not
                        break;
                    case 0x6:   // 8XY6: Shift VX right by 1. VF is set to least significant bit before shift
                        break;
                    case 0x7:   // 8XY7: Set VX to VY - VX. VF is set to 0 when there's a borrow, 1 when there's not
                        break;
                    case 0xE:   // 8XYE: Shifts VX left by 1. VF is set to the most significant bit before shift.
                        break;
                }
                break;

            case 0x9:   // 9XY0: Skip the next instruction if VX doesn't equal VY
                break;

            case 0xA:    // ANNN: Set I to NNN
                index = opcode & 0x0FFF;
                writeHexValue(index);
                break;

            case 0xB:   // BNNN: Jump to NNN plus V0
                break;

            case 0xC:   // CXNN: Set VX to bitwise AND operation on a random number and NN.
                break;

            case 0xD:    // DXYN: Draw...
                break;

            case 0xE:
                switch(getBottomByte(opcode)){
                    case 0x9E:  // EX9E: Skip the next instruction if the key stored in VX is pressed.
                        break;
                    case 0xA1:  // EXA1: Skips the next instruction if the key stored in VX isn't pressed.
                        break;
                }
                break;

            case 0xF:
                switch (getBottomByte(opcode)) {
                    case 0x07:    // FX07: Set VX to value of delay timer.

                        break;
                    case 0x0A:    // FX0A: Wait for key press, store key value in VX.
                        int x = getSecond4Bits(opcode);
                        v[x] = 1; // TODO: dummy value... get keyboard input
                        break;
                    case 0x15:  // FX15: Sets the delay timer to VX
                        break;
                    case 0x18:  // FX18: Sets the sound timer to VX
                        break;
                    case 0x1E:  // FX1E: Add VX to I
                        break;
                    case 0x29:
                        writeHexValue(v[getSecond4Bits(opcode)]);
                    case 0x33:    // Store binary-coded decimal of VX at I, I+1, I+2
                        memory[index] = v[(opcode & 0x0F00 >> 8)] / 100;
                        memory[index + 1] = (v[(opcode & 0x0F00 >> 8)] / 100) % 10;
                        memory[index + 2] = (v[(opcode & 0x0F00) >> 8] % 100) % 10;
                    case 0x55:    // FX55: Store V0 to VX in memory starting at I
                        for (int n = 0; n < getSecond4Bits(opcode); n++) { // TODO: < or <=?
                            memory[index + n] = v[n];
                        }
                    case 0x65:    // FX65: Fill V0 to VX with memory values starting at address I (index)
                        for (int n = 0; n < getSecond4Bits(opcode); n++) { // TODO: < or <=?
                            v[n] = memory[index + n];    // TODO: ??
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
            counter += 2;
        }
    }

    private int getTop4Bits(int o) {
        return (o & 0xF000) >> 12;
    }

    private int getSecond4Bits(int o) {
        return (o & 0x0F00) >> 8;
    }

    private int getTopByte(int o) {
        return (o & 0xFF00) >> 8;
    }

    private int getBottomByte(int o) {
        return (o & 0x00FF);
    }

    private void writeHexValue(int o) {
        System.err.println(Integer.toHexString(o).toUpperCase());
    }

    private void initHexSprites() {
        int[] font = {
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
                0xF0, 0x80, 0xF0, 0x80, 0x80 // F
        };

        for (int i = 0; i < font.length; i++) {
            memory[i] = font[i];
        }

    }

}