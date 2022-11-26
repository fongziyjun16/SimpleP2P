package connect;

import config.*;
import main.PeerLogger;

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
            updateUnchokedNeighbors();
            PeerLogger.changePreferredNeighbors(
                    peerRegister.getSelfID(), new ArrayList<>(unchokeNeighbors));
            synchronized (unchokeNeighbors) {
                peerRegister.updateUnchokedNeighbors(interestedNeighbors, unchokeNeighbors);
            }
        }, 0, Common.unchokingInterval, TimeUnit.SECONDS);

        scheduler.scheduleWithFixedDelay(() -> {
            updateOptimisticallyUnchokedNeighbor();
            PeerLogger.changeOptimisticallyUnchokedNeighbor(
                    peerRegister.getSelfID(), optimisticallyUnchokeNeighbors[0]);
            synchronized (optimisticallyUnchokeNeighbors) {
                int peerID = optimisticallyUnchokeNeighbors[0];
                if (peerID != -1) {
                    peerRegister.updateOptimisticallyUnchokedNeighbor(peerID);
                }
            }
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

    public void addDownloadBytes(int neighborID, long byteNumber) {
        synchronized (downloadRateTable) {
            DownloadRateTableEntry entry = downloadRateTable.get(neighborID);
            entry.amount += byteNumber;
        }
    }

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

    private List<Integer> getRandomNeighborIDs() {
        List<Integer> peerIDs = new ArrayList<>(interestedNeighbors);
        for (int i = 0; i < peerIDs.size(); i++) {
            int next = random.nextInt(peerIDs.size());
            int temp = peerIDs.get(i);
            peerIDs.set(i, peerIDs.get(next));
            peerIDs.set(next, temp);
        }
        return peerIDs;
    }

    private void updateUnchokedNeighbors() {
        synchronized (unchokeNeighbors) {
            unchokeNeighbors.clear();
            if (!PeerInfo.doesPeerHaveFile(peerRegister.getSelfID())) {
                List<Integer> topNeighbors = topK();
                synchronized (interestedNeighbors) {
                    for (Integer topNeighbor : topNeighbors) {
                        if (interestedNeighbors.contains(topNeighbor)) {
                            unchokeNeighbors.add(topNeighbor);
                        }
                    }
                }
                resetDownloadTable();
            } else {
                List<Integer> randomNeighborIDs = getRandomNeighborIDs();
                for (Integer randomNeighborID : randomNeighborIDs) {
                    unchokeNeighbors.add(randomNeighborID);
                    if (unchokeNeighbors.size() == Common.numberOfPreferredNeighbors) {
                        break;
                    }
                }
            }
        }
    }

    private void updateOptimisticallyUnchokedNeighbor() {
        synchronized (optimisticallyUnchokeNeighbors) {
            List<Integer> randomNeighborIDs = getRandomNeighborIDs();
            synchronized (interestedNeighbors) {
                for (Integer randomNeighborID : randomNeighborIDs) {
                    if (!unchokeNeighbors.contains(randomNeighborID) &&
                            interestedNeighbors.contains(randomNeighborID)) {
                        optimisticallyUnchokeNeighbors[0] = randomNeighborID;
                        break;
                    }
                }
            }
        }
    }

}

