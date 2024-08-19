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
//import red.enspi.exceptable.annotation.ExceptableSignal;

/** Signals for illegal arguments. */
//@ExceptableSignal(extendsFrom = "java.lang.IllegalArgumentException")
public enum IllegalArgument implements Signal<IllegalArgumentException> {
  BadValue;

  public record Context<V>(String name, V value, String requirement) implements Signal.Context {
    @Override
    public String template() {
      return "'{name}' must be {requirement}; '{value}' provided.";
    }
  }
}
