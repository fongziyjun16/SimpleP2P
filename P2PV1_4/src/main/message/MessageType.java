package main.message;

public enum MessageType {

    CHOKE("choke", 0),
    UNCHOKE("unchoke", 1),
    INTERESTED("interested", 2),
    NOT_INTERESTED("not_interested", 3),
    HAVE("have", 4),
    BITFIELD("bitfield", 5),
    REQUEST("request", 6),
    PIECE("piece", 7),
    END("end", 8);

    private final String name;
    private final int value;

    MessageType(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }
}
