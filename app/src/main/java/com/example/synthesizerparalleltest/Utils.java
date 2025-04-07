package com.example.synthesizerparalleltest;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;

public class Utils {
    public static void writeToFile(String filename, ArrayList<String> contents) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
            for (String line : contents) {
                bw.write(line);
                bw.newLine();
            }
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
