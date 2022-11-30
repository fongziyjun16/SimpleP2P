package main;

import config.*;

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

    private final PeerHandler peerHandler;

    private final Map<Integer, DownloadRateTableEntry> downloadRateTable = new HashMap<>();

    private final Set<Integer> interestedNeighbors = new HashSet<>();
    private final Set<Integer> unchokeNeighbors = new HashSet<>();
    private final int[] optimisticallyUnchokeNeighbors = {-1};

    private final static Random random = new Random();
    // runtime Logger
    private final static Logger logger = Logger.getLogger(PeerSelector.class.getName());

    public PeerSelector(PeerHandler peerHandler, PeerHub peerHub) {
        this.peerHandler = peerHandler;

        ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(4);

        scheduler.scheduleWithFixedDelay(() -> {
            updateUnchokeNeighbors();
            synchronized (unchokeNeighbors) {
                synchronized (interestedNeighbors) {
                    peerHub.updateUnchokeNeighbors(interestedNeighbors, unchokeNeighbors);
                }
            }
        }, 0, Common.unchokingInterval, TimeUnit.SECONDS);

        scheduler.scheduleWithFixedDelay(() -> {
            updateOptimisticallyUnchokeNeighbor();
            synchronized (optimisticallyUnchokeNeighbors) {
                peerHub.updateOptimisticallyUnchokeNeighbor(optimisticallyUnchokeNeighbors[0]);
            }
        }, 0, Common.optimisticUnchokingInterval, TimeUnit.SECONDS);
    }

    public void downloadRegister(int neighborID) {
        synchronized (downloadRateTable) {
            downloadRateTable.put(neighborID, new DownloadRateTableEntry(neighborID));
            logger.log(Level.INFO, "download table records neighbor " + neighborID);
        }
    }

    public void addInterested(int neighborID) {
        synchronized (interestedNeighbors) {
            interestedNeighbors.add(neighborID);
        }
    }

    public void removeInterested(int neighborID) {
        synchronized (interestedNeighbors) {
            interestedNeighbors.remove(neighborID);
        }
        synchronized (unchokeNeighbors) {
            unchokeNeighbors.remove(neighborID);
        }
        synchronized (optimisticallyUnchokeNeighbors) {
            if (optimisticallyUnchokeNeighbors[0] == neighborID) {
                optimisticallyUnchokeNeighbors[0] = -1;
            }
        }
    }

    private List<Integer> getRandomInterestedNeighborIDs() {
        synchronized (interestedNeighbors) {
            List<Integer> peerIDs = new ArrayList<>(interestedNeighbors);
            for (int i = 0; i < peerIDs.size(); i++) {
                int next = random.nextInt(peerIDs.size());
                int temp = peerIDs.get(i);
                peerIDs.set(i, peerIDs.get(next));
                peerIDs.set(next, temp);
            }
            return peerIDs;
        }
    }

    private List<Integer> topK() {
        synchronized (downloadRateTable) {
            List<Integer> selected = new ArrayList<>();
            PriorityQueue<DownloadRateTableEntry> pq =
                    new PriorityQueue<>((e1, e2) -> Long.compare(e2.amount, e1.amount));
            for (DownloadRateTableEntry entry : downloadRateTable.values()) {
                pq.offer(entry);
            }
            while (!pq.isEmpty()) {
                selected.add(pq.poll().id);
            }
            return selected;
        }
    }

    private void updateUnchokeNeighbors() {
        synchronized (unchokeNeighbors) {
            unchokeNeighbors.clear();
            if (!PeerInfo.doesPeerHaveFile(peerHandler.getSelfID())) {
                List<Integer> topNeighbors = topK();
                synchronized (interestedNeighbors) {
                    int k = Common.numberOfPreferredNeighbors;
                    for (int topNeighbor : topNeighbors) {
                        if (interestedNeighbors.contains(topNeighbor)) {
                            unchokeNeighbors.add(topNeighbor);
                            k--;
                            if (k == 0) {
                                break;
                            }
                        }
                    }
                }
                resetDownloadTable();
            } else {
                List<Integer> randomNeighborIDs = getRandomInterestedNeighborIDs();
                for (int randomNeighborID : randomNeighborIDs) {
                    unchokeNeighbors.add(randomNeighborID);
                    if (unchokeNeighbors.size() == Common.numberOfPreferredNeighbors) {
                        break;
                    }
                }
            }
        }
    }

    private void updateOptimisticallyUnchokeNeighbor() {
        synchronized (optimisticallyUnchokeNeighbors) {
            List<Integer> randomInterestedNeighborIDs = getRandomInterestedNeighborIDs();
            synchronized (unchokeNeighbors) {
                for (int randomInterestedNeighborID : randomInterestedNeighborIDs) {
                    if (!unchokeNeighbors.contains(randomInterestedNeighborID)) {
                        optimisticallyUnchokeNeighbors[0] = randomInterestedNeighborID;
                        break;
                    }
                }
            }
        }
    }

    public void addDownloadBytes(int neighborID, int byteNumber) {
        synchronized (downloadRateTable) {
            DownloadRateTableEntry entry = downloadRateTable.get(neighborID);
            entry.amount += byteNumber;
        }
    }

    private void resetDownloadTable() {
        synchronized (downloadRateTable) {
            for (DownloadRateTableEntry entry : downloadRateTable.values()) {
                entry.amount = 0;
            }
        }
    }

}
