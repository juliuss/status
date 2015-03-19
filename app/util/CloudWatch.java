package util;

import java.sql.SQLException;
import play.Play;
import play.cache.Cache;
import com.amazonaws.auth.*;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;

public class CloudWatch {
  
  public static AmazonCloudWatchClient getClient() throws SQLException {
    AmazonCloudWatchClient client = Cache.get("aws_client",AmazonCloudWatchClient.class);
    if (client != null) return client;
    client = new AmazonCloudWatchClient(new BasicAWSCredentials(SQLite.get("awskey"),SQLite.get("awssec")));
    Cache.set("aws_client", client);
    return client;
  }
}
