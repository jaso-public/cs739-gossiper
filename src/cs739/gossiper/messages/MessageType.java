package cs739.gossiper.messages;

public enum MessageType {
    Gossip(10),
    Rumor(21),
    BootstrapRequest(31),
    BootstrapReply(32),
    Heartbeat(41),
    Dump(101);

    private int value;
    

    private MessageType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
