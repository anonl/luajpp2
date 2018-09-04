package nl.weeaboo.lua2;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import nl.weeaboo.lua2.compiler.ScriptLoader;
import nl.weeaboo.lua2.vm.LuaConstants;
import nl.weeaboo.lua2.vm.LuaThread;
import nl.weeaboo.lua2.vm.Varargs;

public abstract class AbstractLuaTest {

    static {
        System.setProperty("sun.io.serialization.extendedDebugInfo", "true");

        configureLogging();
    }

    protected LuaRunState luaRunState;

    @Before
    public void initLuaRunState() throws LuaException {
        luaRunState = LuaRunState.create();
    }

    private static void configureLogging() {
        LogManager logManager = LogManager.getLogManager();
        try {
            InputStream in = AbstractLuaTest.class.getResourceAsStream("/logging.debug.properties");
            try {
                logManager.readConfiguration(in);
            } finally {
                in.close();
            }
        } catch (IOException ioe) {
            throw new AssertionError(ioe);
        }
    }

    @After
    public void deinitLuaRunState() {
        luaRunState.destroy();
    }

    protected LuaThread loadScript(String filename) {
        Varargs loadResult = ScriptLoader.loadFile(filename);
        if (loadResult.isnil(1)) {
            throw new LuaException(loadResult.tojstring(2));
        }

        LuaThread mainThread = luaRunState.getMainThread();
        mainThread.pushPending(loadResult.checkclosure(1), LuaConstants.NONE);
        return mainThread;
    }

    /**
     * Runs Lua code until all threads are finished.
     */
    public void runToCompletion() {
        for (int n = 0; n < 1000; n++) {
            luaRunState.update();

            if (luaRunState.isFinished()) {
                break;
            }
        }
        Assert.assertTrue(luaRunState.isFinished());
    }

}