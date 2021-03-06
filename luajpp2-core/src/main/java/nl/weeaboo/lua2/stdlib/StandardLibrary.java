package nl.weeaboo.lua2.stdlib;

import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.luajava.LuajavaLib;

/**
 * A full set of standard Lua libraries/modules
 */
public final class StandardLibrary {

    private final PackageLib packageLib;
    private final LuajavaLib luajavaLib;

    private boolean debugEnabled = true;
    private boolean unsafeIo;

    public StandardLibrary() {
        packageLib = new PackageLib();
        luajavaLib = new LuajavaLib();
    }

    /** Availability of debugging functions, including the debug library. */
    public void setDebugEnabled(boolean enable) {
        debugEnabled = enable;
    }

    /** Availability of 'unsafe' I/O functions such as writing to arbitrary files on the host system. */
    public void setAllowUnsafeIO(boolean allow) {
        unsafeIo = allow;
    }

    /** Enables loading/instantiation of arbitrary Java classes. */
    public void setAllowUnsafeClassLoading(boolean allow) {
        luajavaLib.setAllowUnsafeClassLoading(allow);
    }

    /**
     * Registers the standard library in the current Lua environment.
     * @throws LuaException If an error occurs.
     */
    public void register() throws LuaException {
        new BaseLib().register();
        packageLib.register();
        new TableLib().register();
        new StringLib().register();
        new CoroutineLib().register();
        new MathLib().register();
        ILuaIoImpl ioImpl = createIoImpl();
        new IoLib(ioImpl).register();
        new OsLib(ioImpl).register();
        luajavaLib.register();
        new ThreadLib().register();

        if (debugEnabled) {
            new DebugLib().register();
        }
    }

    private ILuaIoImpl createIoImpl() {
        if (unsafeIo) {
            return new UnsafeIo();
        } else {
            return new SafeIo();
        }
    }

}
