package util;

public class Strings {
  
  public static boolean isNullOrEmpty(String ... strings) {
    for (String s : strings) {
      if (s == null || s.isEmpty()) return true;
    }
    return false;
  }
}
