package com.squareup.spoon.misc;

import com.google.common.collect.ImmutableList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;

/** A representation of {@link Throwable} suitable for serialization. */
public class StackTrace {
  private static final Pattern HEADER = Pattern.compile("(?:Caused by: )?([^:]+)(?::( .*)?)?");
  private static final Pattern MORE = Pattern.compile("\\.\\.\\. \\d+ more");
  private static final Pattern ELEMENT =
      Pattern.compile("\\s*at (.*?)\\.([^.(]+)\\((?:([^:]+):(\\d+)|Native Method)\\)");

  /** Convert a {@link Throwable} to its equivalent {@link StackTrace}. */
  public static StackTrace from(Throwable exception) {
    checkNotNull(exception);

    StackTrace cause = null;
    Throwable realCause = exception.getCause();
    if (realCause != null && realCause != exception) {
      cause = from(realCause);
    }

    Deque<Element> elements = new ArrayDeque<Element>();
    for (StackTraceElement element : exception.getStackTrace()) {
      elements.add(Element.from(element));
    }

    String className = exception.getClass().getCanonicalName();
    String message = exception.getMessage();
    return new StackTrace(className, message, elements, cause);
  }

  public static StackTrace from(String exception) {
    checkNotNull(exception);
    String parts[] = exception.replace("\r\n", "\n").split("\n");

    StackTrace last = null;
    Deque<String> messageParts = new ArrayDeque<String>();
    Deque<Element> elements = new ArrayDeque<Element>();
    boolean matchingElements = true; // Assume we will be matching elements first (bottom, up).
    for (int i = parts.length - 1; i >= 0; i--) {
      String part = parts[i];

      Matcher elementMatch = ELEMENT.matcher(part);
      Matcher moreMatch = MORE.matcher(part);
      boolean moreMatches = moreMatch.matches();
      if (elementMatch.matches() || moreMatches) {
        if (!matchingElements) {
          last = acceptTrace(messageParts, elements, last);
          elements.clear();
          messageParts.clear();
        }
        matchingElements = true;

        if (!moreMatches) {
          String className = elementMatch.group(1);
          String methodName = elementMatch.group(2);
          String fileName = elementMatch.group(3);
          boolean isNative = fileName == null;
          int line = isNative ? 0 : Integer.parseInt(elementMatch.group(4));

          elements.addFirst(new Element(className, fileName, line, methodName, isNative));
        }
      } else {
        matchingElements = false;
        messageParts.addFirst(part);
      }
    }

    return acceptTrace(messageParts, elements, last);
  }

  private static StackTrace acceptTrace(Deque<String> messageParts, Deque<Element> elements,
      StackTrace last) {
    String header = messageParts.removeFirst();
    Matcher headerMatch = HEADER.matcher(header);
    if (!headerMatch.matches()) {
      // The exception doesn't match our expected format, so fallback to something sensible so the
      // user can still see their test results
      return new StackTrace("", header, elements, last);
    }
    String exceptionClass = headerMatch.group(1);

    String messagePart = headerMatch.group(2);
    // Ensure we don't add empty leading lines.
    if (!StringUtils.isEmpty(messagePart)) {
      messageParts.addFirst(messagePart.trim());
    }
    // Remove trailing empty lines.
    if (!messageParts.isEmpty() && StringUtils.isEmpty(messageParts.peekLast())) {
      messageParts.removeLast();
    }
    String message = StringUtils.join(messageParts, "\n");
    if (message.equals("")) {
      message = null;
    }

    return new StackTrace(exceptionClass, message, elements, last);
  }

  private final String className;
  private final String message;
  private final List<Element> elements;
  private final StackTrace cause;

  public StackTrace(String className, String message, Deque<Element> elements, StackTrace cause) {
    checkNotNull(elements);

    this.className = className;
    this.message = message;
    this.elements = ImmutableList.copyOf(elements.iterator());
    this.cause = cause;
  }

  public String getClassName() {
    return className;
  }

  public String getMessage() {
    return message;
  }

  public List<Element> getElements() {
    return elements;
  }

  public StackTrace getCause() {
    return cause;
  }

  @Override public String toString() {
    if (className != null) {
      if (message != null) {
        return className + ": " + message;
      }
      return className;
    }
    return message;
  }

  /** A representation of {@link StackTraceElement} suitable for serialization. */
  public static class Element {
    static Element from(StackTraceElement e) {
      return new Element(e.getClassName(), e.getFileName(), e.getLineNumber(), e.getMethodName(),
          e.isNativeMethod());
    }

    private final String className;
    private final String fileName;
    private final int line;
    private final String methodName;
    private final boolean isNative;

    public Element(String className, String fileName, int line, String methodName,
        boolean isNative) {
      this.className = className;
      this.fileName = fileName;
      this.line = line;
      this.methodName = methodName;
      this.isNative = isNative;
    }

    public String getClassName() {
      return className;
    }

    public String getFileName() {
      return fileName;
    }

    public int getLine() {
      return line;
    }

    public String getMethodName() {
      return methodName;
    }

    public boolean isNative() {
      return isNative;
    }

    @Override public String toString() {
      if (isNative) {
        return String.format("%s.%s(Native Method)", className, methodName);
      }
      return String.format("%s.%s(%s:%d)", className, methodName, fileName, line);
    }
  }
}
