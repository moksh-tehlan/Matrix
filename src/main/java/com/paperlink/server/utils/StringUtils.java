package com.paperlink.server.utils;

/** Utility class for string operations */
public class StringUtils {

  /**
   * Format a string to be used as a subdomain Removes special characters, spaces, and makes
   * lowercase
   *
   * @param input String to format
   * @return Formatted subdomain string
   */
  public static String formatSubdomain(String input) {
    if (input == null || input.isEmpty()) {
      return "";
    }

    return input.toLowerCase().replaceAll("[^a-z0-9]", "").replaceAll("\\s+", "");
  }

  /**
   * Check if a string is null or empty
   *
   * @param str String to check
   * @return true if string is null or empty
   */
  public static boolean isEmpty(String str) {
    return str == null || str.trim().isEmpty();
  }

  /**
   * Convert a string to snake_case
   *
   * @param input String to convert
   * @return String in snake_case
   */
  public static String toSnakeCase(String input) {
    if (input == null || input.isEmpty()) {
      return "";
    }

    return input.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase().replaceAll("[^a-z0-9_]", "_");
  }

  /**
   * Convert a string to camelCase
   *
   * @param input String to convert
   * @return String in camelCase
   */
  public static String toCamelCase(String input) {
    if (input == null || input.isEmpty()) {
      return "";
    }

    StringBuilder result = new StringBuilder();
    boolean nextUpper = false;

    for (char c : input.toCharArray()) {
      if (c == '_' || c == '-' || c == ' ') {
        nextUpper = true;
      } else {
        if (nextUpper) {
          result.append(Character.toUpperCase(c));
          nextUpper = false;
        } else {
          result.append(Character.toLowerCase(c));
        }
      }
    }

    return result.toString();
  }
}
