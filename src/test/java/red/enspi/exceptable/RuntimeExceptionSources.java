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

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import red.enspi.exceptable.annotation.TestSource;
import red.enspi.exceptable.RuntimeException.E;

@TestSource(exceptableClass = "RuntimeException")
public class RuntimeExceptionSources {

  public static Stream<Arguments> construct_source() {
    var cause = new java.lang.Exception("This is a test.");
    var context = new E.Context(cause, null);
    return Stream.of(
      Arguments.of(E.UnknownError, null, null),
      Arguments.of(E.UncaughtException, null, null),
      Arguments.of(E.UncaughtException, context, null),
      Arguments.of(E.UncaughtException, null, cause),
      Arguments.of(E.UncaughtException, context, cause));
  }

  public static Stream<Arguments> SignalCode_source() {
    return Stream.of(
      Arguments.of(E.UnknownError, "red.enspi.exceptable.RuntimeException.UnknownError"),
      Arguments.of(E.UncaughtException, "red.enspi.exceptable.RuntimeException.UncaughtException"));
  }

  public static Stream<Arguments> SignalMessage_source() {
    return Stream.of(
      Arguments.of(
        E.UnknownError,
        null,
        "red.enspi.exceptable.RuntimeException.UnknownError: Unknown runtime error."),
      Arguments.of(
        E.UncaughtException,
        new E.Context(new java.lang.Exception("This is a test."), null),
        "red.enspi.exceptable.RuntimeException.UncaughtException: Uncaught runtime exception:"
          + " java.lang.Exception: This is a test."));
  }
}
