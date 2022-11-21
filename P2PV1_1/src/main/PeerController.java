package main;

import config.*;
import connect.PeerRegister;
import utils.*;

import java.io.*;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.logging.*;

public class PeerController {

    private final int selfID;
    private final byte[] selfBitfield;

    public static ExecutorService threadPool = Executors.newCachedThreadPool();

    public static final Integer[] targetFileSegmentLock = new Integer[BitfieldUtils.bitfieldLength];

    // runtime Logger
    private static final Logger logger = Logger.getLogger(PeerController.class.getName());

    public PeerController(int selfID) {
        this.selfID = selfID;
        selfBitfield = new byte[BitfieldUtils.bitfieldLength];
        if (PeerInfo.doesPeerHaveFile(selfID)) {
            Arrays.fill(selfBitfield, (byte) -1);
        }
        initialize();
        for (int i = 0; i < BitfieldUtils.bitfieldLength; i++) {
            targetFileSegmentLock[i] = 0;
        }
    }

    private void initialize() {
        fileInitialize();
        new PeerRegister(selfID, selfBitfield);
    }

    private void fileInitialize() {
        try {
            String targetFilename = PeerUtils.getTargetFilename(selfID);
            boolean hasFile = PeerInfo.doesPeerHaveFile(selfID);
            if (!hasFile) {
                // create work directory
                File dir = new File(String.valueOf(selfID));
                if (!dir.exists() && !dir.mkdir()) {
                    throw new RuntimeException("Fail to create directory");
                }

                // create empty file with specific length if not exists
                File file = new File(targetFilename);
                if (file.exists()) {
                    file.delete();
                }
                if (file.createNewFile()) {
                    RandomAccessFile accessFile = new RandomAccessFile(targetFilename, "rw");
                    accessFile.setLength(Common.fileSize);
                    accessFile.close();
                } else {
                    throw new RuntimeException("Fail to create empty file for receiving");
                }
            } else {
                File file = new File(targetFilename);
                if (!file.exists()) {
                    throw new RuntimeException("This peer should have the target file, but the target file doesn't exist");
                }
            }

            File logFile = new File("log_peer_" + selfID + ".log");
            if (logFile.exists()) {
                logFile.delete();
            }
            if (!logFile.createNewFile()) {
                throw new RuntimeException("Fail to create log file");
            }
        } catch (Exception e) {
            System.exit(1);
        }
    }

}
