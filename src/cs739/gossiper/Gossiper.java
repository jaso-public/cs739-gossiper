package cs739.gossiper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class Gossiper {
	 private static final Logger logger = LogManager.getLogger(Gossiper.class);


	public static void main(String[] args) {
		System.out.println("hello world");
		
		   Configurator.initialize(null, "log4j2.xml");

		    // Use the logger to log messages
		    logger.debug("Debug message");
		    logger.info("Info message");


	}

}
