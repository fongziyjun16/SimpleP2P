package dto;

import java.io.Serializable;

public class PieceInfoMessage implements Serializable {

    private static final long serialVersionUID = 1621539677435300418L;

    private final int index;

    public PieceInfoMessage(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

}
