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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import red.enspi.exceptable.Exceptable.Signal;
import red.enspi.exceptable.Exceptable.Signal.Context;

/**
 * To implement Test classes for Exceptables,
 * <ol>
 *   <li> implement this interface.
 *   <li> add the {@code ExceptableTestSource} annotation, with the classname of the Exceptable you're testing.
 *   <li> implement the source methods for each test method.
 * </ol>
 *
 * For example,
 * <pre>{@code
 * @ExceptableTestSource (exceptableClass = "fully.qualified.ExceptableName")
 * public class ExceptionTestSources {
 *   public static Stream<Arguments> SignalThrowable_source() {
 *     return Stream.of(
 *       Arguments.of( . . . ),
 *       . . .
 * }</pre>
 *
 * will generate something like...
 * <pre>{@code
 * public class ExceptableNameTest extends ExceptableTest {
 *
 *   public static Stream<Arguments> SignalThrowable_source() {
 *     return Stream.of(
 *       Arguments.of( . . . ),
 *       . . .
 *     );
 *   }
 *
 *   @ParameterizedTest
 *   @MethodSource ("SignalThrowable_source")
 *   default void SignalThrowable(Signal signal, Context context, Throwable cause) {
 *     Throwable actual = signal.throwable(context, cause);
 *     . . .
 * }</pre>
 */
public interface ExceptableTest {

  /** Provides the Exceptable Class under test. */
  Class<? extends Exceptable> exceptable();

  /**
   * Tests that an Exceptable Signal provides the expected error code.
   *
   * This is a `@ParameterizedTest` and requires a source that provides:
   * <ul>
   * <li> `Signal`: the case from a Signal enum to be tested.
   * <li> `String`: the expected error code.
   * </ul>
   */
  void SignalCode(Signal<?> signal, String expected);

  /**
   * Tests that an Exceptable.Signal case can build an error message using a given Context object.
   *
   * This is a `@ParameterizedTest` and requires a source that provides:
   * <ul>
   * <li> `Signal`: the case from a Signal enum to be tested.
   * <li> `Context`: a Context record appropriate for the Signal being tested.
   * <li> `String`: the expected error message.
   * </ul>
   *
   * For each Signal tested, you should have (at a minimum) two test cases:
   * <ul>
   * <li> with a context object.
   * <li> without.
   * </ul>
   *
   * Note this test is for successful test scenarios (i.e., correct usage).
   * However, it should also be able verify that a Signal with mismatched context is handled gracefully.
   */
  void SignalMessage(Signal<?> signal, Context context, String expected);

  /**
   * Tests that an Exceptable.Signal case can properly build an Exceptable instance.
   *
   * This is a `@ParameterizedTest` and requires a source that provides:
   * <ul>
   * <li> `Signal`: the case from a Signal enum to be tested.
   * <li> `Context`: a Context record appropriate for the Signal being tested.
   * <li> `Throwable`: an Exception to use as the cause of the Signal being tested.
   * </ul>
   *
   * This test takes the same arguments as construct() and implementations can likely use the same source;
   *  see .construct() for details.
   */
  void SignalThrowable(Signal<?> signal, Context context, Throwable cause);

  default void cause_assertions(Throwable expected, Exceptable actual) {
    // .cause()
    Throwable actualCause = actual.cause();
    assertEquals(
      expected,
      actualCause,
      String.format("Expected actual.cause() to be '%s', but saw '%s'.", expected, actualCause));
    // .rootCause()
    Throwable expectedRoot = (Throwable) actual;
    while (expectedRoot.getCause() instanceof Throwable nextCause) {
      expectedRoot = nextCause;
    }
    Throwable actualRoot = actual.rootCause();
    assertEquals(
      expectedRoot,
      actualRoot,
      String.format("Expected actual.rootCause() to be '%s', but saw '%s'.", expectedRoot, actualRoot));
  }

  default void context_assertions(Context expected, Context actual) {
    assertEquals(
      expected,
      actual,
      "Expected actual.context() to return the same Context instance as constructed.");
  }

  default void message_assertions(String expected, String actual, Signal<?> signal) {
    assertTrue(
      actual.startsWith(signal.code()),
      String.format("Expected actual.message() to be prefixed with Signal code, but saw '%s'.", actual));
    assertEquals(
      expected,
      actual,
      String.format("Expected actual.message() to be '%s', but saw '%s'.", expected, actual));
  }

  default void signal_assertions(Signal<?> expected, Exceptable actual) {
    // .is()
    assertTrue(actual.is(expected), String.format("Expected actual.is(%s) to be true.", expected));
    // .has()
    assertTrue(actual.has(expected), String.format("Expected actual.has(%s) to be true.", expected));
    // .signal()
    Signal<?> actualSignal = actual.signal();
    assertEquals(
      expected,
      actualSignal,
      String.format("Expected actual.signal() to be '%s', but saw '%s'.", expected, actualSignal));
  }
}
