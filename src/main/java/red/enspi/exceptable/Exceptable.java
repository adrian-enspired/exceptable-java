/*
 * author     Adrian <adrian@enspi.red>
 * copyright  2024
 * license    GPL-3.0 (only)
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License, version 3.
 *  The right to apply the terms of later versions of the GPL is RESERVED.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with this program.
 *  If not, see <http://www.gnu.org/licenses/gpl-3.0.txt>.
 */
package red.enspi.exceptable;

import java.lang.Class;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.lang.Throwable;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Making exceptions exceptional!
 *
 * Unfortunately, since java has no interface for throwables, and does not allow for default constructors,
 *  we have to make some implementation rules and assumptions:
 *
 * Classes that implement `Exceptable`:
 * - MUST extend from `Throwable`
 * - MUST declare a constructor like `(Signal error, Context context, Throwable cause)`
 *
 * Classes that implement `Exceptable.Signal`:
 * - MUST be enums
 *
 * Classes that implement `Exceptable.Signal.Context`:
 * - MUST be records
 * - SHOULD have components that provide information relevant to error messages
 *
 * The easiest (recommend) way to meet these requirements is to extend from one of the provided base Exceptables:
 * - `Exception`
 * - `RuntimeException`
 * - `IllegalArgumentException`
 * These classes also extend from the built-in exception classes of the same names,
 *  so can be handled transparently by code that expects those exception types.
 *
 * Abstract Tests are provided to verify correct implementation of your custom Exceptables.
 */
public interface Exceptable {

  /** The previous Exception (if any) in the chain. */
  default Throwable cause() {
    return this.getCause();
  }

  /** Contextual information for this Exceptable. */
  Signal.Context context();

  /** The Signal that describes the error case this Exceptable was thrown for. */
  Signal signal();

  /** Does this Exceptable contain the given Signal, anywhere in its chain? */
  default boolean has(Signal signal) {
    if (this instanceof Throwable t) {
      while (t instanceof Throwable) {
        if (t instanceof Exceptable x && x.signal() == signal) {
          return true;
        }
        t = t.getCause();
      }
    }
    return false;
  }

  /** Was this Exceptable directly caused by the given Signal? */
  default boolean is(Signal signal) {
    return this.signal() == signal;
  }

  /** The human-readable error message. */
  default String message() {
    return this.getMessage();
  }

  /** Finds the previous-most Throwable in the Exceptable's chain (which may be this Exceptable). */
  default Throwable rootCause() {
    Throwable prev;
    if (this instanceof Throwable root) {
      while ((prev = root.getCause()) != null) {
        root = prev;
      }
      return root;
    }
    return null;
  }

  /** Specific error scenarios for this Exceptable. */
  interface Signal {

    /** Factory: builds an Exceptable from this case. */
    default Throwable throwable(Context context, Throwable cause) {
      try {
        return Throwable.class.cast(
          this.throwableType()
            .getDeclaredConstructor(Signal.class, Context.class, Throwable.class)
            .newInstance(this, context, cause));
      } catch (Throwable t) {
        // problem building the intended Exceptable. fall back on using a basic Exceptable.
        return new Exception(this, context, cause);
      }
    }

    default Throwable throwable(Context context) {
      return this.throwable(context, null);
    }

    default Throwable throwable(Throwable cause) {
      return this.throwable(null, cause);
    }

    default Throwable throwable() {
      return this.throwable(null, null);
    }

    /** A unique and identifiable error code for this case. */
    default String code() {
      return String.format("%s.%s", this.getClass().getEnclosingClass().getName(), this.toString());
    }

    /** An error message for this case, including specific context where available. */
    default String message(Context context) {
      String template = this.template();
      if (! template.contains("{")) {
        // no formatting; return plain template
        return String.format("%s: %s", this.code(), template);
      }
      if (context != null) {
        // custom message
        String message = context.message(template);
        if (! message.contains("{")) {
          // successful format
          return String.format("%s: %s", this.code(), message);
        }
      }
      // unsuccessful format; omit custom message
      return this.code();
    }

    /**
     * A template for the error message for this case, possibly with {token}s for context.
     *
     * Override this method to return case-specific error message for each case the Signal defines
     *  (e.g., using switch (this) { ... }).
     * Templates may be literal text,
     *  or include "{tokens}" which will be replaced with values from the ErrorContext record.
     * Currently, there is no support for a literal "{" in a template.
     */
    default String template() {
      return "An error occured.";
    }

    /**
     * The Throwable+Exceptable class this Signal must use.
     *
     * Override this method IF your Signal class is not enclosed by your desired Exceptable class -
     *  otherwise, just let it be.
     * You should keep the same checks in place
     *  (ensuring the Class both extends Throwable, and implements Exceptable)
     *  as there's no way to enforce a type like <X extends Throwable & Exceptable>.
     */
    default Class<?> throwableType() {
      Class<?> c = this.getClass().getEnclosingClass();
      return (Exceptable.class.isAssignableFrom(c) && Throwable.class.isAssignableFrom(c)) ?
        c :
        Exception.class;
    }

    /** Contextual information specific to one or more of this Signal's cases. */
    interface Context {

      /**
       * Formats the given template based on contextual information.
       *
       * Supports string replacement of the follow context types:
       * - primitives, via String.valueOf()
       * - objects, via .toString()
       * - arrays of primitives or objects, via above strategies, wrapped in brackets and joined by commas.
       *
       * It is the implementation's responsibility to ensure that any context referenced in a message template
       *  has a meaningful string representation.
       */
      default String message(String template) {
        // non ops
        if (template == null) {
          return "";
        }
        if (! template.contains("{")) {
          return template;
        }
        // iterate and stringify record components
        for (RecordComponent rc : this.getClass().getRecordComponents()) {
          try {
            Class<?> type = rc.getType();
            String value;
            if (type.isArray()) {
              value = "[" +
                Arrays.stream((Object[]) rc.getAccessor().invoke(this))
                  .map(String::valueOf)
                  .collect(Collectors.joining(", ")) +
                "]";
            } else {
              value = String.valueOf(rc.getAccessor().invoke(this));
            }
            // do replacement
            template = template.replace("{" + rc.getName() + "}", value);
          } catch (IllegalAccessException | InvocationTargetException e) {
            // ignore; move on to next
          }
        }
        return template;
      }
    }
  }

  // Throwable methods
  Throwable getCause();
  String getMessage();
  StackTraceElement[] getStackTrace();
  String toString();
}
