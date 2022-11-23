package main;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.*;

public class PeerLogger {

    // runtime logger
    private static final Logger logger = Logger.getLogger(PeerLogger.class.getName());

    private static void write(int selfID, String logInfo) {
        String filename = "log_peer_" + selfID + ".log";
        synchronized (PeerLogger.class) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true))) {
                writer.write(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-yyy HH:mm:ss")) + ": " + logInfo);
                writer.newLine();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Fail to add log info to log file");
            }
        }
    }

    // TCP connection log
    public static void makeConnection(int selfID, int peerID2) {
        write(selfID, "Peer " + selfID + " makes a connection to Peer " + peerID2 + ".");
    }

    public static void receiveConnection(int selfID, int peerID2) {
        write(selfID, "Peer " + selfID + " is connected from Peer " + peerID2 + ".");
    }

    // change of preferred neighbors
    public static void changePreferredNeighbors(int selfID, List<Integer> neighbors) {
        write(selfID, "Peer " + selfID + " has the preferred neighbors " + neighbors + ".");
    }

    // change of optimistically unchoked neighbor
    public static void changeOptimisticallyUnchokedNeighbor(int selfID, int optimisticallyUnchokedNeighborID) {
        write(selfID, "Peer " + selfID + " has the preferred neighbors " +
                optimisticallyUnchokedNeighborID + ".");
    }

    // unchoking
    public static void unchoking(int selfID, int peerID2) {
        write(selfID, "Peer " + selfID + " is unchoked by Peer " + peerID2 + ".");
    }

    // choking
    public static void choking(int selfID, int peerID2) {
        write(selfID, "Peer " + selfID + " is choked by Peer " + peerID2 + ".");
    }

    // receiving 'have' message
    public static void have(int selfID, int peerID2, int pieceIndex) {
        write(selfID, "Peer " + selfID + " received the 'have' message from " +
                peerID2 + " for the piece" + pieceIndex + ".");
    }

    // receiving 'interested' message
    public static void interested(int selfID, int peerID2) {
        write(selfID, "Peer " + selfID + " received the 'interested' message from " + peerID2 + ".");
    }

    // receiving 'not interested' message
    public static void notInterested(int selfID, int peerID2) {
        write(selfID, "Peer " + selfID + " received the 'not interested' message from " + peerID2 + ".");
    }

    // downloading one piece
    public static void downloadingOnePiece(int selfID, int peerID2, int pieceIndex, int numberOfPieces) {
        write(selfID, "Peer " + selfID + " has downloaded the piece " + pieceIndex + " from " + peerID2 + "."
                + "Now the number of pieces it has is " + numberOfPieces + ".");
    }

    // completion of download
    public static void completionOfDownload(int selfID) {
        write(selfID, "Peer " + selfID + " has downloaded the complete file.");
    }

}
