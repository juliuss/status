package util;

import java.sql.*;
import java.util.*;
import java.util.Date;
import models.*;
import play.Logger;
import play.cache.Cache;

public class SQLite {
  
  static {
    try {
      init();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
 
  static public String get(String key) throws SQLException   {
    if (Cache.get("status_admin_"+key)!=null) return Cache.get("status_admin_"+key,String.class);
    PreparedStatement stmt = null;
    Connection connection = null;
    String value = null;
    try {
      connection = getConnection();
      stmt = connection.prepareStatement("SELECT V FROM status_admin WHERE K = ?;");
      stmt.setString(1, key);
      ResultSet rs = stmt.executeQuery();
      if (rs.next()) value = rs.getString("V");
      rs.close();
      stmt.close();
    } finally {
      close(connection,stmt,null);
    }
    Cache.set("status_admin_"+key, value);
    return value;
  }
  
  static public void set(String key, String value) throws SQLException   {
    PreparedStatement stmt = null;
    Connection connection = null;
    try {
      connection = getConnection();
      stmt = connection.prepareStatement("INSERT OR REPLACE INTO status_admin (K, V) VALUES (?, ?);");
      stmt.setString(1, key);
      stmt.setString(2, value);
      stmt.execute();
      stmt.close();
    } finally {
      close(connection,stmt,null);
    }
    Cache.delete("status_admin_"+key);
  }
  
  static public void putMetric(Integer id, String name, String namespace, String display, String unit, String operation) throws SQLException   {
    Cache.delete("metrics");
    if (!tableExists("metric")) {
      //create table
      Connection connection = getConnection();
      PreparedStatement stmt = connection.prepareStatement("CREATE TABLE metric (id INT PRIMARY KEY NOT NULL, name TEXT, namespace TEXT, display TEXT, unit TEXT, operation TEXT);");
      stmt.execute();
      close(connection,stmt,null);
    }
    if (id == null || id == 0) {
      Connection connection = getConnection();
      PreparedStatement stmt = connection.prepareStatement("SELECT max(id) FROM metric;");
      ResultSet rs = stmt.executeQuery();
      if (rs.next()) {
        id = rs.getInt(1)+1;
      } else {
        id = 1;
      }
      close(connection,stmt,null);
    }
    PreparedStatement stmt = null;
    Connection connection = getConnection();
    stmt = connection.prepareStatement("INSERT OR REPLACE INTO metric (id,name,namespace,display,unit,operation) VALUES (?,?,?,?,?,?);");
    stmt.setInt(1, id);
    stmt.setString(2, name);
    stmt.setString(3, namespace);
    stmt.setString(4, display);
    stmt.setString(5, unit);
    stmt.setString(6, operation);
    stmt.execute();
    close(connection,stmt,null);
  }
  
  static public void deleteMetric(Integer id) throws SQLException   {
    Cache.delete("metrics");
    //delete the metric
    Connection connection = getConnection();
    PreparedStatement stmt = connection.prepareStatement("DELETE FROM metric WHERE id=?;");
    stmt.setInt(1, id);
    stmt.execute();
    close(connection,stmt,null);
  }
  
  static public Metric getMetric(int id) throws SQLException   {
    List<Metric> metrics = getMetrics();
    for (Metric m : metrics) {
      if (m.id == id) return m;
    }
    return null;
  }
  
  static public List<Metric> getMetrics() throws SQLException   {
    //check cache first
    List<Metric> metrics = Cache.get("metrics",List.class);
    if (metrics != null) return metrics;
    //next, if the table doesn't exist, we have no metrics yet
    metrics = new ArrayList<Metric>();
    if (!tableExists("metric")) return metrics;
    //finally, pull from the db
    Connection connection = getConnection();
    PreparedStatement stmt = connection.prepareStatement("SELECT * FROM metric ORDER BY id;");
    ResultSet rs = stmt.executeQuery();
    while (rs.next()) {
      Metric m = new Metric(rs.getInt("id"), rs.getString("name"), rs.getString("namespace"), rs.getString("display"), rs.getString("unit"), rs.getString("operation"));
      metrics.add(m);
    }
    close(connection,stmt,rs);
    Cache.set("metrics", metrics);
    return metrics;
  }
  
  static private void init() throws SQLException {
    Connection connection = getConnection();
    //init the database if needed
    try {
      if (!tableExists("status_admin")) {
        //create table
        PreparedStatement stmt = connection.prepareStatement("CREATE TABLE status_admin (K TEXT PRIMARY KEY NOT NULL, V TEXT NOT NULL);");
        stmt.execute();
        close(connection,stmt,null);
      }
    } catch (SQLException e) {
      Logger.error("Database error: %s",e);
    }
  }
  
  static private boolean tableExists(String table) throws SQLException {
    Connection connection = getConnection();
    PreparedStatement stmt = connection.prepareStatement("SELECT name FROM sqlite_master WHERE type='table' AND name=?");
    stmt.setString(1, table);
    ResultSet rs = stmt.executeQuery();
    boolean exists = rs.next();
    close(connection,stmt,rs);
    return exists;
  }
  
  public static Connection getConnection() throws SQLException {
    Connection connection = null;
    try {
      Class.forName("org.sqlite.JDBC");
    } catch (ClassNotFoundException e) {
      throw new SQLException(e);
    }
    connection = DriverManager.getConnection("jdbc:sqlite:status.db");
    return connection;
  }
  
  public static void close(Connection connection, Statement statement, ResultSet resultSet) {
    try {
      connection.close();
    } catch (Exception e) {}
    try {
      statement.close();
    } catch (Exception e) {}
    try {
      resultSet.close();
    } catch (Exception e) {}
  }

  public static void putIssue(String impact, String message, String name) throws SQLException {
    Cache.delete("issues");
    Cache.delete("lastIssues");
    if (!tableExists("issue")) {
      //create table
      Connection connection = getConnection();
      PreparedStatement stmt = connection.prepareStatement("CREATE TABLE issue (id INTEGER PRIMARY KEY, name TEXT, message TEXT, impact TEXT, resolved BOOLEAN, start INT, end INT);");
      stmt.execute();
      close(connection,stmt,null);
    }
    PreparedStatement stmt = null;
    Connection connection = getConnection();
    stmt = connection.prepareStatement("INSERT INTO issue (name,message,impact,resolved,start) VALUES (?,?,?,?,?);");
    stmt.setString(1, name);
    stmt.setString(2, message);
    stmt.setString(3, impact);
    stmt.setBoolean(4, false);
    stmt.setInt(5, (int)(new Date().getTime() / 1000));
    stmt.execute();
    close(connection,stmt,null);
  }
  
  static public List<Issue> getIssues() throws SQLException   {
    //check cache first
    List<Issue> issues = Cache.get("issues",List.class);
    if (issues != null) return issues;
    //next, if the table doesn't exist, we have no metrics yet
    issues = new ArrayList<Issue>();
    if (!tableExists("issue")) return issues;
    //finally, pull from the db
    Connection connection = getConnection();
    PreparedStatement stmt = connection.prepareStatement("SELECT * FROM issue ORDER BY start DESC;");
    ResultSet rs = stmt.executeQuery();
    Issue lastIssue = null;
    while (rs.next()) {
      Issue i = new Issue(rs.getInt("id"), rs.getString("name"), rs.getString("message"), rs.getString("impact"), rs.getInt("resolved"), rs.getInt("start"), rs.getInt("end"));
      issues.add(i);
      if (lastIssue == null) lastIssue = i;
    }
    close(connection,stmt,rs);
    Cache.set("issues", issues);
    Cache.set("last_issue", lastIssue);
    return issues;
  }

  public static void updateIssue(Integer id, String update) {
    return;
  }

  public static void editIssue(Integer id, String impact, String message, String name) throws SQLException {
    Cache.delete("issues");
    Cache.delete("lastIssues");
    PreparedStatement stmt = null;
    Connection connection = getConnection();
    stmt = connection.prepareStatement("UPDATE issue SET name=?,message=?,impact=? WHERE id=?;");
    stmt.setString(1, name);
    stmt.setString(2, message);
    stmt.setString(3, impact);
    stmt.setInt(4, id);
    stmt.execute();
    close(connection,stmt,null);
  }

  public static void resolveIssue(Integer id, Boolean resolved) throws SQLException {
    Cache.delete("issues");
    Cache.delete("lastIssues");
    PreparedStatement stmt = null;
    Connection connection = getConnection();
    stmt = connection.prepareStatement("UPDATE issue SET resolved=?,end=? WHERE id=?;");
    stmt.setBoolean(1, resolved);
    if (resolved) {
      stmt.setInt(2, (int)(System.currentTimeMillis() / 1000));
    } else {
      stmt.setNull(2, Types.INTEGER);
    }
    stmt.setInt(3, id);
    stmt.execute();
    close(connection,stmt,null);
  }

  public static void deleteIssue(Integer id) throws SQLException {
    Cache.delete("issues");
    Cache.delete("lastIssues");
    PreparedStatement stmt = null;
    Connection connection = getConnection();
    stmt = connection.prepareStatement("DELETE FROM issue WHERE id=?;");
    stmt.setInt(1, id);
    stmt.execute();
    close(connection,stmt,null);
  }
}
