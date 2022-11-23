package utils;

import config.Common;

public class PeerUtils {

    public static String getTargetFilename(int peerID) {
        return "./peer_" + peerID + "/" + Common.fileName;
    }


}
