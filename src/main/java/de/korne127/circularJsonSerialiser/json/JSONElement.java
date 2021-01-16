package de.korne127.circularJsonSerialiser.json;

import de.korne127.circularJsonSerialiser.Serialiser;

interface JSONElement {

	String toString(int indentFactor);

	default String getTabs(int numberOfTabs) {
		StringBuilder tabs = new StringBuilder();
		for (int i = 0; i < numberOfTabs; i++) {
			tabs.append("\t");
		}
		return tabs.toString();
	}

	default String elementToString(Object object, int currentIndentFactor) {
		if (object == null) {
			return "null";
		}
		if (object instanceof String) {
			String string = (String) object;
			StringBuilder builder = new StringBuilder();
			for (int stringIterator = 0; stringIterator < string.length(); stringIterator++) {
				char stringChar = string.charAt(stringIterator);
				switch (stringChar) {
					case '\t': builder.append("\\t"); break;
					case '\b': builder.append("\\b"); break;
					case '\n': builder.append("\\n"); break;
					case '\r': builder.append("\\r"); break;
					case '\f': builder.append("\\f"); break;
					case '\"': builder.append("\\\""); break;
					case '\'': builder.append("\\'"); break;
					case '\\': builder.append("\\\\"); break;
					default: builder.append(stringChar); break;
				}
			}
			return "\"" + builder.toString() + "\"";
		}
		if (object instanceof JSONElement) {
			return ((JSONElement) object).toString(currentIndentFactor + 1);
		}
		if (object instanceof Character) {
			return "'" + object + "'";
		}
		if (Serialiser.isSimpleType(object.getClass()) || object instanceof Boolean) {
			if (object instanceof Byte) {
				return object.toString() + "B";
			} else if (object instanceof Short) {
				return object.toString() + "S";
			} else if (object instanceof Long) {
				return object.toString() + "L";
			} else if (object instanceof Float) {
				return object.toString() + "F";
			} else {
				return object.toString();
			}
		}
		return "\"" + object + "\"";
	}
}
