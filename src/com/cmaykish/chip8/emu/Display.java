package com.cmaykish.chip8.emu;

import java.awt.*;

public class Display extends Frame{
    public static final int WIDTH = 64;
    public static final int HEIGHT = 32;
    public static final int SCALE = 10;

    public int[][] pixels = new int[64][64];
    private Canvas c;

    public Display(){
        c = new Canvas(){
            public void paint(Graphics g){
                Graphics2D g2 = (Graphics2D) g;

                for (int i = 0; i < Display.WIDTH; i++) {
                    for (int j = 0; j < Display.HEIGHT; j++) {
                        if (pixels[i][j] == 1){
                            g2.fillRect(i*Display.SCALE, j*Display.SCALE, Display.SCALE, Display.SCALE);
                        }
                    }
                }
            }
        };

        c.setSize(Display.WIDTH, Display.HEIGHT);
        c.setBackground(Color.BLACK);
        c.setForeground(Color.WHITE);
        add(c);

        setTitle("CHIP-8");
        setSize(720, 380);
        setVisible(true);
    }

    public void clearScreen(){
        // TODO

    }

    public void redraw(){
        c.repaint();
    }

}
