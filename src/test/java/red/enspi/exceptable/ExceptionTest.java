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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import red.enspi.exceptable.Exceptable.Signal;
import red.enspi.exceptable.Exceptable.Signal.Context;

public class ExceptionTest implements ExceptableTest {

  public static Stream<Arguments> SignalThrowable_source() {
    Throwable cause = new java.lang.Exception("This is a test.");
    return Stream.of(
      Arguments.of(Exception.E.UnknownError, null, null),
      Arguments.of(Exception.E.UncaughtException, new Exception.E.Context(cause, null), null),
      Arguments.of(Exception.E.UncaughtException, null, cause),
      Arguments.of(Exception.E.UncaughtException, new Exception.E.Context(cause, null), cause));
  }

  @Override
  @ParameterizedTest
  @MethodSource("SignalThrowable_source")
  public void SignalThrowable(Signal signal, Context context, Throwable cause) {
    Throwable actual = signal.throwable(context, cause);
    // Signal.throwable()
    assertTrue(actual instanceof Exceptable);
    // stupid java
    if (actual instanceof Exceptable actualX) {
      this.signal_assertions(signal, actualX);
      this.cause_assertions(cause, actualX);
      this.message_assertions(signal.message(context), actualX);
    }
  }
}
