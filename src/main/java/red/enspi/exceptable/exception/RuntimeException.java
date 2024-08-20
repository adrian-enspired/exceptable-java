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

import java.lang.reflect.InvocationTargetException;

import red.enspi.exceptable.Exceptable;
import red.enspi.exceptable.Exceptable.Signal.Context;
import red.enspi.exceptable.signal.Runtime;

/** _Exceptable_ for Runtime errors. */
public class RuntimeException extends java.lang.RuntimeException implements Exceptable {

  private final Signal<?> signal;
  private final Context context;

  public RuntimeException() {
    this(Runtime.UnknownError, null, null);
  }

  public RuntimeException(Signal<?> signal, Context context, Throwable cause) {
    super(signal != null ? signal.message(context) : Runtime.UnknownError.message(context));
    if (cause != null && this instanceof Throwable t) {
      try {
        var setCause = Throwable.class.getDeclaredMethod("initCause", Throwable.class);
        setCause.setAccessible(true);
        setCause.invoke(t, cause);
      } catch (java.lang.NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {}
    }
    this.signal = (signal != null) ? signal : Runtime.UnknownError;
    this.context = context;
  }

  @Override
  public Context context() { return this.context; }

  @Override
  public Signal<?> signal() { return this.signal; }
}
