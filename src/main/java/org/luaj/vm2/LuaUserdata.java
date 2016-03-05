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
package org.luaj.vm2;

import java.io.Serializable;

import nl.weeaboo.lua2.io.LuaSerializable;

@LuaSerializable
public class LuaUserdata extends LuaValue implements Serializable {

	private static final long serialVersionUID = -2825288508171353992L;

	public final Object m_instance;
	public LuaValue m_metatable;

	public LuaUserdata(Object obj) {
        this(obj, null);
	}

	public LuaUserdata(Object obj, LuaValue metatable) {
        if (obj == null) {
            throw new LuaError("Attempt to create userdata from null object");
        }

		m_instance = obj;
		m_metatable = metatable;
	}

	@Override
	public String tojstring() {
		return String.valueOf(m_instance);
	}

	@Override
	public int type() {
		return LuaValue.TUSERDATA;
	}

	@Override
	public String typename() {
		return "userdata";
	}

	@Override
	public int hashCode() {
		return m_instance.hashCode();
	}

	public Object userdata() {
		return m_instance;
	}

	@Override
	public boolean isuserdata() {
		return true;
	}

	@Override
	public boolean isuserdata(Class<?> c) {
		return c.isAssignableFrom(m_instance.getClass());
	}

	@Override
	public Object touserdata() {
		return m_instance;
	}

	@Override
	public <T> T touserdata(Class<T> c) {
		return c.isAssignableFrom(m_instance.getClass()) ? c.cast(m_instance) : null;
	}

	@Override
	public Object optuserdata(Object defval) {
		return m_instance;
	}

	@Override
	public <T> T optuserdata(Class<T> c, T defval) {
		if (!c.isAssignableFrom(m_instance.getClass())) typerror(c.getName());
		return c.cast(m_instance);
	}

	@Override
	public LuaValue getmetatable() {
		return m_metatable;
	}

	@Override
	public LuaValue setmetatable(LuaValue metatable) {
		this.m_metatable = metatable;
		return this;
	}

	@Override
	public Object checkuserdata() {
		return m_instance;
	}

	@Override
	public <T> T checkuserdata(Class<T> c) {
		if (!c.isAssignableFrom(m_instance.getClass())) typerror(c.getName());
		return c.cast(m_instance);
	}

	@Override
	public LuaValue get(LuaValue key) {
		return m_metatable != null ? gettable(this, key) : NIL;
	}

	@Override
	public void set(LuaValue key, LuaValue value) {
		if (m_metatable == null || !settable(this, key, value)) error("cannot set " + key + " for userdata");
	}

	@Override
	public boolean equals(Object val) {
		if (this == val) return true;
		if (!(val instanceof LuaUserdata)) return false;
		LuaUserdata u = (LuaUserdata) val;
		return m_instance.equals(u.m_instance);
	}

	// equality w/ metatable processing
	@Override
	public LuaValue eq(LuaValue val) {
		return eq_b(val) ? TRUE : FALSE;
	}

	@Override
	public boolean eq_b(LuaValue val) {
		if (val.raweq(this)) return true;
		if (m_metatable == null || !val.isuserdata()) return false;
		LuaValue valmt = val.getmetatable();
		return valmt != null && LuaValue.eqmtcall(this, m_metatable, val, valmt);
	}

	// equality w/o metatable processing
	@Override
	public boolean raweq(LuaValue val) {
		return val.raweq(this);
	}

	@Override
	public boolean raweq(LuaUserdata val) {
//		System.out.printf("== %s %s\n  %d\n  %d %s %s\n  %d\n",
//				this, val,
//				(this==val)?1:0,
//				m_metatable==val.m_metatable?1:0, m_metatable, val.m_metatable,
//				m_instance==val.m_instance?1:0);

		return this == val || (m_metatable == val.m_metatable && m_instance.equals(val.m_instance));
	}

	// __eq metatag processing
	public boolean eqmt(LuaValue val) {
		if (m_metatable != null && val.isuserdata()) {
			return LuaValue.eqmtcall(this, m_metatable, val, val.getmetatable());
		} else {
			return false;
		}
	}
}
