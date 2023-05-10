package cs739.gossiper.messages;

public class GetConfigReply implements Message {

    public Config config;

    public GetConfigReply() {
    }

    public GetConfigReply(Config config) {
        this.config = config;
    }

    @Override
    public MessageType getType() {
        return MessageType.GetConfigReply;
    }
}
