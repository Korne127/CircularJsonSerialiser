package de.korne127.circularJsonSerialiser.json;

import de.korne127.circularJsonSerialiser.serialiser.SerialiseUtils;

/**
 * JSONWriter-Klasse:<br>
 * Diese Klasse verfügt über statische Methoden um ein JSON-Element oder simples Objekt in
 * einen JSON-String umzuwandeln.
 * @author Korne127
 */
public class JSONWriter {

	/**
	 * Gibt ein für JSON benutzbares Objekt als JSON-String zurück.<br>
	 * Ein für JSON benutzbares Objekt ist dabei ein JSON-Element oder ein simples Objekt.<br>
	 * Es wird dafür {@link #writeElement(Object, int) writeElement(object, 0)} aufgerufen und
	 * zurückgegeben.
	 * @param object Das Objekt, das als JSON-String zurückgegeben werden soll
	 * @return Das angegebene Objekt als JSON-String
	 * @throws IllegalStateException Wird geworfen, falls das Objekt nicht für JSON benutzbar ist.
	 */
	public static String writeElement(Object object) {
		return writeElement(object, 1);
	}

	/**
	 * Gibt ein für JSON benutzbares Objekt als JSON-String zurück.<br>
	 * Falls das Objekt null ist, wird "null" zurückgegeben.<br>
	 * Falls das Objekt ein String ist, werden Sonderzeichen escaped und der String in "" eingeschlossen
	 * zurückgegeben.<br>
	 * Falls das Objekt ein JSON-Element ist, wird die String-Representation des JSON-Elementes
	 * zurückgegeben. Dabei wird die aktuelle Einrückungstiefe mit angegeben.<br>
	 * Falls das Objekt ein primitiver Datentyp bzw. dessen Wrapper-Klasse ist, wird es zurückgegeben.
	 * Dabei wird am Ende eines Bytes ein B, am Ende eines Shorts ein S, am Ende eines Longs ein L und
	 * am Ende eines Floats ein F angehängt; ein Char wird in '' eingeschlossen.<br>
	 * Ansonsten wird eine {@link IllegalStateException} geworfen.
	 * @param object Das Objekt, das als JSON-String zurückgegeben werden soll
	 * @param currentIndentFactor Die aktuelle Einrückungstiefe
	 * @return Das angegebene Objekt als JSON-String
	 * @throws IllegalStateException Wird gewofen, falls das Objekt nicht für JSON benutzbar ist.
	 */
	static String writeElement(Object object, int currentIndentFactor) {
		if (object == null) {
			return "null";
		}
		if (object instanceof String) {
			return writeString((String) object);
		}
		if (object instanceof JSONElement) {
			return ((JSONElement) object).toString(currentIndentFactor);
		}
		if (SerialiseUtils.isSimpleType(object.getClass())) {
			if (object instanceof Character) {
				return "'" + writeChar((char) object) + "'";
			} else if (object instanceof Byte) {
				return object + "B";
			} else if (object instanceof Short) {
				return object + "S";
			} else if (object instanceof Long) {
				return object + "L";
			} else if (object instanceof Float) {
				return object + "F";
			} else {
				return object.toString();
			}
		}

		throw new IllegalStateException("Object must be simple or JsonElement.");
	}

	/**
	 * Gibt einen String als JSON-String zurück.<br>
	 * Dabei wird jeder Buchstabe durchgegangen und escaped, falls er ein Sonderzeichen ist.
	 * Der resultierende String wird in "" eingeschlossen und zurückgegeben.
	 * @param string Der angegebene String, der als JSON-String zurückgegeben werden soll
	 * @return Der angegebene String als JSON-String
	 */
	private static String writeString(String string) {
		StringBuilder builder = new StringBuilder();
		for (int stringIterator = 0; stringIterator < string.length(); stringIterator++) {
			char stringChar = string.charAt(stringIterator);
			builder.append(writeChar(stringChar));
		}
		return "\"" + builder.toString() + "\"";
	}

	/**
	 * Gibt einen Char als JSON-String zurück.<br>
	 * Falls der Buchstabe ein Sonderzeichen ist, wird er escaped und zurückgegeben.
	 * Ansonsten wird der Buchstabe selbst als String zurückgegeben.
	 * @param character Der angegebene Char, der als JSON-String zurückgegeben werden soll
	 * @return Der angegebene Char als JSON-String
	 */
	private static String writeChar(char character) {
		switch (character) {
			case '\t': return "\\t";
			case '\b': return "\\b";
			case '\n': return "\\n";
			case '\r': return "\\r";
			case '\f': return "\\f";
			case '\"': return "\\\"";
			case '\'': return "\\'";
			case '\\': return "\\\\";
			default: return String.valueOf(character);
		}
	}

}
