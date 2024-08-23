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
import red.enspi.exceptable.signal.ArrayIndexOutOfBounds;
import red.enspi.exceptable.signal.Checked;
import red.enspi.exceptable.signal.ClassCast;
import red.enspi.exceptable.signal.FileNotFound;
import red.enspi.exceptable.signal.IO;
import red.enspi.exceptable.signal.IllegalArgument;
import red.enspi.exceptable.signal.IllegalState;
import red.enspi.exceptable.signal.IndexOutOfBounds;
import red.enspi.exceptable.signal.Interrupted;
import red.enspi.exceptable.signal.NoSuchElement;
import red.enspi.exceptable.signal.NoSuchMethod;
import red.enspi.exceptable.signal.NullPointer;
import red.enspi.exceptable.signal.Runtime;
import red.enspi.exceptable.signal.Socket;
import red.enspi.exceptable.signal.UnknownHost;
import red.enspi.exceptable.signal.UnsupportedOperation;

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
  Result<V, S> collect(ValueSupplier<V> callback, SignalMap... signalMaps) throws RuntimeException {
    try {
      return Result.success(callback.invoke());
    } catch (Throwable t) {
      return SignalMap.evaluate(t, signalMaps);
    }
  }

  public static <V, S extends Signal<?>>
  Result<V, S> collect(ValueSupplier<V> callback, S ifCaught, Class<?>... throwables) throws RuntimeException {
    return Try.collect(callback, new ThrowablesMap(ifCaught, throwables));
  }

  public static <V, S extends Signal<?>>
  Result<V, S> collect(ValueSupplier<V> callback, S ifCaught, Signal<?>... signals) throws RuntimeException {
    return Try.collect(callback, new SignalsMap(ifCaught, signals));
  }

  public static <V, S extends Signal<?>, S2 extends Signal<?>>
  Result<V, S> collect(ResultSupplier<V, S2> callback, S ifCaught, Signal<?>... signals) throws RuntimeException {
    return Try.collect(callback, new SignalsMap(ifCaught, signals));
  }

  @SuppressWarnings("unchecked")
  public static <V, S extends Signal<?>, S2 extends Signal<?>>
  Result<V, S> collect(ResultSupplier<V, S2> callback, SignalMap... signalMaps) throws RuntimeException {
    try {
      return switch (callback.invoke()) {
        case Result.Success<V, ?> success -> (Result<V, S>) success;
        case Result.Failure<V, S2> failure -> {
          S2 actual = failure.signal();
          for (var signalMap : signalMaps) {
            if (signalMap instanceof SignalsMap signalsMap && signalsMap.isMapped(actual)) {
              yield Result.failure((S) signalsMap.ifCaught());
            }
          }
          throw Runtime.UnknownError.throwable(actual.throwable());
        }
      };
    } catch (Throwable t) {
      return SignalMap.evaluate(t, signalMaps);
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
          signal = (S) ((cause instanceof Exceptable x) ? x.signal() : this.signalFor(cause));
        }
      }

      @Override
      public V assume() throws RuntimeException {
        var cause = this.cause();
        throw (cause instanceof RuntimeException rx && rx.is(Runtime.UncaughtException)) ?
          rx :
          Runtime.UncaughtException.throwable(cause);
      }

      /** A human-readable message describing this failure. */
      public String message() {
        if (this.context() instanceof Context context) {
          Contexceptablized.stage(context, this.cause());
          return this.context().message();
        }
        return this.signal().message();
      }

      private Signal<?> signalFor(Throwable throwable) {
        return (throwable instanceof Exceptable x) ?
          x.signal() :
          switch(throwable) {
            case java.lang.IllegalArgumentException t -> IllegalArgument.UncaughtException;
            case java.lang.IllegalStateException t -> IllegalState.UncaughtException;
            case java.lang.UnsupportedOperationException t -> UnsupportedOperation.UncaughtException;
            case java.lang.NullPointerException t -> NullPointer.UncaughtException;
            case java.util.NoSuchElementException t -> NoSuchElement.UncaughtException;
            case java.io.FileNotFoundException t -> FileNotFound.UncaughtException;
            case java.lang.InterruptedException t -> Interrupted.UncaughtException;
            case java.lang.ArrayIndexOutOfBoundsException t -> ArrayIndexOutOfBounds.UncaughtException;
            case java.lang.ClassCastException t -> ClassCast.UncaughtException;
            case java.lang.NoSuchMethodException t -> NoSuchMethod.UncaughtException;
            case java.net.UnknownHostException t -> UnknownHost.UncaughtException;
            case java.net.SocketException t -> Socket.UncaughtException;
            case java.io.IOException t -> IO.UncaughtException;
            case java.lang.IndexOutOfBoundsException t -> IndexOutOfBounds.UncaughtException;
            case java.lang.RuntimeException t -> Runtime.UncaughtException;
            default -> Checked.UncaughtException;
          };
      }
    }

    /** Factory: builds a failure Result. */
    public static <V, S extends Signal<?>> Result<V, S> failure(S signal, Context context, Throwable cause) {
      return new Failure<>(signal, context, cause);
    }

    public static <V, S extends Signal<?>> Result<V, S> failure(S signal, Context context) {
      return new Failure<>(signal, context, null);
    }
    
    public static <V, S extends Signal<?>> Result<V, S> failure(S signal) {
      return new Failure<>(signal, null, null);
    }

    public static <V, S extends Signal<?>> Result<V, S> failure(S signal, Throwable cause) {
      return new Failure<>(signal, null, cause);
    }

    public static <V, S extends Signal<?>> Result<V, S> failure(Context context, Throwable cause) {
      return new Failure<>(null, context, cause);
    }

    public static <V, S extends Signal<?>> Result<V, S> failure(Throwable cause) {
      return new Failure<>(null, null, cause);
    }

    /** Factory: builds a success Result from the given return value. */
    public static <V, S extends Signal<?>> Result<V, S> success(V value) {
      return new Success<>(value);
    }

    /** Assumes the Result is successful and tries to return its value. */
    V assume() throws RuntimeException;
  }

  public interface ResultSupplier<V, S extends Signal<?>> { Result<V, S> invoke(); }

  public interface ValueSupplier<V> { V invoke() throws Throwable; }

  public sealed interface SignalMap permits SignalsMap, ThrowablesMap {

    @SuppressWarnings("unchecked")
    static <V, S extends Signal<?>> Result<V, S> evaluate(Throwable actual, SignalMap... signalMaps) {
      for (var signalMap : signalMaps) {
        if (signalMap.isMapped(actual)) {
          return Result.failure((S) signalMap.ifCaught(), actual);
        }
      }
      throw Runtime.UncaughtException.throwable(actual);
    }

    Signal<?> ifCaught();
    boolean isMapped(Throwable actual);
  }

  record SignalsMap(Signal<?> ifCaught, Signal<?>... signals) implements SignalMap {

    @Override
    public boolean isMapped(Throwable actual) {
      Exceptable actualX = (actual instanceof Exceptable x) ?
        x :
        (RuntimeException) Runtime.UncaughtException.throwable(actual);
      for (var s : this.signals()) {
        if (actualX.has(s)) {
          return true;
        }
      }
      return false;
    }

    public boolean isMapped(Signal<?> actual) {
      for (var s : this.signals()) {
        if (actual.equals(s)) {
          return true;
        }
      }
      return false;
    }
  }

  record ThrowablesMap(Signal<?> ifCaught, Class<?>... throwables) implements SignalMap {

    @Override
    public boolean isMapped(Throwable actual) {
      Class<?> actualClass = actual.getClass();
      for (var t : this.throwables()) {
        if (actualClass.equals(t)) {
          return true;
        }
      }
      return false;
    }
  }

  public interface WhenIgnored { void invoke(Throwable t); }
}
