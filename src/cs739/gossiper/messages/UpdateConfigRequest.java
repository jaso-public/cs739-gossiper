package cs739.gossiper.messages;

import cs739.gossiper.Config;

public class UpdateConfigRequest implements Message {

    public Config config;

    public UpdateConfigRequest() {
    }

    public UpdateConfigRequest(Config config) {
        this.config = config;
    }

    @Override
    public MessageType getType() {
        return MessageType.UpdateConfig;
    }

}
