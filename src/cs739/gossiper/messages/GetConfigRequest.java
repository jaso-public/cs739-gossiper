package cs739.gossiper.messages;

public class GetConfigRequest implements Message {

    public GetConfigRequest() {
    }

    @Override
    public MessageType getType() {
        return MessageType.GetConfigRequest;
    }
}
