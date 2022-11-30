package config;

import java.io.*;
import java.util.*;
import java.util.logging.*;

public class PeerInfo {

    private static final String peerInfoFilename = "PeerInfo.cfg";

    static class Item {
        int id;
        String address;
        int port;
        boolean hasFile;

    }

    private static final Map<Integer, Item> peerMap = new HashMap<>();

    // runtime Logger
    private static final Logger logger = Logger.getLogger(PeerInfo.class.getName());

    static {
        try (BufferedReader reader = new BufferedReader(new FileReader(peerInfoFilename))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                Item item = new Item();
                item.id = Integer.parseInt(parts[0]);
                item.address = parts[1];
                item.port = Integer.parseInt(parts[2]);
                item.hasFile = Integer.parseInt(parts[3]) == 1;
                peerMap.put(item.id, item);
            }
        } catch (Exception e) {
            // fail to load peer info from "PeerInfo.cfg"
            System.exit(1);
        }
    }

    public static String getPeerAddress(int peerID) {
        return peerMap.get(peerID).address;
    }

    public static int getPeerPort(int peerID) {
        return peerMap.get(peerID).port;
    }

    public static boolean doesPeerHaveFile(int peerID) {
        return peerMap.get(peerID).hasFile;
    }

    public static Set<Integer> getPeerIDs() {
        return peerMap.keySet();
    }


}
