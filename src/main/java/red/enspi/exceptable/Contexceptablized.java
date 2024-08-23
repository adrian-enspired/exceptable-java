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

import java.util.ArrayDeque;

import red.enspi.exceptable.Exceptable.Signal.Context;

/** Associates recent context instances with their source and/or cause. */
class Contexceptablized {

  private static final int size = 5;
  private static final ArrayDeque<Set> deque = new ArrayDeque<>(Contexceptablized.size);

  public static Throwable cause(Context context) {
    return (Contexceptablized.find(context) instanceof Set set) ? set.cause() : null;
  }

  public static void stage(Context context, Throwable cause) {
    if (
      context != null &&
      cause != null &&
      Contexceptablized.find(context) == null) {
      if (Contexceptablized.deque.size() == Contexceptablized.size) {
        Contexceptablized.deque.pollFirst();
      }
      Contexceptablized.deque.addLast(new Set(context, cause));
    }
  }

  private static Set find(Context context) {
    if (context != null) {
      for (var set : Contexceptablized.deque) {
        if (set.context == context) {
          return set;
        }
      }
    }
    return null;
  }

  record Set(Context context, Throwable cause) {}
}
