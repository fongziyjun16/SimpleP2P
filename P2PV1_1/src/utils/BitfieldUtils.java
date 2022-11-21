package utils;

import config.Common;

import java.util.*;
import java.util.logging.Logger;

public class BitfieldUtils {

    public static final int fileSize;
    public static final int pieceSize;
    public static final int pieceNumber;
    public static final int bitfieldLength;

    static {
        fileSize = Common.fileSize;
        pieceSize = Common.pieceSize;
        pieceNumber = fileSize / pieceSize + (fileSize % pieceSize == 0 ? 0 : 1);
        bitfieldLength = pieceNumber / 8  + (pieceNumber % 8 == 0 ? 0 : 1);
    }

    // runtime Logger
    private final static Logger logger = Logger.getLogger(BitfieldUtils.class.getName());

    public static void received(byte[] bitfield, int index) {
        synchronized (bitfield) {
            int row = index / 8;
            int col = index % 8;
            bitfield[row] += 1 << (7 - col);
        }
    }

    public static boolean exists(byte[] bitfield, int index) {
        synchronized (bitfield) {
            int row = index / 8;
            int col = index % 8;
            return (bitfield[row] & (1 << (7 - col))) != 0;
        }
    }

    // find the first one interested
    public static boolean isInterested(byte[] selfBitfield, byte[] neighborBitfield) {
        synchronized (selfBitfield) {
            synchronized (neighborBitfield) {
                for (int i = 0; i < selfBitfield.length; i++) {
                    for (int j = 7; j >= 0; j--) {
                        byte base = (byte) (1 << j);
                        if ((selfBitfield[i] & base) == 0 && (neighborBitfield[i] & base) != 0) {
                            return true;
                        }
                    }
                }
                return false;
            }
        }
    }

    public static int randomSelectOneInterested(byte[] selfBitfield, byte[] neighborBitfield) {
        synchronized (selfBitfield) {
            synchronized (neighborBitfield) {
                List<Integer> interestedPieces = new ArrayList<>();
                for (int i = 0; i < neighborBitfield.length; i++) {
                    for (int j = 7; j >= 0; j--) {
                        byte base = (byte) (1 << j);
                        if ((selfBitfield[i] & base) == 0 && (neighborBitfield[i] & base) != 0) {
                            interestedPieces.add(i * 8 + (7 - j));
                        }
                    }
                }
                return interestedPieces.size() == 0 ? -1 : interestedPieces.get(new Random().nextInt(interestedPieces.size()));
            }
        }
    }

    public static int getPieceLength(int index) {
        if (index != bitfieldLength - 1) {
            return pieceSize;
        } else {
            return fileSize - (bitfieldLength - 1) * pieceSize;
        }
    }

    public static int numberOfPiecesHaving(byte[] bitfield) {
        synchronized (bitfield) {
            int count = 0;
            for (int i = 0; i < bitfield.length; i++) {
                for (int j = 7; j >= 0; j--) {
                    byte base = (byte) (1 << j);
                    if ((bitfield[i] & base) != 0) {
                        count++;
                    }
                }
            }
            return count;
        }
    }

    public static boolean doesHaveCompleteFile(byte[] bitfield) {
        synchronized (bitfield) {
            for (int i = 0; i < bitfield.length; i++) {
                if (bitfield[i] != (byte) 128) {
                    return false;
                }
            }
        }
        return true;
    }

}
