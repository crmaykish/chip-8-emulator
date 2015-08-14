package com.cmaykish.chip8.emu;

import java.awt.*;

public class Display extends Frame{
    private boolean[][] pixels = new boolean[64][32];
    private Canvas c;

    public Display(){
        c = new Canvas(){
            public void paint(Graphics g){
                Graphics2D g2 = (Graphics2D) g;

                for (int i = 0; i < 64; i++) {
                    for (int j = 0; j < 32; j++) {
                        if (pixels[i][j]){
                            g2.fillRect(i*10, j*10, 10, 10);
                        }
                    }
                }
            }
        };

        c.setSize(640, 320);
        c.setBackground(Color.BLACK);
        c.setForeground(Color.WHITE);
        add(c);

        setTitle("CHIP-8");
        setSize(640, 320);
        setVisible(true);
    }

    public void clearScreen(){
        // TODO
    }

    public void redraw(){
        c.repaint();
    }

}
