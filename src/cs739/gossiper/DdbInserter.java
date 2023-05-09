package cs739.gossiper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;

public class DdbInserter implements Runnable {
	private static final Logger logger = LogManager.getLogger(DdbInserter.class);

	private final String applicationId;
	private BlockingQueue<Record> records = new LinkedBlockingQueue<>();
	private boolean running = true;
	private Thread thread;

	static class Record {
		final String application;
		final long timestamp;
		final Map<String,String> values;
		
		public Record(String application, long timestamp, Map<String, String> values) {
			this.application = application;
			this.timestamp = timestamp;
			this.values = values;
		}	
	}
	
	static class DrainRecord extends Record {
		public DrainRecord() {
			super(null, 0, null);
		}
	}
	
	public DdbInserter(String applicationId) {
		this.applicationId = applicationId;
		thread = new Thread(this,"DdbInserterThread");
		thread.start();
	}

	@Override
	public void run() {
		while(running || records.size() > 0) {
			try {
				doLoop();
			} catch(Throwable t) {
				logger.error("error during run", t);
			}
		}		
	}
	
	public void Record(Map<String,String> values) {
		logger.info("record values");
		records.add(new Record(applicationId, System.currentTimeMillis(), values));
	}
	
	public void drain() {
		logger.warn("drain called");
		
		records.add(new DrainRecord());
		
		// wait to join the thread (wait for background thread to stop)
		try {
			thread.join();
		} catch (InterruptedException e) {
			logger.error("joining thread", e);
		}
		
		if(records.size() > 0) {
			logger.info("stopping with items remaining in the queue:"+records.size());
		}
	}
	
	private void doLoop() throws Exception {
		// Create AWS credentials using the access key and secret key
		AWSCredentialsProvider credentialsProvider = new ProfileCredentialsProvider(Constants.profile);
	    
	    // Create a DynamoDB client using the credentials
	    AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
	        .withCredentials(credentialsProvider)
	        .withRegion(Constants.region)
	        .build();

		while(running || records.size() > 0) {
			Record record = records.poll(10, TimeUnit.SECONDS);
			if(record == null) {
				logger.info("doLoop -- nothing to store in DDB");
				continue;
			}
			
			if(record instanceof DrainRecord) {
				logger.info("doLoop returning after dequeueing a StopEvent");
				running = false;
				return;
			}
			
			Map<String,AttributeValue> item = new HashMap<>();
			item.put(Constants.partitionKey, new AttributeValue(applicationId));
			item.put(Constants.sortKey, new AttributeValue().withN(String.valueOf(record.timestamp)));
			for(Map.Entry<String,String> entry : record.values.entrySet() ) {
				item.put(entry.getKey(), new AttributeValue(entry.getValue()));								
			}
			
			PutItemRequest putItemRequest = new PutItemRequest().withTableName(Constants.tableName).withItem(item);			
			putItemRequest.setReturnValues(ReturnValue.ALL_OLD);
            
		    try {
            	PutItemResult putItemResult = client.putItem(putItemRequest);
            	if(putItemResult.getAttributes() != null) {
            		logger.error("item already existed applicationId:"+applicationId+" timestamp:"+record.timestamp);
            	}
            } catch (Exception e) {
                logger.error("putItem", e);
            }
		}   
	}
}
