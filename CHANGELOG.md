
# v3.4.2
- fix: `LuaThread.reset()` 

# v3.4.1
- fix: Persisting a `LuaTable` with userdata keys could become corrupted after loading due to changing hash codes.

# v3.4.0
- `LuaRunState` now has user-settable type coercions.

# v3.3.0
- `LuaRunState` now has a user-settable exception handler.

# v3.2.0

- eval now has read/write access to variables in the top-most frame of the call stack
- Better stack traces (available through `DebugTrace.stackTrace()`)
- Threads now have a changeable name
- Turned a few array parameters into varargs where possibly (notably `LuaValue.varargsof()`)
- Added a user-settable uncaught exception handler for Lua threads (`LuaRunState.setExceptionHandler`)
- fix: The 'call' debug hook was wrongly called with `__call` instead of `call`.
- performance: stack/upValue arrays are sized smaller and reused to reduce garbage collection pressure.
