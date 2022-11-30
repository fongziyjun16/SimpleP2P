package test;

import dto.*;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.*;

public class Client {

    private static final Logger logger = Logger.getLogger(Client.class.getName());

    public static void main(String[] args) {
        randomAccessFileTest();
//        objectTest();
//        connectTest();
    }

    private static void randomAccessFileTest() {
        // FileSize 2167705
        // PieceSize 16384
        String targetFilename = "./client/thefile";
        long fileSize = 2167705;
        long pieceSize = 16384;

        // FileSize 24301474
        // PieceSize 16384
//        String targetFilename = "./client/tree.jpg";
//        long fileSize = 24301474;
//        long pieceSize = 16384;

        long pieceNumber = fileSize / pieceSize + (fileSize % pieceSize == 0 ? 0 : 1);

        File file = new File(targetFilename);
        if (file.exists()) {
            file.delete();
        }
        try {
            if (file.createNewFile()) {
                RandomAccessFile accessFile = new RandomAccessFile(targetFilename, "rw");
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

        for (int index : indices) {
            logger.log(Level.INFO, "request piece " + index);
            try (Socket socket = new Socket("localhost", 10010);
                 RandomAccessFile targetFile = new RandomAccessFile(targetFilename, "rwd")) {
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

                oos.writeObject(new PieceInfoMessage(index));

                targetFile.seek(index * pieceSize);
                logger.log(Level.INFO, "File Pointer " + targetFile.getFilePointer());

                int inputLength;
                byte[] inputBuffer = new byte[1024];
                while ((inputLength = socket.getInputStream().read(inputBuffer)) != -1) {
                    targetFile.write(inputBuffer, 0, inputLength);
                    logger.log(Level.INFO, "File Pointer " + targetFile.getFilePointer());
                }

                logger.log(Level.INFO, "received piece " + index);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void objectTest() {
        try (Socket socket = new Socket("localhost", 10010);) {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            oos.writeObject(new HandshakeMessage(1001));
            oos.writeObject(new ActualMessage(18, 1, "Hello".getBytes(StandardCharsets.UTF_8)));
            Object o = ois.readObject();
            System.out.println();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void connectTest() {
        try {
            Socket socket = new Socket("localhost", 10010);
            InputStream inputStream = socket.getInputStream();
            int inputLength;
            byte[] inputBuffer = new byte[1024];
            while ((inputLength = inputStream.read(inputBuffer)) != -1) {

            }
            System.out.println(inputLength);
            System.out.println("close");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
