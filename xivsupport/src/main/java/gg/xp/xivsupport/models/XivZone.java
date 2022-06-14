package gg.xp.xivsupport.models;

import java.io.Serial;
import java.io.Serializable;

public class XivZone implements Serializable {

	@Serial
	private static final long serialVersionUID = 4105483326757045638L;
	// IMPORTANT: Annoyingly, these all must be 'long' instead of 'int' because the game treats them as
	// unsigned 32-bit, but Java treats them as signed, so values above 7FFFFFFF cause an overflow
	private final long id;
	private final String name;

	public XivZone(long id, String name) {
		this.id = id;
		this.name = name == null ? null : name.intern();
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return String.format("XivZone(0x%X:%s)", id, name);
	}
}
