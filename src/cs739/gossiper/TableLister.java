package cs739.gossiper;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;

/**
 * Dumps the contents of the DynamoDB table containing the gossiper events.
 * (Put your credentials and region in ~/.aws/credentials)
 */
public class TableLister {

    static Map<String, List<String>> table = new HashMap<String, List<String>>() {
    };

    public static void add_item(String key, String value) {
        if (table.get(key) == null) {
            table.put(key, new ArrayList<String>());
        }
        table.get(key).add(value);
    }

    public static void even_table() {
        int max = -1;

        for (var list : table.entrySet()) {
            if (max < list.getValue().size()) {
                max = list.getValue().size();
            }
        }

        for (var list : table.entrySet()) {
            while (max > list.getValue().size()) {
                list.getValue().add("");
            }
        }
    }

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

                add_item("application", application);
                add_item("timestamp", timestamp);
                add_item("timestamp-date", new Timestamp((long) Double.parseDouble(timestamp)).toString());

                for (Map.Entry<String, AttributeValue> value : item.entrySet()) {
                    String name = value.getKey();
                    if (Constants.partitionKey.equals(name))
                        continue;
                    if (Constants.sortKey.equals(name))
                        continue;

                    add_item(name, value.getValue().getS());
                }
                even_table();
            }
        } while (result.getLastEvaluatedKey() != null);

        List<String> keys = new ArrayList<String>();
        int num_rows = 0;
        for (var columns : table.entrySet()) {
            keys.add(columns.getKey());
            num_rows = columns.getValue().size();
            System.out.print(columns.getKey() + ",");
        }
        System.out.println("");

        for (int i = 0; i < num_rows; i++) {
            for (String string : keys) {
                System.out.print(table.get(string).get(i) + ",");
            }
            System.out.println("");
        }
        System.out.println("");
    }
}
