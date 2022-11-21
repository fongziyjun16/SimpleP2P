package utils;

import config.Common;

import java.net.Socket;
import java.nio.ByteBuffer;

public class PeerUtils {

    public static byte[] int2Bytes(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    public static int bytes2Int(byte[] value) {
        return ByteBuffer.wrap(value).getInt();
    }

    public static byte[] long2Bytes(long value) {
        return ByteBuffer.allocate(Long.BYTES).putLong(value).array();
    }

    public static String getConnectionWholeAddress(Socket connection) {
        return connection == null ? "" : connection.getInetAddress().getHostAddress() + ":" + connection.getPort();
    }

    public static String bytes2String(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte oneByte : bytes) {
            for (int i = 7; i >= 0; i--) {
                sb.append((oneByte & (1 << i)) == 0 ? '0' : '1');
            }
        }
        return sb.toString();
    }

    public static String getTargetFilename(int peerID) {
        return "./peer_" + peerID + "/" + Common.fileName;
    }

}
