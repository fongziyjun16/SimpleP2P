package main;

import config.*;
import utils.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.*;

public class PeerHandler {

    private final int selfID;

    private final byte[] selfBitfield;
    private final ReentrantReadWriteLock selfBitfieldLock = new ReentrantReadWriteLock();

    private final List<ReentrantLock> targetFileSegmentLocks = new ArrayList<>();

    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    // runtime Logger
    private final static Logger logger = Logger.getLogger(PeerHandler.class.getName());

    public PeerHandler(int selfID) {
        this.selfID = selfID;

        selfBitfield = new byte[BitfieldUtils.bitfieldLength];
        if (PeerInfo.doesPeerHaveFile(selfID)) {
            for (int i = 0; i < BitfieldUtils.pieceNumber; i++) {
                BitfieldUtils.received(selfBitfield, i);
            }
        }

        for (int i = 0; i < BitfieldUtils.pieceNumber; i++) {
            targetFileSegmentLocks.add(new ReentrantLock());
        }

        fileInitialize();

        new PeerHub(this);
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

    public void stopPeerProcess() {
        logger.log(Level.INFO, "Stop & Exit");
        System.exit(0);
    }

    public int getSelfID() {
        return selfID;
    }

    public byte[] getSelfBitfield() {
        return selfBitfield;
    }

    public ReentrantReadWriteLock getSelfBitfieldLock() {
        return selfBitfieldLock;
    }

    public List<ReentrantLock> getTargetFileSegmentLocks() {
        return targetFileSegmentLocks;
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

}
