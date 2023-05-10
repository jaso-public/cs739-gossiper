package cs739.gossiper.messages;

import cs739.gossiper.Config;

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
