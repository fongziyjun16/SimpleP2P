package utils;

import config.Common;

import java.util.*;
import java.util.logging.*;

public class BitfieldUtils {

    public static final int pieceNumber;
    public static final int bitfieldLength;

    // runtime Logger
    private final static Logger logger = Logger.getLogger(BitfieldUtils.class.getName());

    static {
        pieceNumber = Common.fileSize / Common.pieceSize + (Common.fileSize % Common.pieceSize == 0 ? 0 : 1);
        bitfieldLength = pieceNumber / 8  + (pieceNumber % 8 == 0 ? 0 : 1);
    }

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

    public static void received(byte[] bitfield, int index) {
        synchronized (bitfield) {
            int row = index / 8;
            int col = index % 8;
            bitfield[row] += 1 << (7 - col);
        }
    }

}
