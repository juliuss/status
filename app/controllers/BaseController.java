package controllers;

import java.sql.SQLException;
import play.Logger;
import play.mvc.*;

public class BaseController extends Controller {
  
  @Catch(value={SQLException.class})
  public static void onSQLException(SQLException e) {
    Logger.error("SQLite error: %s", e);
    error(e);
  }
  
}
