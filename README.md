_exceptable_ makes exceptions exceptional!

## Signals

What are "signals"?

_Exceptable_ uses enum values to define specific error cases. The advantage is that you can have a clear list of all the error conditions that your application might encounter, in a way that both the programmer and the compiler can be aware of. Signals also allow you to avoid the expense of building exceptions when not necessary, and enables a more functional style of error handling.

Signals can also define _context_ classes for any error case, which allows you to provide runtime information with more details about what went wrong - for example, classnames, inputs/arguments, and other stateful information that can help with handling the error.

Each Signal is associated with a particular Exception type, and can construct an exception where needed.

## Exceptions

Exceptable's basic Exception types extend from java's built-in Exceptions (e.g., `red.enspi.exceptable.signal.RuntimeException` extends from `java.lang.RuntimeException`), so code that isn't aware of Exceptables can still catch classes of exceptions in a sensible way. You can build your own custom types "from scratch," but it's generally best to extend from one of these "base" Exceptables.

## Usage

### Using the Result Pattern

```java
import red.enspi.exceptable.Exceptable.Signal;
import red.enspi.exceptable.signal.IllegalArgumentException;

class Your {
  enum IllegalArgument implements Signal<IllegalArgumentException> {
    WhoEvenAreYou;
  }

  public Result<String, IllegalArgument> trulyExceptableMethod(String name) {
    if (name == null) {
      return Result.failure(IllegalArgument.WhoEvenAreYou);
    }
    return Result.success("Hello, " + name + "!");
  }
}
```
The `Result` interface is sealed, meaning we know there will only ever be `Success` or `Failure` results. This allows us to use pattern matching, with a switch expression, to handle both cases:
```java
Result<String, Error> result = Try.result(() -> new Your().trulyExceptableMethod("Billy"));
switch (result) {
  case Result.Success success -> System.out.println(success.value().toUpperCase() + "!");
  case Result.Failure failure -> {
    // this will be IllegalArgument.WhoEvenAreYou
    Error signal = failure.signal();
    // this will always be null (since we didn't provide any context)
    Error.Context context = failure.context();
    // we didn't provide a cause when we made this result, so this will be null
    Exception cause = failure.cause();
    // ...but if desired, we can create one now using the signal
    IllegalArgumentException exceptable = signal.throwable();
  };
}
```

### Working With un-Exceptable Code

Obviously, your dependencies, the java standard library, and even other parts of your own application may not use or be aware of your Exceptables. The `Try` class has tools to make it easy to continue working functionally and consistently in these cases.

#### `Try.result()`

The most basic case is where you have a method that returns a value on success, and throws on failure. You can use `Try.result()` to wrap this behavior in a `Result` object.
```java
import red.enspi.exceptable.signal.Error;
import red.enspi.exceptable.Try;
import red.enspi.exceptable.Try.Result;

class Some {
  public String normalMethod(String name) {
    if (name == null) {
      throw new java.lang.IllegalArgumentException("I'm throwing an Exception to tell you about this trivial error."):
    }
    return "Hello, " + name + "!";
  }
}

Result<String, Error> result = Try.result(() -> new Some().normalMethod("Billy"));
switch (result) {
  case Result.Success success -> System.out.println(success.value().toUpperCase() + "!");
  case Result.Failure failure -> {
    // this will always be Error.UncaughtException
    Error signal = failure.signal();
    // this will always be null (since your.normalMethod() doesn't provide any context)
    Error.Context context = failure.context();
    // this is whatever exception was thrown (an IllegalArgumentException, in this case)
    Exception cause = failure.cause();
  };
}
```

#### `Try.collect()`

If a method may throw a number of exception types that we want to handle in the same way, we can _collect_ these types and represent them as a given Signal in our Result:
```java
import red.enspi.exceptable.Exceptable.Signal;
import red.enspi.exceptable.Exception;
import red.enspi.exceptable.signal.RuntimeException;

class Some {
  public String fragileMethod(int mode) {
    switch (mode) {
      case 1 -> throw new IOException("Can't read a file I need");
      case 2 -> throw new RuntimeException("I'm too tired to keep running.");
      case 3 -> throw new NullPointerException("WHUUUPS");
      default: return "it's cool, it's cool.";
    }
  }
}

enum SomeError implements Signal<RuntimeException> {
  Recoverable, Unrecoverable;
}

try {
  Result<String, SomeError> result = Try.collect(
    () -> new Some().fragileMethod(n),
    SomeError.Recoverable,
    IOException.class, RuntimeException.class);
  switch (result) {
    case Result.Success success -> System.out.println(success.value());
    case Result.Failure failure -> System.out.println("Try again later: " + failure.cause().getMessage());
  }
} catch (Exception e) {
  System.out.println("This is not a recoverable error: " + e.cause().getMessage());
}
```
Here, if `.fragileMethod()` throws an `IOException` or a `RuntimeException`, we'll get a `Failure` result and can handle it as we please. If it throws a `NullPointerException` (or any other exception type), that exception will be rethrown as an `UncaughtException`.

The concept of "collecting" errors also works with Signals. So, if the `.fragileMethod()` instead returned a `Result` with Signals instead of throwing Exceptions, we might do something like this:
```java
try {
  Result<String, SomeError> result = Try.collect(
    () -> new Some().fragileMethod(n),
    SomeError.Recoverable,
    Some.FragileError.IO, Some.FragileError.Runtime);
  switch (result) {
    case Result.Success success -> System.out.println(success.value());
    case Result.Failure failure -> System.out.println("Try again later: " + failure.signal());
  }
} catch (Exception e) {
  System.out.println("This is not a recoverable error: " + e.cause().getMessage());
}
```

#### `Try.ignore()`

Similarly, there may be error conditions where we only need to know there _was_ a failure, and don't really care about what the specific problem was or to do anything about it. In those situations, we can _ignore_ those errors and get a normal `null` result instead:
```java
try {
  String result = Try.ignore(
    () -> new Some().fragileMethod(n),
    IOException.class, RuntimeException.class);
  if (result == null) {
    result == "some default value";
  }
} catch (Exception e) {
  System.out.println("This is not a recoverable error: " + e.cause().getMessage());
}
```
As when collecting errors, this also works with Signals, and if an Exception/Signal is encoutered that you didn't specify, it is rethrown as an `UncaughtException`.

#### `Try.assuming()`

We can take the "ignore" concept a step further and just _assume_ that the method is going to succeed. If it's not, the Exception/Signal that caused the failure will be rethrown as an `UncaughtException`:
```java
try {
  System.out.println(Try.result.assuming());
} catch (Exception e) {
  System.out.println("Well that was unexpected: " + e.cause().getMessage());
}
```
