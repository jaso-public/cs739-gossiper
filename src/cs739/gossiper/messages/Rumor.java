package cs739.gossiper.messages;

import cs739.gossiper.Application;

public class Rumor implements Message {
    
    public Application application;
    public int ttl;

    
    public Rumor() {
    }

    public Rumor(Application application, int ttl) {
        this.application = application;
        this.ttl = ttl;
    }

    @Override
    public MessageType getType() {
        return MessageType.Rumor;
    }
}
