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

import red.enspi.exceptable.Exceptable;
import red.enspi.exceptable.Exceptable.Signal.Context;

/** _Exceptable_ for FileNotFound errors. */
public class FileNotFoundException extends java.io.FileNotFoundException implements Exceptable {

  private final Signal<?> signal;
  private final Context context;

  public FileNotFoundException(ConstructArgs args) {
    super(args.message());
    this.signal = args.signal();
    this.context = args.context();
    var cause = args.cause();
    if (cause != null && this instanceof Throwable t) {
      try {
        var setCause = Throwable.class.getDeclaredMethod("initCause", Throwable.class);
        setCause.setAccessible(true);
        setCause.invoke(t, cause);
      } catch (java.lang.NoSuchMethodException | java.lang.IllegalAccessException | java.lang.reflect.InvocationTargetException e) {}
    }
  }

  @Override
  public Context context() { return this.context; }

  @Override
  public Signal<?> signal() { return this.signal; }
}
