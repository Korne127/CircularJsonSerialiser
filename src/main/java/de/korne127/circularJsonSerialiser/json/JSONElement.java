package de.korne127.circularJsonSerialiser.json;

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
		if (object instanceof Number || object instanceof Boolean) {
			return object.toString();
		}
		return "\"" + object + "\"";
	}
}
