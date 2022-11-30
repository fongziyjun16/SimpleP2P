package test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CodeTesting {

    public static void main(String[] args) {
        String sour = "./server/thefile";
        String dest = "./client/thefile";

        long fileSize = 2167705;
        long pieceSize = 16384;

//        String sour = "./server/tree.jpg";
//        String dest = "./client/test.jpg";
//
//        long fileSize = 24301474;
//        long pieceSize = 16384;

        long pieceNumber = fileSize / pieceSize + (fileSize % pieceSize == 0 ? 0 : 1);

        File file = new File(dest);
        if (file.exists()) {
            file.delete();
        }
        try {
            if (file.createNewFile()) {
                RandomAccessFile accessFile = new RandomAccessFile(dest, "rw");
                accessFile.setLength(fileSize);
                accessFile.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < pieceNumber; i++) {
            indices.add(i);
        }
        Random random = new Random();
        for (int i = 0; i < indices.size(); i++) {
            int temp = indices.get(i);
            int next = random.nextInt(indices.size());
            indices.set(i, indices.get(next));
            indices.set(next, temp);
        }

        try (RandomAccessFile sourFile = new RandomAccessFile(sour, "r");
             RandomAccessFile destFile = new RandomAccessFile(dest, "rwd")) {
            for (int index : indices) {
                sourFile.seek(index * pieceSize);
                destFile.seek(index * pieceSize);
                long contentLength = index == pieceNumber - 1 ? (fileSize - pieceSize * index) : pieceSize;
                int readLength;
                byte[] readBuffer = new byte[1024];
                while ((readLength = sourFile.read(readBuffer)) != -1) {
                    if (contentLength > readLength) {
                        destFile.write(readBuffer);
                        contentLength -= readLength;
                    } else {
                        destFile.write(readBuffer, 0, (int) contentLength);
                        contentLength = 0;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
