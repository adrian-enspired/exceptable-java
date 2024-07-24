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

import java.lang.Throwable;
import red.enspi.exceptable.Exception;
import red.enspi.exceptable.Exceptable.Signal;

public enum Error implements Signal {
  UncaughtException("Uncaught exception: {cause}"),
  UnknownError("Unknown error.");

  private String template;
  private Error(String template) { this.template = template; }
  public String template() { return this.template; }
  @Override
  public Class<?> throwableType() { return Exception.class; }

  public record Context(Throwable cause, Signal.Context more) implements Signal.Context {}
}
