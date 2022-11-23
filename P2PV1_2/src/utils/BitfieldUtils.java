package utils;

import config.Common;

public class BitfieldUtils {

    public static final long pieceNumber;
    public static final long bitfieldLength;

    static {
        pieceNumber = Common.fileSize / Common.pieceSize + (Common.fileSize % Common.pieceSize == 0 ? 0 : 1);
        bitfieldLength = pieceNumber / 8  + (pieceNumber % 8 == 0 ? 0 : 1);
    }

}
