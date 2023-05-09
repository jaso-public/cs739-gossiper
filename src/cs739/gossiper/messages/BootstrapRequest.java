package cs739.gossiper.messages;

import cs739.gossiper.Application;

public class BootstrapRequest implements Message {
    
    public Application application;

    public BootstrapRequest() {
    }


    public BootstrapRequest(Application application) {
        this.application = application;
    }

    @Override
    public MessageType getType() {
        return MessageType.BootstrapRequest;
    }
}
