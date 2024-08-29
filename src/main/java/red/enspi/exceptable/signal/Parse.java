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
package red.enspi.exceptable.signal;

import red.enspi.exceptable.Exceptable.Signal;
import red.enspi.exceptable.exception.ParseException;

/** Signals that an error has been reached or encountered while parsing. */
public enum Parse implements Signal<ParseException> {
  UncaughtException, UnknownError;

  public interface Context extends Signal.Context { Integer offset(); }

  public record UncaughtException(Integer offset) implements Context {
    @Override
    public String _template() { return "Uncaught exception: {cause}"; }
  }
  public record UnknownError(Integer offset) implements Context {}

  @Override
  public Context _defaultContext() {
    return switch(this) {
      case UncaughtException -> new Parse.UncaughtException(0);
      case UnknownError -> new Parse.UnknownError(0);
    };
  }
}
