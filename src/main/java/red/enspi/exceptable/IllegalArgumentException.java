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

import java.lang.Throwable;

/** Base Exceptable for illegal arguments. */
public class IllegalArgumentException extends java.lang.IllegalArgumentException implements Exceptable {

  public enum E implements Signal {
    IllegalArgument;

    public String template() {
      return switch (this) {
        case IllegalArgument -> "Illegal argument '{name}' ({value}); must be {requirement}.";
      };
    }

    public record Context<V>(String name, V value, String requirement) implements Signal.Context {}
  }

  private final Signal signal;
  private final Signal.Context context;

  public IllegalArgumentException(Signal signal, Signal.Context context, Throwable cause) {
    super(signal.message(context), cause);
    this.signal = (signal == null) ? E.IllegalArgument : signal;
    this.context = context;
  }

  @Override
  public Signal.Context context() {
    return this.context;
  }

  @Override
  public Signal signal() {
    return this.signal;
  }
}
