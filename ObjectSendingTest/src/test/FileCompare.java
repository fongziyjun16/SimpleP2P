package test;

import java.io.*;

public class FileCompare {

    public static void main(String[] args) {
        System.out.println(FileCompare.compareFiles("./server/thefile", "./client/thefile"));
    }

    public static boolean compareFiles(String sour, String dest) {
        try (FileInputStream sourInputStream = new FileInputStream(sour);
             FileInputStream destInputStream = new FileInputStream(dest)) {
            int length = 1024;
            byte[] sourInputBuffer = new byte[length];
            byte[] destInputBuffer = new byte[length];
            while (sourInputStream.read(sourInputBuffer) != -1 && destInputStream.read(destInputBuffer) != -1) {
                for (int i = 0; i < 1024; i++) {
                    if (sourInputBuffer[i] != destInputBuffer[i]) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
