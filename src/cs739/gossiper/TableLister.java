package cs739.gossiper;

import java.util.List;
import java.util.Map;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;

public class TableLister {


	
	public static void main(String[] args) {

		// Create AWS credentials using the access key and secret key
		AWSCredentialsProvider credentialsProvider = new ProfileCredentialsProvider(Constants.profile);
        
        // Create a DynamoDB client using the credentials
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
            .withCredentials(credentialsProvider)
            .withRegion(Constants.region)
            .build();
        
        ScanRequest scanRequest = new ScanRequest().withTableName(Constants.tableName);
        ScanResult result = null;
        do {
            if (result != null) {
                scanRequest.setExclusiveStartKey(result.getLastEvaluatedKey());
            }
            result = client.scan(scanRequest);
            List<Map<String, AttributeValue>> items = result.getItems();
            for (Map<String, AttributeValue> item : items) {

            	String application = item.get(Constants.partitionKey).getS();
                String timestamp = item.get(Constants.sortKey).getN();
                System.out.println(application+" "+timestamp);
                
                for(Map.Entry<String, AttributeValue> value : item.entrySet()) {
                	String name = value.getKey();
                  	if(Constants.partitionKey.equals(name)) continue;
                  	if(Constants.sortKey.equals(name)) continue;
                           	
                	System.out.println("    "+name+" "+value.getValue());
                }
             }
        } while (result.getLastEvaluatedKey() != null);
	}
}
