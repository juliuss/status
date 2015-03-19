package controllers;

import play.*;
import play.cache.Cache;
import play.libs.Crypto;
import play.mvc.*;
import util.SQLite;
import java.sql.SQLException;
import java.util.*;
import org.joda.time.LocalDate;
import models.*;

public class Status extends BaseController {
  
  @SuppressWarnings("unused")
  @Before(priority=1)
  private static void checkInit() throws SQLException {
    if (SQLite.get("awskey")==null) {
      Setup.firstRun();
    }
  }
  
  @SuppressWarnings("unused")
  @Before(priority=3)
  private static void cookieCheck() throws SQLException {
    renderArgs.put("admin",false);
    if (request.cookies.get("a") != null) {
      String userId = Crypto.decryptAES(request.cookies.get("a").value);
      if (userId.equals(SQLite.get("dcuserid"))) {
        renderArgs.put("admin",true);
      }
    }
  }
  
  public static void index() throws SQLException {
    List<Metric> metrics = SQLite.getMetrics();
    List<Issue> issues = SQLite.getIssues();
    Issue last_issue = Cache.get("last_issue",Issue.class);
    Map<String,List<Issue>> issues_by_date = new LinkedHashMap<String,List<Issue>>();
    {
      //put recent days in a map
      LocalDate day = new LocalDate(); //starts at today
      LocalDate endDay = new LocalDate().minusDays(7); //7 days ago
      while (day.isAfter(endDay)) {
        issues_by_date.put(day.toString("MMM dd, yyyy"), new ArrayList<Issue>());
        day = day.minusDays(1);
      }
    }
    //now put recent issues in the map for their date
    for (Issue i : issues) {
      String key = new LocalDate(i.start).toString("MMM dd, yyyy");
      if (issues_by_date.containsKey(key)) {
        issues_by_date.get(key).add(i);
      }
    }
    render(metrics,last_issue,issues_by_date);
  }
}