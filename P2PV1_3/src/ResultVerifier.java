import config.*;

import java.io.*;
import java.util.*;
import java.util.logging.*;

public class ResultVerifier {

    // runtime Logger
    private static final Logger logger = Logger.getLogger(ResultVerifier.class.getName());

    public static void main(String[] args) {
        List<Integer> owners = new ArrayList<>();
        List<Integer> customers = new ArrayList<>();
        int oneOwner = -1;
        for (int peerID : PeerInfo.getPeerIDs()) {
            if (PeerInfo.doesPeerHaveFile(peerID)) {
                owners.add(peerID);
                if (oneOwner == -1) {
                    oneOwner = peerID;
                }
            } else {
                customers.add(peerID);
            }
        }

        for (int i = 0; i < owners.size(); i++) {
            String fileA = getFilename(owners.get(i));
            for (int j = i + 1; j < owners.size(); j++) {
                String fileB = getFilename(owners.get(j));
                if (!compareFiles(fileA, fileB)) {
                    logger.log(Level.SEVERE, "ERROR!!! The specific file has to be same.");
                    return;
                }
            }
        }

        String fileA = getFilename(oneOwner);
        for (int customer : customers) {
            String fileB = getFilename(customer);
            if (!compareFiles(fileA, fileB)) {
                logger.log(Level.SEVERE, "ERROR!!! The files received are broken.");
                return;
            }
        }

        logger.log(Level.INFO, "Success!!! All files pass verification.");
    }

    public static boolean compareFiles(String fileA, String fileB) {
        try (FileInputStream sourInputStream = new FileInputStream(fileA);
             FileInputStream destInputStream = new FileInputStream(fileB)) {
            int length = 1024;
            byte[] sourInputBuffer = new byte[length];
            byte[] destInputBuffer = new byte[length];
            while (sourInputStream.read(sourInputBuffer) != -1 && destInputStream.read(destInputBuffer) != -1) {
                for (int i = 0; i < 1024; i++) {
                    if (sourInputBuffer[i] != destInputBuffer[i]) {
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fail to Compare file " + fileA + " and " + fileB +
                    ". Exception Message: " + e.getMessage());
            return false;
        }
        return true;
    }

    public static String getFilename(int peerID) {
        return "./peer_" + peerID + "/" + Common.fileName;
    }

}
