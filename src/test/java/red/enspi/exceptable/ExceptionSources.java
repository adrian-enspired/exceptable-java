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
import red.enspi.exceptable.signal.Error;

@TestSource(exceptableClass = "Exception")
public class ExceptionSources {

  public static Stream<Arguments> construct_source() {
    var cause = new java.lang.Exception("This is a test.");
    var context = new Error.Context(cause, null);
    return Stream.of(
      Arguments.of(Error.UnknownError, null, null),
      Arguments.of(Error.UncaughtException, null, null),
      Arguments.of(Error.UncaughtException, context, null),
      Arguments.of(Error.UncaughtException, null, cause),
      Arguments.of(Error.UncaughtException, context, cause));
  }

  public static Stream<Arguments> SignalCode_source() {
    return Stream.of(
      Arguments.of(Error.UnknownError, "red.enspi.exceptable.signal.Error.UnknownError"),
      Arguments.of(Error.UncaughtException, "red.enspi.exceptable.signal.Error.UncaughtException"));
  }

  public static Stream<Arguments> SignalMessage_source() {
    return Stream.of(
      Arguments.of(Error.UnknownError, null, "red.enspi.exceptable.signal.Error.UnknownError"),
      Arguments.of(
        Error.UncaughtException,
        null,
        "red.enspi.exceptable.signal.Error.UncaughtException"),
      Arguments.of(
        Error.UncaughtException,
        new Error.Context(new java.lang.Exception("This is a test."), null),
        "red.enspi.exceptable.signal.Error.UncaughtException: Uncaught exception: java.lang.Exception: This is a test."));
  }
}
