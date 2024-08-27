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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;

import java.util.Arrays;
import java.util.stream.Collectors;

import red.enspi.exceptable.exception.CheckedException;
import red.enspi.exceptable.signal.Runtime;

/**
 * Making exceptions exceptional!
 *
 * <p> Unfortunately, since java has no interface for throwables, and does not allow for default constructors,
 *  we have to make some implementation rules and assumptions.
 * Classes that implement `Exceptable`:
 * <ul>
 * <li> MUST extend from `Throwable`
 * <li> MUST declare a constructor like `(Signal error, Context context, Throwable cause)`
 * </ul>
 *
 * Classes that implement `Exceptable.Signal`:
 * <ul>
 * <li> MUST be enums
 * </ul>
 *
 * Classes that implement `Exceptable.Signal.Context`:
 * <ul>
 * <li> MUST be records
 * <li> SHOULD have components that provide information relevant to error messages
 * </ul>
 *
 * The easiest (recommend) way to meet these requirements is to extend from one of the provided base Exceptables:
 * <ul>
 * <li> `Exception`
 * <li> `RuntimeException`
 * <li> `IllegalArgumentException`
 * </ul>
 *
 * These classes also extend from the built-in exception classes of the same names,
 *  so can be handled transparently by code that expects those exception types.
 * Abstract Tests are provided to verify correct implementation of your custom Exceptables.
 */
public interface Exceptable {

  public static void _stage(Signal.Context context, Throwable cause) {
    if (context != null && cause != null) {
      Contexceptablized.stage(context, cause);
    }
  }

  /** The previous Exception (if any) in the chain. */
  default Throwable cause() {
    return this.getCause();
  }

  /** Contextual information for this Exceptable. */
  Signal.Context context();

  /** The Signal that describes the error case this Exceptable was thrown for. */
  Signal<?> signal();

  /** Does this Exceptable contain the given Signal, anywhere in its chain? */
  default boolean has(Signal<?> signal) {
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
  default boolean is(Signal<?> signal) {
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

  record ConstructArgs(Signal<?> signal, Signal.Context context, Throwable cause) {
    public ConstructArgs {
      if (signal == null) {
        signal = Runtime.UnknownError;
      }
      if (context == null) {
        context = signal.defaultContext(cause);
      }
    }
  }

  /** Specific error scenarios for this Exceptable. */
  interface Signal<T extends Throwable> {

    /** Factory: builds an Exceptable from this case. */
    @SuppressWarnings("unchecked")
    default T throwable(Context context, Throwable cause) {
      try {
        Exceptable._stage(context, cause);
        Class<T> type = this._throwableType();
        return Exceptable.class.isAssignableFrom(type) ?
          type.getDeclaredConstructor(Signal.class, Context.class, Throwable.class)
            .newInstance(this, context, cause) :
          type.getDeclaredConstructor(String.class, Throwable.class)
            .newInstance(this.message(context), cause);
      } catch (Throwable t) {
        // problem building the intended Exceptable. fall back on using a basic Exceptable.
        return (T) new CheckedException(this, context, cause);
      }
    }

    default T throwable(Context context) {
      return this.throwable(context, null);
    }

    default T throwable(Throwable cause) {
      return this.throwable(null, cause);
    }

    default T throwable() {
      return this.throwable(null, null);
    }

    /** A unique and identifiable error code for this case. */
    default String code() {
      return String.format("%s.%s", this.getClass().getName().toString(), this.toString());
    }

    /**
     * A human-readable description of this signal.
     *
     * <p> Override this method to provide a description for your Signal(s).
     * This will be used to construct the Signal's message if no Context object is provided,
     * or if the Context object does not provide a contextualized message.
     */
    default String description() {
      return null;
    }

    /** An error message for this case, including specific context where available. */
    default String message(Context context) {
      var message = this.code();
      if (context != null && context.message() instanceof String contextualized) {
        message = message + ": " + contextualized;
      } else if (this.description() instanceof String description) {
        message = message + ": " + description;
      }
      return message;
    }

    default String message() {
      return this.message(null);
    }

    default Context _defaultContext(Throwable cause) {
      //
    }

    /** The Throwable+Exceptable class this Signal must use. */
    @SuppressWarnings("unchecked")
    default Class<T> _throwableType() {
      for (var genericInterface : this.getClass().getGenericInterfaces()) {
        if (genericInterface instanceof ParameterizedType parameterizedInterface &&
          parameterizedInterface.getRawType() == Signal.class) {
          return (Class<T>) parameterizedInterface.getActualTypeArguments()[0];
        }
      }
      // for the compiler; we'll never actually get here
      return (Class<T>) Exception.class;
    }

    /**
     * Contextual information specific to one or more of this Signal's cases.
     *
     * <p> Implementations are expected to be records.
     * Be aware that default method implementations won't work as expected otherwise.
     */
    interface Context {

      /** Provides a contextualized error message. */
      default String message() {
        if (this._template() instanceof String template) {
          if (template.contains("{")) {
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
            if (template.contains("{cause}") && Contexceptablized.cause(this) instanceof Throwable cause) {
              template = template.replace("{cause}", cause.toString());
            }
            if (! template.contains("{")) {
              return template;
            }
          }
        }
        return null;
      }

      /**
       * The message template for this context class.
       *
       * <p> Override this method to provide a contextualized message format.
       * The message may include {@code{tokens}} with names corresponding to Context properties.
       * String replacement of the following context types is supported:
       * <ul>
       * <li> primitives, via {@code String.valueOf(primitive)}
       * <li> objects, via {@code object.toString()}
       * <li> arrays of primitives or objects, via above strategies, wrapped in brackets and joined by commas
       * <li> the {@code cause} of the related Exceptable,
       *  if it was instantiated via {@code Signal.throwable()} and a cause was given.
       * </ul>
       *
       * It is the implementation's responsibility to ensure that any context referenced in a message template
       *  has a meaningful string representation.
       */
      default String _template() { return null; }
    }
  }

  // Throwable methods
  Throwable getCause();
  String getMessage();
  StackTraceElement[] getStackTrace();
}
