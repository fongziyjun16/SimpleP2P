package utils;

import config.Common;

import java.net.Socket;

public class PeerUtils {

    public static String getTargetFilename(int peerID) {
        return "./peer_" + peerID + "/" + Common.fileName;
    }

    public static String getConnectionWholeAddress(Socket connection) {
        return connection == null ? "" : connection.getInetAddress().getHostAddress() + ":" + connection.getPort();
    }

}
