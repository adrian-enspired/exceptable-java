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
import java.lang.Throwable;

import java.util.function.Supplier;

import red.enspi.exceptable.Exceptable.Signal;
import red.enspi.exceptable.Exceptable.Signal.Context;
import red.enspi.exceptable.Exception.E;

/** Utility methods for various error-handling strategies. */
public class Try {

  /**
   * Invokes a callback, returning its value.
   *
   * If the callback throws any of the given Exception types, they are rethrown as the cause of the given Signal.
   * Throws E.UncaughtException if any other Exception type is thrown.
   */
  public static <T, S extends Signal> T collect(
    Supplier<T> callback,
    Class<? extends Throwable>[] throwables,
    S ifCaught
  ) throws Throwable {
    try {
      return callback.get();
    } catch (Throwable t) {
      Class<?> tc = t.getClass();
      for (Class<?> c : throwables) {
        if (c.equals(tc)) {
          throw ifCaught.throwable(t);
        }
      }
      throw E.UncaughtException.throwable(t);
    }
  }

  public static <T, S extends Signal> T collect(
    Supplier<T> callback,
    Signal[] signals,
    S ifCaught
  ) throws Throwable {
    try {
      return callback.get();
    } catch (Throwable t) {
      Throwable tx = E.UncaughtException.throwable(t);
      if (tx instanceof Exceptable x) {
        for (Signal s : signals) {
          if (x.has(s)) {
            throw ifCaught.throwable(t);
          }
        }
      }
      throw tx;
    }
  }

  /**
   * Invokes a callback, returning its value.
   *
   * If the callback throws any of the given Exception types, they are caught and `null` is returned.
   * Throws E.UncaughtException if any other Exception type is thrown.
   */
  public static <T> T ignore(Supplier<T> callback, Class<? extends Throwable>[] throwables) throws Throwable {
    try {
      return callback.get();
    } catch (Throwable t) {
      Class<?> tc = t.getClass();
      for (Class<?> c : throwables) {
        if (c.equals(tc)) {
          return null;
        }
      }
      throw E.UncaughtException.throwable(t);
    }
  }

  public static <T> T ignore(Supplier<T> callback, Signal[] signals) throws Throwable {
    try {
      return callback.get();
    } catch (Throwable t) {
      Throwable tx = E.UncaughtException.throwable(t);
      if (tx instanceof Exceptable x) {
        for (Signal s : signals) {
          if (x.has(s)) {
            return null;
          }
        }
      }
      throw tx;
    }
  }

  /** Invokes a callback, wrapping the return value (or any thrown exception) in a Result object. */
  public static <V> Result<V> result(Supplier<V> callback) {
    try {
      V value = callback.get();
      return Result.success(value);
    } catch (Throwable t) {
      return Result.failure(t);
    }
  }

  /**
   * A functional Result object.
   *
   * Contains the following components:
   * - <V> `value`: the "success" value of the Result.
   * - Signal `error`: the "failure" value of the Result.
   * - Context `context`: contextual information about the failure.
   * - Throwable `cause`: the exception that caused the failure.
   *
   * For a successful result, only the `value` should be populated.
   *
   * For a failure result, the `value` must be `null`.
   * The `error` must be populated (possibly indirectly, via the `cause`).
   * The `context` and/or `cause` may be populated, if relevant and available.
   */
  public static record Result<V>(V value, Signal signal, Context context, Throwable cause) {

    /** Factory: builds a failure Result. */
    public static <V> Result<V> failure(Signal signal, Context context) {
      return new Result<>(null, signal, null, null);
    }

    /** Factory: builds a failure Result from the given Signal. */
    public static <V> Result<V> failure(Signal signal) {
      return new Result<>(null, signal, null, null);
    }

    /** Factory: builds a failure Result from the given Throwable. */
    public static <V> Result<V> failure(Throwable cause) {
      return new Result<>(null, null, null, cause);
    }

    /** Factory: builds a success Result from the given return value. */
    public static <V> Result<V> success(V value) {
      return new Result<>(value, null, null, null);
    }

    public Result {
      // fill an empty error from a given exception
      if (signal == null && cause != null) {
        signal = (cause instanceof Exceptable x) ? x.signal() : E.UncaughtException;
      }
      // enforce sanity (we cannot succeed _and_ fail). should this throw?
      if (signal == null) {
        context = null;
        cause = null;
      } else {
        value = null;
      }
    }

    /** Assumes the Result is successful and tries to return its value. */
    public V assuming() throws Throwable {
      return this.throwOnFailure().value();
    }

    /** Is this a failure Result? */
    public boolean isFailure() {
      return this.signal() != null;
    }

    /** Is this a success Result? */
    public boolean isSuccess() {
      return this.signal() == null;
    }

    /** Throws if this is a failure Result. */
    public Result<V> throwOnFailure() throws Throwable {
      if (this.isFailure()) {
        if (this.cause() instanceof Throwable t) {
          throw (t instanceof Exceptable x && x.is(E.UncaughtException)) ?
            t :
            E.UncaughtException.throwable(t);
        }
        throw this.signal().throwable();
      }
      return this;
    }
  }
}
