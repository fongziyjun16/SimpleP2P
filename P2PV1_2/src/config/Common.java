package config;

import java.io.*;
import java.util.*;
import java.util.logging.*;

public class Common {

    private static final String commonFilename = "Common.cfg";

    // Common.cfg
    public static final int numberOfPreferredNeighbors;
    public static final int unchokingInterval;
    public static final int optimisticUnchokingInterval;
    public static final String fileName;
    public static final long fileSize;
    public static final long pieceSize;

    // runtime Logger
    private static final Logger logger = Logger.getLogger(Common.class.getName());

    static {
        // load Common.cfg
        Map<String, String> commonInfo = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(commonFilename))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                commonInfo.put(parts[0], parts[1]);
            }
        } catch (Exception e) {
            // fail to load common info from "Common.cfg"
            System.exit(1);
        }
        numberOfPreferredNeighbors = Integer.parseInt(commonInfo.get("NumberOfPreferredNeighbors"));
        unchokingInterval = Integer.parseInt(commonInfo.get("UnchokingInterval"));
        optimisticUnchokingInterval = Integer.parseInt(commonInfo.get("OptimisticUnchokingInterval"));
        fileName = commonInfo.get("FileName");
        fileSize = Long.parseLong(commonInfo.get("FileSize"));
        pieceSize = Long.parseLong(commonInfo.get("PieceSize"));
    }

}
