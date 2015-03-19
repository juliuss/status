package util;

import java.util.*;
import com.amazonaws.services.cloudwatch.model.*;

public class SortableDatapoint implements Comparable {
  
  public Datapoint datapoint = null;

  public SortableDatapoint(Datapoint d) {
    this.datapoint = d;
  }
  
  public int compareTo(Object o) {
    SortableDatapoint t = (SortableDatapoint)(o);
    if (datapoint.getTimestamp().getTime() < t.datapoint.getTimestamp().getTime()) {
      return -1;
    } else if (datapoint.getTimestamp().getTime() > t.datapoint.getTimestamp().getTime()) {
      return 1;
    }
    return 0;
  }
  
  public static List<SortableDatapoint> convertDatapointsToSortableDatapoints(List<Datapoint> list) {
    List l = new ArrayList<SortableDatapoint>();
    for (Datapoint d : list) {
      l.add(new SortableDatapoint(d));
    }
    return l;
  }

}