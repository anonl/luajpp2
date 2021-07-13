package nl.weeaboo.lua2.vm;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import nl.weeaboo.lua2.AbstractLuaTest;
import nl.weeaboo.lua2.compiler.ScriptLoader;
import nl.weeaboo.lua2.stdlib.DebugTrace;

public final class LuaThreadTest extends AbstractLuaTest {

    @Test
    public void testDebugCallstack() {
        LuaThread thread = loadScript("vm/debug-callstack.lua");
        luaRunState.update();
        assertStackTrace(thread, "sub");

        // jump() resets the current callstack + debug callstack
        thread.jump(ScriptLoader.loadFile("vm/debug-callstack2.lua").checkclosure(1),
                LuaConstants.NONE);
        luaRunState.update();
        assertStackTrace(thread, "sub2");
    }

    private void assertStackTrace(LuaThread thread, String... expectedFunctionNamed) {
        Assert.assertEquals(Arrays.asList(expectedFunctionNamed), DebugTrace.stackTrace(thread).stream()
                .map(e -> e.getFunctionName())
                .collect(Collectors.toList()));
    }

}
