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

import red.enspi.exceptable.Exceptable.Signal;
import red.enspi.exceptable.Exceptable.Signal.Context;
import red.enspi.exceptable.exception.RuntimeException;
import red.enspi.exceptable.signal.Runtime;

/** Utility methods for various error-handling strategies. */
public class Try {

  public static WhenIgnored whenIgnored = null;

  public static <V, S extends Signal<?>> V assume(ResultSupplier<V, S> callback) throws RuntimeException {
    return callback.invoke().assume();
  }

  /**
   * Invokes a callback, returning a Result with its value.
   *
   * <p> If the callback throws any of the given Exception types,
   *  they are returned in the Failure Result as the cause of the given Signal.
   * Throws Runtime.UncaughtException if any other Exception type is thrown.
   */
  public static <V, S extends Signal<?>>
  Result<V, S> collect(ValueSupplier<V> callback, S ifCaught, Class<?>... throwables) throws RuntimeException {
    try {
      return Result.success(callback.invoke());
    } catch (Throwable t) {
      Class<?> tc = t.getClass();
      for (Class<?> c : throwables) {
        if (c.equals(tc)) {
          return Result.failure(ifCaught, t);
        }
      }
      throw Runtime.UncaughtException.throwable(t);
    }
  }

  public static <V, S extends Signal<?>>
  Result<V, S> collect(ValueSupplier<V> callback, S ifCaught, Signal<?>... signals) throws RuntimeException {
    try {
      return Result.success(callback.invoke());
    } catch (Throwable t) {
      var rx = (RuntimeException) Runtime.UncaughtException.throwable(t);
      for (Signal<?> s : signals) {
        if (rx.has(s)) {
          return Result.failure(ifCaught, t);
        }
      }
      throw rx;
    }
  }

  public static <V, S extends Signal<?>>
  Result<V, S> collect(ResultSupplier<V, S> callback, S ifCaught, Signal<?>... signals) throws RuntimeException {
    try {
      return switch (callback.invoke()) {
        case Result.Success<V, S> success -> success;
        case Result.Failure<V, S> failure -> {
          var signal = failure.signal();
          for (Signal<?> s : signals) {
            if (signal.equals(s)) {
              yield Result.failure(ifCaught);
            }
          }
          yield failure;
        }
      };
    } catch (Throwable t) {
      var rx = (RuntimeException) Runtime.UncaughtException.throwable(t);
      for (Signal<?> s : signals) {
        if (rx.has(s)) {
          return Result.failure(ifCaught, t);
        }
      }
      throw rx;
    }
  }

  /**
   * Invokes a callback, returning its value.
   *
   * If the callback throws any of the given Exception types, they are caught and `null` is returned.
   * Throws Runtime.UncaughtException if any other Exception type is thrown.
   */
  public static <V> V ignore(ValueSupplier<V> callback, Class<?>... throwables) throws RuntimeException {
    try {
      return callback.invoke();
    } catch (Throwable t) {
      Class<?> tc = t.getClass();
      for (Class<?> c : throwables) {
        if (c.equals(tc)) {
          if (Try.whenIgnored != null) {
            Try.whenIgnored.invoke(t);
          }
          return null;
        }
      }
      throw Runtime.UncaughtException.throwable(t);
    }
  }

  public static <V> V ignore(ValueSupplier<V> callback, Signal<?>... signals) throws RuntimeException {
    try {
      return callback.invoke();
    } catch (Throwable t) {
      var rx = (RuntimeException) Runtime.UncaughtException.throwable(t);
      for (Signal<?> s : signals) {
        if (rx.has(s)) {
          if (Try.whenIgnored != null) {
            Try.whenIgnored.invoke(t);
          }
          return null;
        }
      }
      throw rx;
    }
  }

  public static <V, S extends Signal<?>>
  V ignore(ResultSupplier<V, S> callback, Signal<?>... signals) throws RuntimeException {
    try {
      return switch (callback.invoke()) {
        case Result.Success<V, S> success -> success.value();
        case Result.Failure<V, S> failure -> {
          var signal = failure.signal();
          for (Signal<?> s : signals) {
            if (signal.equals(s)) {
              if (Try.whenIgnored != null) {
                Try.whenIgnored.invoke(failure.cause());
              }
              yield null;
            }
          }
          throw (RuntimeException) Runtime.UnknownError.throwable(failure.cause());
        }
      };
    } catch (Throwable t) {
      var rx = (RuntimeException) Runtime.UncaughtException.throwable(t);
      for (Signal<?> s : signals) {
        if (rx.has(s)) {
          if (Try.whenIgnored != null) {
            Try.whenIgnored.invoke(t);
          }
          return null;
        }
      }
      throw rx;
    }
  }

  /** Invokes a callback, wrapping the return value (or any thrown exception) in a Result object. */
  public static <V, S extends Signal<?>> Result<V, S> result(ValueSupplier<V> callback) {
    try {
      return Result.success(callback.invoke());
    } catch (Throwable t) {
      return Result.failure(t);
    }
  }

  public static <V, S extends Signal<?>> Result<V, S> result(ResultSupplier<V, S> callback) {
    try {
      return callback.invoke();
    } catch (Throwable t) {
      return Result.failure(t);
    }
  }

  /**
   * Result types.
   *
   * <p> A Success Result holds the method's return value.
   * You can build a Success result directly, or by using the factory method {@code Result.success(V)}.
   *
   * <p> A Failure Result holds an error Signal describing the reason for failure.
   * It may also contain a context object and/or a cause.
   * You can build a Failure result directly, or by using one of the factory methods:
   * <ul>
   * <li> {@code Result.failure(S, Context, Throwable)}
   * <li> {@code Result.failure(S, Context)}
   * <li> {@code Result.failure(S)}
   * <li> {@code Result.failure(Throwable)}
   * </ul>
   *
   * Result objects may be inspected and used/acted upon in an enhanced switch expression.
   * For example, given a method that returns a {@code Result<Party, FauxPas>}:
   * <pre>{@code
   * switch (result) {
   *   case Result.Success party -> party.value().allNightLong();
   *   case Result.Failure fauxPas -> switch (fauxPas.signal()) {
   *     case FauxPas.minor -> Party.tryAgain();
   *     case FauxPas.major -> leave();
   *   };
   * };
   * }</pre>
   *
   * If you have no expectation that a method will produce a Failure result (or no recourse if it does),
   *  you can _assume_ it is successful and continue with your happy-path code:
   * <pre>{@code result.assuming().allNightLong();}</pre>
   * This automatically extracts the {@code .value()} from a Success result,
   *  or throws from a Failure result's {@code .signal()}.
   */
  public sealed interface Result<V, S extends Signal<?>> permits Result.Success, Result.Failure {

    /** A successful result. */
    record Success<V, S extends Signal<?>>(V value) implements Result<V, S> {

      @Override
      public V assume() {
        return this.value();
      }
    }

    /** A failure result. */
    record Failure<V, S extends Signal<?>>(S signal, Context context, Throwable cause) implements Result<V, S> {

      @SuppressWarnings("unchecked")
      public Failure {
        // fill an empty error from a given exception
        if (signal == null && cause != null) {
          signal = (S) ((cause instanceof Exceptable x) ? x.signal() : Runtime.UncaughtException);
        }
      }

      @Override
      public V assume() throws RuntimeException {
        var cause = this.cause();
        throw (cause instanceof RuntimeException rx && rx.is(Runtime.UncaughtException)) ?
          rx :
          Runtime.UncaughtException.throwable(cause);
      }
    }

    /** Factory: builds a failure Result. */
    public static <V, S extends Signal<?>> Result<V, S> failure(S signal, Context context) {
      return new Failure<>(signal, context, null);
    }

    /** Factory: builds a failure Result from the given Signal. */
    public static <V, S extends Signal<?>> Result<V, S> failure(S signal) {
      return new Failure<>(signal, null, null);
    }

    /** Factory: builds a failure Result from the given Throwable. */
    public static <V, S extends Signal<?>> Result<V, S> failure(Throwable cause) {
      return new Failure<>(null, null, cause);
    }

    /** Factory: builds a failure Result from the given Signal and cause. */
    public static <V, S extends Signal<?>> Result<V, S> failure(S signal, Throwable cause) {
      return new Failure<>(signal, null, cause);
    }

    /** Factory: builds a success Result from the given return value. */
    public static <V, S extends Signal<?>> Result<V, S> success(V value) {
      return new Success<>(value);
    }

    /** Assumes the Result is successful and tries to return its value. */
    V assume() throws RuntimeException;
  }

  public interface ValueSupplier<V> { V invoke() throws Throwable; }

  public interface ResultSupplier<V, S extends Signal<?>> { Result<V, S> invoke(); }

  public interface WhenIgnored { void invoke(Throwable t); }
}
