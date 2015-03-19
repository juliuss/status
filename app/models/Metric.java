package models;

import java.util.*;
import com.google.common.base.Splitter;
import com.mysql.jdbc.StringUtils;

public class Metric {
  
  public Integer id;
  public String name;
  public String namespace;
  public String display;
  public String unit;
  public String operation;
  public Map<String,String> dimensions;
  public String raw;
  
  public Metric(Integer id,String name,String namespace,String display,String unit,String operation) {
    this.id = id;
    this.namespace = namespace;
    this.display = display;
    this.unit = unit;
    this.operation = operation;
    //dimensions are stored with name so we have to seperate them out
    this.raw = name;
    if (name.contains("(")) {
      String dimensions = name.substring(name.indexOf('(')+1,name.indexOf(')')).trim();
      this.dimensions = Splitter.on(",").withKeyValueSeparator(": ").split(dimensions);
      this.name = name.substring(0,name.indexOf('(')).trim();
    } else {
      this.name = name;
      this.dimensions = new HashMap<String,String>();
    }
  }
  
}
