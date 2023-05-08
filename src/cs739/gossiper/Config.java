package cs739.gossiper;

import java.util.Random;

public class Config {
    
    String myApplicationId = String.valueOf(Math.abs(new Random().nextLong()));
    
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
}
