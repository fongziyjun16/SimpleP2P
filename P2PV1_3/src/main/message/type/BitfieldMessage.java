package main.message.type;

import main.message.*;

public class BitfieldMessage extends ActualMessage {

    private static final long serialVersionUID = 6896540997329607257L;

    private final byte[] bitfield;

    public BitfieldMessage(byte[] bitfield) {
        super(1 + bitfield.length, MessageType.BITFIELD);
        this.bitfield = bitfield;
    }

    public byte[] getBitfield() {
        return bitfield;
    }
}
