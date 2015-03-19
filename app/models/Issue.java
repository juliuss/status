package models;

import java.io.Serializable;
import java.util.*;
import com.google.common.base.Splitter;
import com.mysql.jdbc.StringUtils;

public class Issue implements Serializable {
  
  
  public Integer id;
  public String name;
  public String message;
  public String impact;
  public Boolean resolved;
  public Long start;
  public Long end;
  
  public Issue(int id, String name, String message, String impact, Integer resolved, Integer start, Integer end) {
    this.id = id;
    this.name = name;
    this.message = message;
    this.impact = impact;
    this.resolved = (resolved==1);
    this.start = start * 1000L;
    this.end = end * 1000L;
  }
  
  
}
