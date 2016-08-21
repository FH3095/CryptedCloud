package eu._4fh.cryptedcloud.test;

import java.lang.reflect.Field;

public class TestUtils {
	private TestUtils() {
	}

	static public Field getSetableField(final Object obj, final String fieldName) {
		Field result;
		try {
			result = obj.getClass().getDeclaredField(fieldName);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}

		result.setAccessible(true);
		return result;
	}

	static public void setField(final Object obj, final String fieldName, Object value) {
		Field field = getSetableField(obj, fieldName);
		try {
			field.set(obj, value);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
