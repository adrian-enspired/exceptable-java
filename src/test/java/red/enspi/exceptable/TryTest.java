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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import red.enspi.exceptable.signal.Error;
import red.enspi.exceptable.signal.RuntimeException;
import red.enspi.exceptable.Try.Result;

/** Tests for Results and Try utilities. */
public class TryTest {

  @Test
  void collectedExceptions() {
    assertDoesNotThrow(() -> {
      Result<String, Error> actual = Try.collectX(
        () -> new Trier().throwHappy(), List.of(RuntimeException.class), Error.UncaughtException);
      this.throwHappyResult_assertions(actual);
    });
  }

  @Test
  void uncollectedExceptions() {
    assertThrows(
      Exception.class,
      () -> Try.collectX(() -> new Trier().throwHappy(), List.of(Exception.class), Error.UncaughtException));
  }

  @Test
  void collectedSignals() {
    assertDoesNotThrow(() -> {
      Result<String, Error> actual = Try.collect(
        () -> new Trier().throwHappy(), List.of(Trier.Signal.Six), Error.UncaughtException);
      this.throwHappyResult_assertions(actual);
    });
  }

  @Test
  void uncollectedSignals() {
    assertThrows(
      Exception.class,
      () -> Try.collect(
        () -> new Trier().throwHappy(), List.of(Error.UnknownError), Error.UncaughtException));
  }

  private void throwHappyResult_assertions(Result<String, Error> actual) {
    if (actual instanceof Result.Failure<String, Error> actualFailure) {
      if (actualFailure.cause() instanceof RuntimeException x) {
        assertEquals(Trier.Signal.Six, x.signal());
      } else {
        // if the above fails, we'll hit this and fail
        assertTrue(actualFailure.cause() instanceof RuntimeException);
      }
      assertTrue(actualFailure.signal().equals(Error.UncaughtException));
    } else {
      // if the above failed, we'll hit this and fail
      assertTrue(actual instanceof Result.Failure<String, Error>);
    }
  }

  @Test
  void ignoredExceptions() {
    assertDoesNotThrow(() -> {
      String actual = Try.ignoreX(() -> new Trier().throwHappy(), List.of(RuntimeException.class));
      assertTrue(actual == null);
    });
  }

  @Test
  void ignoredSignals() {
    assertDoesNotThrow(() -> {
      String actual = Try.ignore(() -> new Trier().throwHappy(), List.of(Trier.Signal.Six));
      assertTrue(actual == null);
    });
  }

  @Test
  void unignoredExceptions() {
    assertThrows(
      Exception.class,
      () -> Try.ignoreX(() -> new Trier().throwHappy(), List.of(Exception.class)));
  }

  @Test
  void unignoredSignals() {
    assertThrows(
      Exception.class,
      () -> Try.ignore(() -> new Trier().throwHappy(), List.of(Error.UnknownError)));
  }

  @Test
  void successResult() {
    Result<String, Trier.Signal> actual = Try.result(() -> new Trier().getResult());
    this.successResult_assertions(actual);
  }

  @Test
  void successResultFromValue() {
    Result<String, Trier.Signal> actual = Try.result(() -> new Trier().getValue());
    this.successResult_assertions(actual);
  }

  private void successResult_assertions(Result<String, Trier.Signal> actual) {
    if (actual instanceof Result.Success<String, Trier.Signal> actualSuccess) {
      assertTrue(actualSuccess.value().equals("foo!"));
    } else {
      // if the above failed, we'll hit this and fail
      assertTrue(actual instanceof Result.Success<String, Trier.Signal>);
    }
  }

  @Test
  void failureResult() {
    Result<String, Trier.Signal> actual = Try.result(() -> new Trier().getSignal());
    this.failureResult_assertions(actual);
  }

  private void failureResult_assertions(Result<String, Trier.Signal> actual) {
    if (actual instanceof Result.Failure<String, Trier.Signal> actualFailure) {
      assertTrue(actualFailure.signal().equals(Trier.Signal.Six));
    } else {
      // if the above failed, we'll hit this and fail
      assertTrue(actual instanceof Result.Failure<String, Trier.Signal>);
    }
  }

  @Test
  void failureResultFromException() {
    Result<String, Trier.Signal> actual = Try.result(() -> new Trier().throwHappy());
    if (actual instanceof Result.Failure<String, Trier.Signal> actualFailure) {
      if (actualFailure.cause() instanceof RuntimeException x) {
        assertEquals(Trier.Signal.Six, x.signal());
      } else {
        // if the above fails, we'll hit this and fail
        assertTrue(actualFailure.cause() instanceof RuntimeException);
      }
      assertTrue(actualFailure.signal().equals(Trier.Signal.Six));
    } else {
      // if the above failed, we'll hit this and fail
      assertTrue(actual instanceof Result.Failure<String, Trier.Signal>);
    }
  }

  @Test
  void assumingSuccess() {
    Result<String, Trier.Signal> actual = new Trier().getResult();
    this.successResult_assertions(actual);
    assertDoesNotThrow(() -> assertEquals("foo!", actual.assuming()));
  }

  @Test
  void assumingFailure() {
    Result<String, Trier.Signal> actual = new Trier().getSignal();
    this.failureResult_assertions(actual);
    assertThrows(RuntimeException.class, () -> assertEquals("foo!", actual.assuming()));
  }

  private static class Trier {

    public static enum Signal implements Exceptable.Signal<RuntimeException> {
      Six;
      public String message() { return "He's in the hotel!"; }
    }

    public String getValue() {
      return "foo!";
    }

    public Result<String, Signal> getResult() {
      return Result.success(this.getValue());
    }

    public Result<String, Signal> getSignal() {
      return Result.failure(Signal.Six);
    }

    public String throwHappy() throws RuntimeException {
      throw Signal.Six.throwable();
    }
  }
}
