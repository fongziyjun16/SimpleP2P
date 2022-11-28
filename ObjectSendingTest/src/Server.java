import dto.*;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.logging.*;

public class Server {

    private static final Logger logger = Logger.getLogger(Server.class.getName());

    public static void main(String[] args) {
//        randomAccessFileTest();
//        objectTest();
        connectTest();
    }

    private static void randomAccessFileTest() {
        // FileSize 2167705
        // PieceSize 16384
        String targetFilename = "./server/thefile";
        long fileSize = 2167705;
        long pieceSize = 16384;
        long pieceNumber = fileSize / pieceSize + (fileSize % pieceSize == 0 ? 0 : 1);

        try (ServerSocket server = new ServerSocket(10010);
             RandomAccessFile targetFile = new RandomAccessFile(targetFilename, "r")) {
            while (true) {
                Socket socket = server.accept();
                InputStream inputStream = socket.getInputStream();
                OutputStream outputStream = socket.getOutputStream();

                byte[] inputBuffer = new byte[1024];
                inputStream.read(inputBuffer);
                byte[] indexInByte = new byte[4];
                System.arraycopy(inputBuffer, 0, indexInByte, 0, 4);
                int index = ByteBuffer.wrap(indexInByte).getInt();
                logger.log(Level.INFO, "response piece " + index);

                long contentLength = index == pieceNumber - 1 ? (fileSize - pieceSize * index): pieceSize;
                long readLength = 0;
                byte[] readBuffer = new byte[1024];
                while ((readLength = targetFile.read(readBuffer)) != -1 && contentLength != 0) {
                    if (contentLength > readLength) {
                        outputStream.write(readBuffer);
                    } else {
                        outputStream.write(readBuffer, 0, (int) contentLength);
                    }
                    contentLength -= readLength;
                }
                logger.log(Level.INFO, "sent piece " + index);
                socket.close();
            }
        } catch (IOException e) {
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