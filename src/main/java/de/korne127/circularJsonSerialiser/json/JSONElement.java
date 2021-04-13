package de.korne127.circularJsonSerialiser.json;

import de.korne127.circularJsonSerialiser.Serialiser;

/**
 * JSONElement-Interface:<br>
 * Dies Interface umschließt JSONObject und JSONArray.<br>
 * Es stellt mehrere Methoden bereit, die von beiden dieser Klasse benutzt werden.
 * @author Korne127
 */
public interface JSONElement {

	/**
	 * Gibt einen formatierten String zurück, der alle Inhalte des JSON-Elementes beinhaltet.
	 * @param indentFactor Die Anzahl an Tabs, die vor den Unterelementen gesetzt werden soll
	 * @return Ein formatierter String, der alle Inhalte des JSON-Elementes beinhaltet
	 */
	String toString(int indentFactor);

	/**
	 * Gibt einen String mit einer bestimmten Anzahl an Tabs zurück.
	 * @param numberOfTabs Die Anzahl an Tabs, die hintereinander zurückgegeben werden sollen
	 * @return Ein String mit einer bestimmten Anzahl an Tabs
	 */
	default String getTabs(int numberOfTabs) {
		StringBuilder tabs = new StringBuilder();
		for (int i = 0; i < numberOfTabs; i++) {
			tabs.append("\t");
		}
		return tabs.toString();
	}

	/**
	 * Gibt ein Element, welches in einem JSON-Objekt oder JSON-Array enthalten ist, als String zurück.<br>
	 * Falls das Objekt null ist, wird "null" zurückgegeben.<br>
	 * Falls das Objekt ein String ist, werden Sonderzeichen escaped und der String in "" eingeschlossen
	 * zurückgegeben.<br>
	 * Falls das Objekt ein JSON-Element ist, wird die String-Representation des JSON-Elementes
	 * zurückgegeben.<br>
	 * Falls das Objekt ein char ist, wird er in '' eingeschlossen zurückgegeben.<br>
	 * Falls das Objekt eine Zahl oder ein boolean ist, wird es zurückgegeben. Dabei wird am Ende eines
	 * Bytes ein B, am Ende eines Shorts ein S, am Ende eines Longs ein L und am Ende eines Floats ein F
	 * angehängt.<br>
	 * Ansonsten wird die standardmäßige String-Representation des Objektes in "" eingeschlossen
	 * zurückgegeben.
	 * @param object Das Element, welches als String zurückgegeben werden soll
	 * @param currentIndentFactor Die Anzahl an Tabs, die die JSON-Struktur aktuell eingeschoben ist
	 * @return Eine String-Representation eines Elementes, welches in einem JSON-Objekt oder einem
	 * JSON-Array enthalten ist
	 */
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
