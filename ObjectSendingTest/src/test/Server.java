package test;

import dto.*;

import java.io.*;
import java.net.*;
import java.util.logging.*;

public class Server {

    private static final Logger logger = Logger.getLogger(Server.class.getName());

    public static void main(String[] args) {
        randomAccessFileTest();
//        objectTest();
//        connectTest();
    }

    private static void randomAccessFileTest() {
        // FileSize 2167705
        // PieceSize 16384
        String targetFilename = "./server/thefile";
        long fileSize = 2167705;
        long pieceSize = 16384;

        // FileSize 24301474
        // PieceSize 16384
//        String targetFilename = "./server/tree.jpg";
//        long fileSize = 24301474;
//        long pieceSize = 16384;

        long pieceNumber = fileSize / pieceSize + (fileSize % pieceSize == 0 ? 0 : 1);

        try (ServerSocket server = new ServerSocket(10010);
             RandomAccessFile targetFile = new RandomAccessFile(targetFilename, "r")) {
            while (true) {
                Socket socket = server.accept();
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

                PieceInfoMessage pieceInfoMessage = (PieceInfoMessage) ois.readObject();
                int index = pieceInfoMessage.getIndex();
                logger.log(Level.INFO, "ready to response piece " + index);

                targetFile.seek(index * pieceSize);
                logger.log(Level.INFO, "File Pointer " + targetFile.getFilePointer());

                long contentLength = index == pieceNumber - 1 ? (fileSize - pieceSize * index) : pieceSize;
                long readLength = 0;
                byte[] readBuffer = new byte[1024];
                while ((readLength = targetFile.read(readBuffer)) != -1 && contentLength != 0) {
                    if (contentLength > readLength) {
                        socket.getOutputStream().write(readBuffer);
                        contentLength -= readLength;
                    } else {
                        socket.getOutputStream().write(readBuffer, 0, (int) contentLength);
                        contentLength = 0;
                    }
                    logger.log(Level.INFO, "File Pointer " + targetFile.getFilePointer());
                }

                logger.log(Level.INFO, "sent piece " + index);
                socket.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void objectTest() {
        try (ServerSocket server = new ServerSocket(10010)) {
            while (true) {
                Socket socket = server.accept();
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

                HandshakeMessage handshakeMessage = (HandshakeMessage) ois.readObject();
                System.out.println(handshakeMessage);

                ActualMessage actualMessage = (ActualMessage) ois.readObject();
                System.out.println(new String(actualMessage.getPayload()));

                System.out.println();
                socket.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void connectTest() {
        try (ServerSocket server = new ServerSocket(10010)) {
            Socket socket = server.accept();
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}