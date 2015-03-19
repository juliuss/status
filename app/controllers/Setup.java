package controllers;

import java.sql.SQLException;
import play.Logger;
import play.cache.Cache;
import play.mvc.*;
import util.*;

public class Setup extends BaseController {
  
  @SuppressWarnings("unused")
  @Before
  private static void authCheck() {
    try {
      if (SQLite.get("locked") != null) unauthorized();
    } catch (SQLException e) {
      Logger.error("SQLite error: %s", e);
      unauthorized(); //default to no access on db issue
    }
  }
  
  public static void firstRun() {
    renderArgs.put("setup", true);
    renderTemplate("Admin/settings.html");
  }
  
  public static void firstRunPost(String awskey, String awssec, String dcappid, String dcsec, String dcuserid, String name, String home) throws SQLException {
    if (Strings.isNullOrEmpty(awskey, awssec, dcappid, dcsec, dcuserid, name, home)) {
      flash.put("error", "All fields are required.");
      flash.keep();
      params.flash();
      Setup.firstRun();
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
}
