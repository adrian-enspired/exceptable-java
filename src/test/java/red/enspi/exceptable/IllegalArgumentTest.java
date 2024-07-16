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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import red.enspi.exceptable.Exceptable.Signal;
import red.enspi.exceptable.Exceptable.Signal.Context;
import red.enspi.exceptable.IllegalArgument.E;

public class IllegalArgumentTest implements ExceptableTest {

  @Override
  public Class<? extends Exceptable> exceptable() {
    return IllegalArgument.class;
  }

  @Override
  @ParameterizedTest
  @MethodSource("construct_source")
  public void construct(Signal signal, Context context, Throwable cause) {
    assertDoesNotThrow(() -> {
      var actual = this.exceptable()
        .getDeclaredConstructor(Signal.class, Context.class, Throwable.class)
        .newInstance(signal, context, cause);
      this.signal_assertions(signal, actual);
      this.cause_assertions(cause, actual);
      this.context_assertions(context, actual.context());
      this.message_assertions(signal.message(context), actual.message(), signal);
    });
  }

  public static Stream<Arguments> construct_source() {
    var cause = new java.lang.Exception("This is a test.");
    var context = new E.Context<Integer>("foo", 41, "the product of 6 and 7");
    return Stream.of(
      Arguments.of(E.IllegalArgument, null, null),
      Arguments.of(E.IllegalArgument, context, null),
      Arguments.of(E.IllegalArgument, null, cause),
      Arguments.of(E.IllegalArgument, context, cause));
  }

  @Override
  @ParameterizedTest
  @MethodSource("SignalCode_source")
  public void SignalCode(Signal signal, String expected) {
    var actual = signal.code();
    assertEquals(
      expected,
      actual,
      String.format("Expected %s.code() to be '%s', but saw '%s'.", signal, expected, actual));
  }

  public static Stream<Arguments> SignalCode_source() {
    return Stream.of(
      Arguments.of(E.IllegalArgument, "red.enspi.exceptable.IllegalArgument.IllegalArgument"));
  }

  @Override
  @ParameterizedTest
  @MethodSource("SignalMessage_source")
  public void SignalMessage(Signal signal, Context context, String expected) {
    // Signal.message(), Signal.code(), Signal.template(), Context.message()
    var actual = signal.message(context);
    assertEquals(
      expected,
      actual,
      String.format("Expected signal.message(%s) to produce '%s', but saw '%s'", context, expected, actual));
    this.message_assertions(expected, actual, signal);
  }

  public static Stream<Arguments> SignalMessage_source() {
    return Stream.of(
      Arguments.of(E.IllegalArgument, null, "red.enspi.exceptable.IllegalArgument.IllegalArgument"),
      Arguments.of(
        E.IllegalArgument,
        new E.Context<Integer>("foo", 41, "the product of 6 and 7"),
        "red.enspi.exceptable.IllegalArgument.IllegalArgument:"
        + " 'foo' must be the product of 6 and 7; '41' provided."));
  }

  @Override
  @ParameterizedTest
  @MethodSource("construct_source")
  public void SignalThrowable(Signal signal, Context context, Throwable cause) {
    var actual = signal.throwable(context, cause);
    // Signal.throwable()
    assertTrue(
      actual instanceof Exceptable,
      String.format(
        "Expected %s.throwable() to return an instance of Exceptable, but saw %s",
        signal,
        actual.getClass().getName()));
    // stupid java
    if (actual instanceof Exceptable actualX) {
      this.signal_assertions(signal, actualX);
      this.cause_assertions(cause, actualX);
      this.context_assertions(context, actualX.context());
      this.message_assertions(signal.message(context), actualX.message(), signal);
    }
  }
}
