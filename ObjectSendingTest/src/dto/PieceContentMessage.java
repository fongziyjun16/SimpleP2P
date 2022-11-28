package dto;

import java.io.Serializable;

public class PieceContentMessage implements Serializable {

    private static final long serialVersionUID = 5997789035076123929L;

    private final byte[] content;

    public PieceContentMessage(byte[] content) {
        this.content = content;
    }

    public byte[] getContent() {
        return content;
    }

}
