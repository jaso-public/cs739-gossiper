package cs739.gossiper;

public class Config {
    String pathToApplicationId = "/tmp/applicationId";
    
    int bootstrapCount = 10;
    int timeToIncommunicado = 10000;
    int rumorTTL = 10;
    int rumorFanOut = 5;
    int gossipInterval = 1000;
    
    int listenPort = 3001;
    int backlog = 5;
    
    String bootstrapHost = "newjaso.com";
    int bootstrapPort = 3001;

    int executorPoolSize = 20;

    long heartbeatInterval = 899; // for no great reason
}
