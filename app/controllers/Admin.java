package controllers;

import java.sql.SQLException;
import java.util.*;
import models.*;
import models.Metric;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.*;
import com.google.gson.JsonElement;
import play.*;
import play.cache.Cache;
import play.libs.*;
import play.libs.WS.*;
import play.mvc.*;
import util.*;

public class Admin extends BaseController {
  
  @Before(unless = {"callback","signout"},priority=3)
  @SuppressWarnings("unused")
  private static void cookieCheck() throws SQLException {
    if (request.cookies.get("a") != null) {
      String userId = Crypto.decryptAES(request.cookies.get("a").value);
      if (!userId.equals(SQLite.get("dcuserid"))) {
        unauthorized();
        return;
      }
    }
  }
  
  public static void signout() throws SQLException {
    response.removeCookie("a");
    Status.index();
  }
  
  public static void callback(String access_token, String code, String state, String coupon, String whence, String error) throws SQLException {
    if (!Strings.isNullOrEmpty(error)) {
      flash.put("error", "All fields are required.");
      flash.keep();
      Status.index();
    }
    
    String id;
    
    //trade code for access_token
    if (code != null && !code.isEmpty()){
      WSRequest req = WS.url("https://www.dailycred.com/oauth/access_token?client_secret="+SQLite.get("dcsec")+"&code="+code);
      HttpResponse res = req.get();
      JsonElement e = res.getJson();
      access_token = e.getAsJsonObject().get("access_token").getAsString();
    }

    //use access_token to get user details
    {
      WSRequest req = WS.url("https://www.dailycred.com/graph/me.json?access_token="+access_token);
      HttpResponse res = req.get();
      JsonElement e = res.getJson();
      //email = e.getAsJsonObject().get("email").getAsString();
      id = e.getAsJsonObject().get("id").getAsString();
    }
    
    //authorized admin?
    if (!SQLite.get("dcuserid").equals(id)) {
      unauthorized();
      return;
    }
    
    response.setCookie("a", Crypto.encryptAES(id));
    Status.index();
  }
  
  public static void settings() {
    renderArgs.put("setup", false);
    render();
  }
  
  public static void settingsPost(String awskey, String awssec, String dcappid, String dcsec, String dcuserid, String name, String home) throws SQLException {
    if (Strings.isNullOrEmpty(awskey, awssec, dcappid, dcsec, dcuserid, name, home)) {
      flash.put("error", "All fields are required.");
      flash.keep();
      params.flash();
      Admin.settings();
    }
    SQLite.set("name", name);
    SQLite.set("home", home);
    SQLite.set("awskey", awskey);
    SQLite.set("awssec", awssec);
    SQLite.set("dcappid", dcappid);
    SQLite.set("dcsec", dcsec);
    SQLite.set("dcuserid", dcuserid);
    SQLite.set("locked", "true");
    Cache.delete("aws_client");
    Status.index();
  }
  
  public static void listCloudWatchMetrics() throws SQLException {
    String namespace = request.params.get("depdrop_parents[0]");
    
    List<String> metrics = new ArrayList<String>();
    AmazonCloudWatchClient client = Metrics.client();
    for (com.amazonaws.services.cloudwatch.model.Metric m : client.listMetrics().getMetrics()) {
      if (m.getNamespace().equals(namespace)) {
        if (m.getDimensions().isEmpty()) {
          metrics.add(m.getMetricName());
        } else {
          String dlist = "";
          for (Dimension dimension : m.getDimensions()) {
            dlist += ", "+dimension.getName() + ": " + dimension.getValue();
          }
          dlist = dlist.substring(2); //remove commas
          metrics.add(m.getMetricName()+" ("+dlist+")");
        }
      }
    }   
    
    Collections.sort(metrics);
    
    Map json = new HashMap();
    List output = new ArrayList();
    for (String m : metrics) {
      Map option = new HashMap();
      option.put("id", m);
      option.put("name", m);
      output.add(option);
    }
    json.put("output",output);
    json.put("selected", "");
    renderJSON(json);
  }
  
  public static void metrics() throws SQLException {
    //get namespaces from AWS
    AmazonCloudWatchClient client = Metrics.client();
    List<String> namespaces = new ArrayList<String>();
    for (com.amazonaws.services.cloudwatch.model.Metric m : client.listMetrics().getMetrics()) {
      if (!namespaces.contains(m.getNamespace())) {
        namespaces.add(m.getNamespace());
      }
    }
    Collections.sort(namespaces);
    //load existing metrics for display
    List<Metric> metrics = SQLite.getMetrics();
    render(namespaces,metrics);
  }
  
  public static void metricsPost(Integer id, String namespace, String name, String display, String unit, String operation) throws SQLException {
    try {
      SQLite.putMetric(id, name, namespace, display, unit, operation);
    } catch (SQLException e) {
      Logger.error("Error creating new metric: %s", e);
      error(e);
    }
    metrics();
  }
  
  public static void metricDelete(Integer id) throws SQLException {
    SQLite.deleteMetric(id);
    Admin.metrics();
  }
  
  public static void newIssue(String impact, String message, String name) throws SQLException {
    SQLite.putIssue(impact,message,name);
    Status.index();
  }
  
  public static void editIssue(Integer id, String impact, String message, String name) throws SQLException {
    SQLite.editIssue(id, impact, message, name);
    Status.index();
  }
  
  public static void resolveIssue(Integer id, Boolean resolved) throws SQLException {
    SQLite.resolveIssue(id,resolved);
    Status.index();
  }
  
  public static void deleteIssue(Integer id) throws SQLException {
    SQLite.deleteIssue(id);
    Status.index();
  }
}