package cs739.gossiper.messages;

public class TerminateRequest implements Message {

    @Override
    public MessageType getType() {
        return MessageType.Terminate;
    }

    public TerminateRequest() {
    }

}
