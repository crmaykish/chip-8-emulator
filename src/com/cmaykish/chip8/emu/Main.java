package com.cmaykish.chip8.emu;

import java.awt.*;

public class Main {

    public static void main(String args[]){

        Display display = new Display();
        Emulator emu = new Emulator(display);
        emu.emulate();
    }
}
