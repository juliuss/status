package controllers;

import java.sql.SQLException;
import java.util.*;
import models.Metric;
import play.Play;
import play.cache.Cache;
import play.mvc.*;
import com.amazonaws.auth.*;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.*;
import util.*;

public class Metrics extends BaseController {
  
  private static AmazonCloudWatchClient client = null;
  
  public static void chart() {
    render();
  }
  
  public static void metric(Integer id) throws SQLException {
    if (id == null) notFound();
    Metric metric = SQLite.getMetric(id);
    if (metric == null) notFound();
    
    List json = new ArrayList();
    if (Cache.get("metric_response_"+id) != null) {
      json = Cache.get("metric_response_"+id,List.class);
    } else {
      GetMetricStatisticsRequest r = new GetMetricStatisticsRequest();
      r.setMetricName(metric.name);
      r.setNamespace(metric.namespace);
      List<Dimension> dimensions = new ArrayList<Dimension>();
      for (String key : metric.dimensions.keySet()) {
        Dimension dim = new Dimension();
        dim.setName(key.trim());
        dim.setValue(metric.dimensions.get(key).trim());
        dimensions.add(dim);
      }
      r.setDimensions(dimensions);
      r.setPeriod(60*60);
      r.setStartTime(new Date(System.currentTimeMillis()-1209600000));
      r.setEndTime(new Date(System.currentTimeMillis()));
      Collection<String> stats = new ArrayList<String>();
      stats.add(metric.operation);
      r.setStatistics(stats);
      GetMetricStatisticsResult response = client().getMetricStatistics(r);
      
      /* format output */
      
      Map series = new HashMap();
      List values = new ArrayList();
      List<SortableDatapoint> data = SortableDatapoint.convertDatapointsToSortableDatapoints(response.getDatapoints());
      Collections.sort(data);
      for (SortableDatapoint sd : data) {
        Datapoint d = sd.datapoint;
        List tuple = new ArrayList();
        tuple.add(d.getTimestamp().getTime());
        if (metric.operation.equals("Average")) {
          tuple.add(d.getAverage());
        } else if (metric.operation.equals("Sum")) {
          tuple.add(d.getSum());
        } else if (metric.operation.equals("Maximum")) {
          tuple.add(d.getMaximum());
        } else if (metric.operation.equals("Minimum")) {
          tuple.add(d.getMinimum());
        } else if (metric.operation.equals("SampleCount")) {
          tuple.add(d.getSampleCount());
        }
        values.add(tuple);
      }
      series.put("values",values);
      series.put("key","Requests");
      json.add(series);
      Cache.set("metric_response_"+id, json, "30s");
    }
    renderJSON(json);
  }
  
  @Util
  public static AmazonCloudWatchClient client() throws SQLException {
    if (client == null) {
      AWSCredentials cred = new BasicAWSCredentials(SQLite.get("awskey"),SQLite.get("awssec"));
      client = new AmazonCloudWatchClient(cred);
    }
    return client;
  }
}
