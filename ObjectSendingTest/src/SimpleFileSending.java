import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;

public class SimpleFileSending {

    public static void main(String[] args) {
        String sour = "./server/thefile";
        String dest = "./client/thefile";
        long fileSize = 2167705;

        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(11111);
                 RandomAccessFile targetFile = new RandomAccessFile(sour, "r")) {
                Socket socket = server.accept();
                byte[] readBuffer = new byte[1024];
                while (targetFile.read(readBuffer) != -1) {
                    socket.getOutputStream().write(readBuffer);
                }
                socket.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        new Thread(() -> {
            File file = new File(dest);
            if (file.exists()) {
                file.delete();
            }

            try (RandomAccessFile accessFile = new RandomAccessFile(dest, "rw")) {
                if (file.createNewFile()) {
                    accessFile.setLength(fileSize);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try (Socket socket = new Socket("localhost", 11111);
                 RandomAccessFile targetFile = new RandomAccessFile(dest, "rw")) {
                byte[] inputBuffer = new byte[1024];
                while (socket.getInputStream().read(inputBuffer) != -1) {
                    targetFile.write(inputBuffer);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

}
