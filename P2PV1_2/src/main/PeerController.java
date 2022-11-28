package main;

import config.*;
import connect.PeerRegister;
import utils.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.*;

public class PeerController {

    private final int selfID;

    public static List<ReentrantLock> targetFileSegmentLocks = new ArrayList<>();

    public static ExecutorService threadPool = Executors.newCachedThreadPool();

    // runtime Logger
    private static final Logger logger = Logger.getLogger(PeerController.class.getName());

    public PeerController(int selfID) {
        this.selfID = selfID;
        byte[] selfBitfield = new byte[BitfieldUtils.bitfieldLength];
        if (PeerInfo.doesPeerHaveFile(selfID)) {
            for (int i = 0; i < BitfieldUtils.pieceNumber; i++) {
                BitfieldUtils.received(selfBitfield, i);
            }
        }
        // each piece has one lock
        for (int i = 1; i <= BitfieldUtils.pieceNumber; i++) {
            targetFileSegmentLocks.add(new ReentrantLock());
        }
        fileInitialize();
        new PeerRegister(selfID, selfBitfield);
    }

    private void fileInitialize() {
        try {
            String targetFilename = PeerUtils.getTargetFilename(selfID);
            boolean hasFile = PeerInfo.doesPeerHaveFile(selfID);
            if (!hasFile) {
                // create work directory
                File dir = new File("peer_" + selfID);
                boolean test = dir.exists();
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

    public static void stop() {
        threadPool.shutdownNow();
        logger.log(Level.INFO, "PeerController Stops");
    }

}
