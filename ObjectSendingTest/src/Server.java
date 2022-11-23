import dto.*;

import java.io.*;
import java.net.*;

public class Server {

    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(10010)) {
            while (true) {
                Socket socket = server.accept();
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

//                HandshakeMessage handshakeMessage = (HandshakeMessage) ois.readObject();
//                System.out.println(handshakeMessage);

                ActualMessage actualMessage = (ActualMessage) ois.readObject();
                System.out.println(new String(actualMessage.getPayload()));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
