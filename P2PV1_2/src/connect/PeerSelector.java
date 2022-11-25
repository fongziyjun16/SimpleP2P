package connect;

import config.Common;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

public class PeerSelector {

    static class DownloadRateTableEntry {

        int id;
        long amount;
        public DownloadRateTableEntry(int id) {
            this.id = id;
            amount = 0;
        }

    }
    private final PeerRegister peerRegister;

    private final Map<Integer, DownloadRateTableEntry> downloadRateTable = new HashMap<>();

    private final Set<Integer> interestedNeighbors = new HashSet<>();
    private final Set<Integer> unchokeNeighbors = new HashSet<>();
    private final int[] optimisticallyUnchokeNeighbors = {-1};

    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(4);

    private final static Random random = new Random();
    // runtime Logger
    private final static Logger logger = Logger.getLogger(PeerSelector.class.getName());

    public PeerSelector(PeerRegister peerRegister) {
        this.peerRegister = peerRegister;

        scheduler.scheduleWithFixedDelay(() -> {

        }, 0, Common.unchokingInterval, TimeUnit.SECONDS);

        scheduler.scheduleWithFixedDelay(() -> {

        }, 0, Common.optimisticUnchokingInterval, TimeUnit.SECONDS);
    }

    public void addInterested(int neighborID) {
        synchronized (this) {
            interestedNeighbors.add(neighborID);
            downloadRateTable.put(neighborID, new DownloadRateTableEntry(neighborID));
        }
    }

    public void removeInterested(int neighborID) {
        synchronized (this) {
            interestedNeighbors.remove(neighborID);
            unchokeNeighbors.remove(neighborID);
            downloadRateTable.remove(neighborID);
            if (optimisticallyUnchokeNeighbors[0] == neighborID) {
                optimisticallyUnchokeNeighbors[0] = -1;
            }
        }
    }

}

