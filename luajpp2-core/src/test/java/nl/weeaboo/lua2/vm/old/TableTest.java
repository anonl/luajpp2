/*******************************************************************************
 * Copyright (c) 2009 Luaj.org. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/

package nl.weeaboo.lua2.vm.old;

import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import nl.weeaboo.lua2.vm.LuaInteger;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaTable;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.TableTester;
import nl.weeaboo.lua2.vm.Varargs;

public class TableTest {

    protected LuaTable newTable() {
        return new LuaTable();
    }

    protected LuaTable newTable(int narray, int nhash) {
        return new LuaTable(narray, nhash);
    }

    @Test
    public void testInOrderIntegerKeyInsertion() {
        LuaTable t = newTable();

        for (int i = 1; i <= 32; ++i) {
            t.set(i, LuaValue.valueOf("Test Value! " + i));
        }

        // Ensure all keys are still there.
        for (int i = 1; i <= 32; ++i) {
            Assert.assertEquals("Test Value! " + i, t.get(i).tojstring());
        }

        // Ensure capacities make sense
        Assert.assertEquals(0, TableTester.getHashLength(t));

        Assert.assertTrue(TableTester.getArrayLength(t) >= 32);
        Assert.assertTrue(TableTester.getArrayLength(t) <= 64);

    }

    @Test
    public void testRekeyCount() {
        LuaTable t = newTable();

        // NOTE: This order of insertion is important.
        t.set(3, LuaInteger.valueOf(3));
        t.set(1, LuaInteger.valueOf(1));
        t.set(5, LuaInteger.valueOf(5));
        t.set(4, LuaInteger.valueOf(4));
        t.set(6, LuaInteger.valueOf(6));
        t.set(2, LuaInteger.valueOf(2));

        for (int i = 1; i < 6; ++i) {
            Assert.assertEquals(LuaInteger.valueOf(i), t.get(i));
        }

        Assert.assertTrue(TableTester.getArrayLength(t) >= 3);
        Assert.assertTrue(TableTester.getArrayLength(t) <= 12);
        Assert.assertTrue(TableTester.getHashLength(t) <= 3);
    }

    @Test
    public void testOutOfOrderIntegerKeyInsertion() {
        LuaTable t = newTable();

        for (int i = 32; i > 0; --i) {
            t.set(i, LuaValue.valueOf("Test Value! " + i));
        }

        // Ensure all keys are still there.
        for (int i = 1; i <= 32; ++i) {
            Assert.assertEquals("Test Value! " + i, t.get(i).tojstring());
        }

        // Ensure capacities make sense
        Assert.assertEquals(32, TableTester.getArrayLength(t));
        Assert.assertEquals(0, TableTester.getHashLength(t));
    }

    @Test
    public void testStringAndIntegerKeys() {
        LuaTable t = newTable();

        for (int i = 0; i < 10; ++i) {
            LuaString str = LuaValue.valueOf(String.valueOf(i));
            t.set(i, str);
            t.set(str, LuaInteger.valueOf(i));
        }

        Assert.assertTrue(TableTester.getArrayLength(t) >= 8); // 1, 2, ..., 9
        Assert.assertTrue(TableTester.getArrayLength(t) <= 16);
        Assert.assertTrue("was: " + TableTester.getHashLength(t),
                TableTester.getHashLength(t) >= 11); // 0, "0", "1", ..., "9"
        Assert.assertTrue(TableTester.getHashLength(t) <= 33);

        List<LuaValue> keys = t.keys();

        int intKeys = 0;
        int stringKeys = 0;

        Assert.assertEquals(20, keys.size());
        for (LuaValue k : keys) {
            if (k instanceof LuaInteger) {
                final int ik = k.toint();
                Assert.assertTrue(ik >= 0 && ik < 10);
                final int mask = 1 << ik;
                Assert.assertTrue((intKeys & mask) == 0);
                intKeys |= mask;
            } else if (k instanceof LuaString) {
                final int ik = Integer.parseInt(k.strvalue().tojstring());
                Assert.assertEquals(String.valueOf(ik), k.strvalue().tojstring());
                Assert.assertTrue(ik >= 0 && ik < 10);
                final int mask = 1 << ik;
                Assert.assertTrue("Key \"" + ik + "\" found more than once", (stringKeys & mask) == 0);
                stringKeys |= mask;
            } else {
                Assert.fail("Unexpected type of key found");
            }
        }

        Assert.assertEquals(0x03FF, intKeys);
        Assert.assertEquals(0x03FF, stringKeys);
    }

    @Test
    public void testBadInitialCapacity() {
        LuaTable t = newTable(0, 1);

        t.set("test", LuaValue.valueOf("foo"));
        t.set("explode", LuaValue.valueOf("explode"));
        Assert.assertEquals(2, t.keyCount());
    }

    @Test
    public void testRemove0() {
        LuaTable t = newTable(2, 0);

        t.set(1, LuaValue.valueOf("foo"));
        t.set(2, LuaValue.valueOf("bah"));
        Assert.assertNotSame(NIL, t.get(1));
        Assert.assertNotSame(NIL, t.get(2));
        Assert.assertEquals(NIL, t.get(3));

        t.set(1, NIL);
        t.set(2, NIL);
        t.set(3, NIL);
        Assert.assertEquals(NIL, t.get(1));
        Assert.assertEquals(NIL, t.get(2));
        Assert.assertEquals(NIL, t.get(3));
    }

    @Test
    public void testRemove1() {
        LuaTable t = newTable(0, 1);

        t.set("test", LuaValue.valueOf("foo"));
        t.set("explode", NIL);
        t.set(42, NIL);
        t.set(newTable(), NIL);
        t.set("test", NIL);
        Assert.assertEquals(0, t.keyCount());

        t.set(10, LuaInteger.valueOf(5));
        t.set(10, NIL);
        Assert.assertEquals(0, t.keyCount());
    }

    @Test
    public void testRemove2() {
        LuaTable t = newTable(0, 1);

        t.set("test", LuaValue.valueOf("foo"));
        t.set("string", LuaInteger.valueOf(10));
        Assert.assertEquals(2, t.keyCount());

        t.set("string", NIL);
        t.set("three", LuaValue.valueOf(3.14));
        Assert.assertEquals(2, t.keyCount());

        t.set("test", NIL);
        Assert.assertEquals(1, t.keyCount());

        t.set(10, LuaInteger.valueOf(5));
        Assert.assertEquals(2, t.keyCount());

        t.set(10, NIL);
        Assert.assertEquals(1, t.keyCount());

        t.set("three", NIL);
        Assert.assertEquals(0, t.keyCount());
    }

    @Test
    public void testShrinkNonPowerOfTwoArray() {
        LuaTable t = newTable(6, 2);

        t.set(1, "one");
        t.set(2, "two");
        t.set(3, "three");
        t.set(4, "four");
        t.set(5, "five");
        t.set(6, "six");

        t.set("aa", "aaa");
        t.set("bb", "bbb");

        t.set(3, NIL);
        t.set(4, NIL);
        t.set(6, NIL);

        t.set("cc", "ccc");
        t.set("dd", "ddd");

        Assert.assertEquals(4, TableTester.getArrayLength(t));
        Assert.assertTrue(TableTester.getHashLength(t) < 10);
        Assert.assertEquals(5, TableTester.getHashEntries(t));
        Assert.assertEquals("one", t.get(1).tojstring());
        Assert.assertEquals("two", t.get(2).tojstring());
        Assert.assertEquals(NIL, t.get(3));
        Assert.assertEquals(NIL, t.get(4));
        Assert.assertEquals("five", t.get(5).tojstring());
        Assert.assertEquals(NIL, t.get(6));
        Assert.assertEquals("aaa", t.get("aa").tojstring());
        Assert.assertEquals("bbb", t.get("bb").tojstring());
        Assert.assertEquals("ccc", t.get("cc").tojstring());
        Assert.assertEquals("ddd", t.get("dd").tojstring());
    }

    @Test
    public void testInOrderLuaLength() {
        LuaTable t = newTable();

        for (int i = 1; i <= 32; ++i) {
            t.set(i, LuaValue.valueOf("Test Value! " + i));
            Assert.assertEquals(i, t.length());
        }
    }

    @Test
    public void testOutOfOrderLuaLength() {
        LuaTable t = newTable();

        for (int j = 8; j < 32; j += 8) {
            for (int i = j; i > 0; --i) {
                t.set(i, LuaValue.valueOf("Test Value! " + i));
            }
            Assert.assertEquals(j, t.length());
        }
    }

    @Test
    public void testStringKeysLuaLength() {
        LuaTable t = newTable();

        for (int i = 1; i <= 32; ++i) {
            t.set("str-" + i, LuaValue.valueOf("String Key Test Value! " + i));
            Assert.assertEquals(0, t.length());
        }
    }

    @Test
    public void testMixedKeysLuaLength() {
        LuaTable t = newTable();

        for (int i = 1; i <= 32; ++i) {
            t.set("str-" + i, LuaValue.valueOf("String Key Test Value! " + i));
            t.set(i, LuaValue.valueOf("Int Key Test Value! " + i));
            Assert.assertEquals(i, t.length());
        }
    }

    private static final void compareLists(LuaTable t, List<? extends LuaValue> v) {
        int n = v.size();
        Assert.assertEquals(v.size(), t.length());
        for (int j = 0; j < n; j++) {
            Object vj = v.get(j);
            Object tj = t.get(j + 1).tojstring();
            vj = ((LuaString)vj).tojstring();
            Assert.assertEquals(vj, tj);
        }
    }

    @Test
    public void testInsertBeginningOfList() {
        LuaTable t = newTable();
        List<LuaValue> v = new ArrayList<>();

        for (int i = 1; i <= 32; ++i) {
            LuaString test = LuaValue.valueOf("Test Value! " + i);
            t.insert(1, test);
            v.add(0, test);
            compareLists(t, v);
        }
    }

    @Test
    public void testInsertEndOfList() {
        LuaTable t = newTable();
        List<LuaValue> v = new ArrayList<>();

        for (int i = 1; i <= 32; ++i) {
            LuaString test = LuaValue.valueOf("Test Value! " + i);
            t.insert(0, test);
            v.add(v.size(), test);
            compareLists(t, v);
        }
    }

    @Test
    public void testInsertMiddleOfList() {
        LuaTable t = newTable();
        List<LuaValue> v = new ArrayList<>();

        for (int i = 1; i <= 32; ++i) {
            LuaString test = LuaValue.valueOf("Test Value! " + i);
            int m = i / 2;
            t.insert(m + 1, test);
            v.add(m, test);
            compareLists(t, v);
        }
    }

    private static final void prefillLists(LuaTable t, List<LuaValue> v) {
        for (int i = 1; i <= 32; ++i) {
            LuaString test = LuaValue.valueOf("Test Value! " + i);
            t.insert(0, test);
            v.add(v.size(), test);
        }
    }

    @Test
    public void testRemoveBeginningOfList() {
        LuaTable t = newTable();
        List<LuaValue> v = new ArrayList<>();
        prefillLists(t, v);
        for (int i = 1; i <= 32; ++i) {
            t.remove(1);
            v.remove(0);
            compareLists(t, v);
        }
    }

    @Test
    public void testRemoveEndOfList() {
        LuaTable t = newTable();
        List<LuaValue> v = new ArrayList<>();
        prefillLists(t, v);
        for (int i = 1; i <= 32; ++i) {
            t.remove(0);
            v.remove(v.size() - 1);
            compareLists(t, v);
        }
    }

    @Test
    public void testRemoveMiddleOfList() {
        LuaTable t = newTable();
        List<LuaValue> v = new ArrayList<>();
        prefillLists(t, v);
        for (int i = 1; i <= 32; ++i) {
            int m = v.size() / 2;
            t.remove(m + 1);
            v.remove(m);
            compareLists(t, v);
        }
    }

    @Test
    public void testRemoveWhileIterating() {
        LuaTable t = LuaValue.tableOf(
                new LuaValue[] { LuaValue.valueOf("a"), LuaValue.valueOf("aa"), LuaValue.valueOf("b"),
                        LuaValue.valueOf("bb"), LuaValue.valueOf("c"), LuaValue.valueOf("cc"),
                        LuaValue.valueOf("d"), LuaValue.valueOf("dd"), LuaValue.valueOf("e"),
                        LuaValue.valueOf("ee"), },
                new LuaValue[] { LuaValue.valueOf("11"), LuaValue.valueOf("22"), LuaValue.valueOf("33"),
                        LuaValue.valueOf("44"), LuaValue.valueOf("55"), });
        // Find expected order after removal.
        java.util.List<String> expected = new java.util.ArrayList<>();
        Varargs n;
        int i;
        for (n = t.next(NIL), i = 0; !n.arg1().isnil(); n = t.next(n.arg1()), ++i) {
            if (i % 2 == 0) {
                expected.add(n.arg1() + "=" + n.arg(2));
            }
        }
        // Remove every other key while iterating over the table.
        for (n = t.next(NIL), i = 0; !n.arg1().isnil(); n = t.next(n.arg1()), ++i) {
            if (i % 2 != 0) {
                t.set(n.arg1(), NIL);
            }
        }
        // Iterate over remaining table, and form list of entries still in table.
        java.util.List<String> actual = new java.util.ArrayList<>();
        for (n = t.next(NIL); !n.arg1().isnil(); n = t.next(n.arg1())) {
            actual.add(n.arg1() + "=" + n.arg(2));
        }
        Assert.assertEquals(expected, actual);
    }
}
