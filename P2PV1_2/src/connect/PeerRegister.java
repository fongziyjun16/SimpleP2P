package connect;

import java.util.*;

public class PeerRegister {

    private final int selfID;
    private final List<Byte> selfBitfield;

    public PeerRegister(int selfID, List<Byte> selfBitfield) {
        this.selfID = selfID;
        this.selfBitfield = selfBitfield;
    }
}
