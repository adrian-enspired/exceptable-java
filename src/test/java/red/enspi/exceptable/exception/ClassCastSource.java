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
package red.enspi.exceptable.exception;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import red.enspi.exceptable.annotation.TestSource;
import red.enspi.exceptable.signal.ClassCast;

@TestSource(exceptableClass = "ClassCastException", packageName = "red.enspi.exceptable.exception")
public class ClassCastSource {

  public static Stream<Arguments> construct_source() {
    return Stream.of(
      Arguments.of(ClassCast.UnknownError, null, null),
      Arguments.of(ClassCast.UnknownError, null, new java.lang.Exception("This is a test.")));
  }

  public static Stream<Arguments> SignalCode_source() {
    return Stream.of(
      Arguments.of(ClassCast.UnknownError, "red.enspi.exceptable.signal.ClassCast.UnknownError"));
  }

  public static Stream<Arguments> SignalMessage_source() {
    return Stream.of(
      Arguments.of(
        ClassCast.UnknownError,
        null,
        "red.enspi.exceptable.signal.ClassCast.UnknownError"));
  }
}
