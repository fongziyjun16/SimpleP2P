package connect;

import utils.PeerUtils;

import java.io.*;
import java.net.Socket;
import java.util.logging.*;

public class MessageSender {

    // runtime Logger
    private final static Logger logger = Logger.getLogger(MessageSender.class.getName());

    public static void sendHandshake(Socket connection, int selfID) throws IOException {
        // build message
        byte[] message = new byte[32];
        byte[] part1 = "P2PFILESHARINGPROJ".getBytes();
        int index = 0;
        while (index < part1.length) {
            message[index] = part1[index];
            index++;
        }
        index += 10;
        byte[] part3 = PeerUtils.int2Bytes(selfID);
        for (byte b : part3) {
            message[index++] = b;
        }

        // send message
        try {
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(message);
            outputStream.flush();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fail to send handshake message to " +
                    PeerUtils.getConnectionWholeAddress(connection));
            throw e;
        }
    }

    // head includes: length & type
    public static void sendHead(Socket connection, int length, byte type) throws IOException {
        InputStream inputStream = connection.getInputStream();
        OutputStream outputStream = connection.getOutputStream();

        byte[] buffer = new byte[1];

        byte[] lengthInByte = PeerUtils.int2Bytes(length);
        outputStream.write(lengthInByte);
        outputStream.flush();
        inputStream.read(buffer);

        outputStream.write(type);
        outputStream.flush();
        inputStream.read(buffer);
    }

    // choke 0
    public static void sendChoke(Socket connection, int neighborID) {
        try {
            sendHead(connection, 1, (byte) 0);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Fail to Send interested to neighbor " + neighborID);
        }
    }

    // unchoke 1
    public static void sendUnchoke(Socket connection, int neighborID) {
        try {
            sendHead(connection, 1, (byte) 1);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Fail to Send not interested to neighbor " + neighborID);
        }
    }

    // interested 2
    public static void sendInterested(Socket connection, int neighborID) {
        try {
            sendHead(connection, 1, (byte) 2);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Fail to Send interested to neighbor " + neighborID);
        }
    }

    // not interested 3
    public static void sendNotInterested(Socket connection, int neighborID) {
        try {
            sendHead(connection, 1, (byte) 3);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Fail to Send not interested to neighbor " + neighborID);
        }
    }

    // have 4
    public static void sendHave(Socket connection, int neighborID, int index) {
        try {
            sendHead(connection, 5, (byte) 4);
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(PeerUtils.int2Bytes(index));
            outputStream.flush();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Fail to Send not interested to neighbor " + neighborID);
        }
    }

    // bitfield 5
    public static void sendSelfBitfield(Socket connection, int neighborID, byte[] selfBitfield) {
        try {
            sendHead(connection, 1 + selfBitfield.length, (byte) 5);
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(selfBitfield);
            outputStream.flush();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Fail to Send bitfield to neighbor " + neighborID);
        }
    }

    // request 6
    public static void sendRequest(Socket connection, int neighborID, int index) {
        try {
            sendHead(connection, 5, (byte) 6);
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(PeerUtils.int2Bytes(index));
            outputStream.flush();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Fail to Send request to neighbor " + neighborID);
        }
    }

}
