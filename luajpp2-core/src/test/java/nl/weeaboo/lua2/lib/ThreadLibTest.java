package nl.weeaboo.lua2.lib;

import org.junit.Assert;
import org.junit.Test;

import nl.weeaboo.lua2.AbstractLuaTest;
import nl.weeaboo.lua2.LuaException;
import nl.weeaboo.lua2.LuaTestUtil;
import nl.weeaboo.lua2.link.LuaLink;

public class ThreadLibTest extends AbstractLuaTest {

    @Test
    public void testYield() throws LuaException {
        LuaLink thread = loadScript("lib/thread/yield.lua");

        thread.update();
        // After yielding, still 10-1=9 yield frames left
        Assert.assertEquals(9, thread.getWait());

        runToCompletion();

        Assert.assertEquals(0, thread.getWait());
    }

    @Test
    public void testNewThread() {
        loadScript("lib/thread/newthread.lua");

        runToCompletion();

        LuaTestUtil.assertGlobal("result", 1);
    }

    @Test
    public void testJump() {
        loadScript("lib/thread/jump1.lua");

        runToCompletion();

        LuaTestUtil.assertGlobal("jump1Finished", false);
        LuaTestUtil.assertGlobal("jump2Finished", true);
    }

    @Test
    public void invalidJump() {
        loadScript("lib/thread/jumpinvalid.lua");

        runToCompletion();

        LuaLink mainThread = luaRunState.getMainThread();
        Assert.assertEquals(true, mainThread.isFinished());
    }

    @Test
    public void testEndCall() {
        loadScript("lib/thread/endcall.lua");

        runToCompletion();

        LuaTestUtil.assertGlobal("testEnd", false);
        LuaTestUtil.assertGlobal("scriptEnd", true);
    }

}
