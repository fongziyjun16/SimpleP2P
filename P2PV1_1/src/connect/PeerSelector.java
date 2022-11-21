package connect;

import config.*;
import main.PeerLogger;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

public class PeerSelector {

    private final PeerRegister peerRegister;

    private final Map<Integer, DownloadRateTableEntry> downloadRateTable = new HashMap<>();

    private final Set<Integer> interestedNeighbors = new HashSet<>();
    private final Set<Integer> unchokedNeighbors = new HashSet<>();
    private final int[] optimisticallyUnchokedNeighbors = {-1};

    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(4);

    private final static Random random = new Random();
    // runtime Logger
    private final static Logger logger = Logger.getLogger(PeerSelector.class.getName());

    public PeerSelector(PeerRegister peerRegister) {
        this.peerRegister = peerRegister;
        schedulerInitialize();
    }

    private void schedulerInitialize() {
        scheduler.scheduleWithFixedDelay(() -> {
            // periodic unchoking neighbors selection logic
            updateUnchokedNeighbors();
            PeerLogger.changePreferredNeighbors(
                    peerRegister.getSelfID(), new ArrayList<>(unchokedNeighbors));
            synchronized (unchokedNeighbors) {
                peerRegister.updateUnchokedNeighbors(unchokedNeighbors);
            }
        }, 0, Common.unchokingInterval, TimeUnit.SECONDS);

        scheduler.scheduleWithFixedDelay(() -> {
            // periodic optimistically unchoking neighbors selection logic
            updateOptimisticallyUnchokedNeighbor();
            PeerLogger.changeOptimisticallyUnchokedNeighbor(
                    peerRegister.getSelfID(), optimisticallyUnchokedNeighbors[0]);
            synchronized (optimisticallyUnchokedNeighbors) {
                int peerID = optimisticallyUnchokedNeighbors[0];
                if (peerID != -1) {
                    peerRegister.updateOptimisticallyUnchokedNeighbor(peerID);
                }
            }
        }, 0, Common.optimisticUnchokingInterval, TimeUnit.SECONDS);
    }

    public void downloadRegister(int neighborID) {
        synchronized (downloadRateTable) {
            downloadRateTable.put(neighborID, new DownloadRateTableEntry(neighborID));
        }
    }

    public void addDownloadBytes(int neighborID, long byteNumber) {
        synchronized (downloadRateTable) {
            DownloadRateTableEntry entry = downloadRateTable.get(neighborID);
            entry.amount += byteNumber;
        }
    }

    // K is equal to NumberOfPreferredNeighbors
    private List<Integer> topK() {
        synchronized (downloadRateTable) {
            int k = Common.numberOfPreferredNeighbors;
            List<Integer> selected = new ArrayList<>();
            PriorityQueue<DownloadRateTableEntry> pq =
                    new PriorityQueue<>((e1, e2) -> Long.compare(e2.amount, e1.amount));
            for (DownloadRateTableEntry entry : downloadRateTable.values()) {
                pq.offer(entry);
            }
            while (!pq.isEmpty() && k > 0) {
                selected.add(pq.poll().id);
                k--;
            }
            return selected;
        }
    }

    private void resetDownloadTable() {
        synchronized (downloadRateTable) {
            for (DownloadRateTableEntry entry : downloadRateTable.values()) {
                entry.amount = 0;
            }
        }
    }

    public void addInterestedNeighbor(int neighborID) {
        synchronized (interestedNeighbors) {
            interestedNeighbors.add(neighborID);
        }
    }

    public void removeInterestedNeighbor(int neighborID) {
        synchronized (interestedNeighbors) {
            interestedNeighbors.remove(neighborID);
        }
    }

    private List<Integer> getRandomNeighborIDs() {
        List<Integer> peerIDs = new ArrayList<>(downloadRateTable.keySet());
        for (int i = 0; i < peerIDs.size(); i++) {
            int next = random.nextInt(peerIDs.size());
            int temp = peerIDs.get(i);
            peerIDs.set(i, peerIDs.get(next));
            peerIDs.set(next, temp);
        }
        return peerIDs;
    }

    private void updateUnchokedNeighbors() {
        synchronized (unchokedNeighbors) {
            unchokedNeighbors.clear();
            if (!PeerInfo.doesPeerHaveFile(peerRegister.getSelfID())) {
                List<Integer> topNeighbors = topK();
                synchronized (interestedNeighbors) {
                    for (Integer topNeighbor : topNeighbors) {
                        if (interestedNeighbors.contains(topNeighbor)) {
                            unchokedNeighbors.add(topNeighbor);
                        }
                    }
                }
                resetDownloadTable();
            } else {
                List<Integer> randomNeighborIDs = getRandomNeighborIDs();
                for (Integer randomNeighborID : randomNeighborIDs) {
                    unchokedNeighbors.add(randomNeighborID);
                    if (unchokedNeighbors.size() == Common.numberOfPreferredNeighbors) {
                        break;
                    }
                }
            }
        }
    }

    public boolean isUnchokedNeighbor(int neighborID) {
        synchronized (unchokedNeighbors) {
            if (unchokedNeighbors.contains(neighborID)) {
                return true;
            }
        }
        return false;
    }

    private void updateOptimisticallyUnchokedNeighbor() {
        synchronized (optimisticallyUnchokedNeighbors) {
            List<Integer> randomNeighborIDs = getRandomNeighborIDs();
            synchronized (interestedNeighbors) {
                for (Integer randomNeighborID : randomNeighborIDs) {
                    if (!unchokedNeighbors.contains(randomNeighborID) &&
                            interestedNeighbors.contains(randomNeighborID)) {
                        optimisticallyUnchokedNeighbors[0] = randomNeighborID;
                        break;
                    }
                }
            }
        }
    }

    public boolean isOptimisticallyUnchokedNeighbor(int neighborID) {
        synchronized (optimisticallyUnchokedNeighbors) {
            return optimisticallyUnchokedNeighbors[0] == neighborID;
        }
    }

}

class DownloadRateTableEntry {
    int id;
    long amount;

    public DownloadRateTableEntry(int id) {
        this.id = id;
        amount = 0;
    }
}