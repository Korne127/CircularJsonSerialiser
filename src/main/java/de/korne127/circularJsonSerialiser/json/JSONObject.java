package de.korne127.circularJsonSerialiser.json;

import java.util.LinkedHashMap;
import java.util.Map;

import de.korne127.circularJsonSerialiser.exceptions.DeserialiseException;
import de.korne127.circularJsonSerialiser.exceptions.JsonParseException;

/**
 * JSONObject-Klasse:<br>
 * Diese Klasse speichert eine Map und bietet Methoden, die Map in einen JSON-String
 * umzuwandeln.
 * @author Korne127
 */
public class JSONObject implements JSONElement {

	private final Map<String, Object> map;

	/**
	 * Der standardmäßige Konstruktor der Klasse:<br>
	 * Er erstellt eine leere verbundene Map.
	 */
	public JSONObject() {
		map = new LinkedHashMap<>();
	}

	/**
	 * Ein Konstruktor der Klasse:<br>
	 * Er setzt die Map auf eine angegebene Map.
	 * @param map Die angegebene Map
	 */
	public JSONObject(Map<String, Object> map) {
		this.map = map;
	}

	/**
	 * Ein Konstruktor der Klasse:<br>
	 * Er benutzt den {@link JSONReader} um einen JSON-String in ein JSON-Objekt umzuwandeln
	 * und setzt die Map auf die Map dieses Objektes.
	 * @param content Der angegebene JSON-String
	 * @throws JsonParseException Wird geworfen, falls das JSON-Objekt aus dem JSON-String nicht
	 * geparst werden konnte.
	 */
	public JSONObject(String content) throws JsonParseException {
		content = content.replaceAll("[\\n\\t]", "");
		map = ((JSONObject) JSONReader.readObject(content, 0).getValue()).map;
	}

	/**
	 * Setzt einen key mit einem dazugehörigen value in die Map.
	 * @param key Der key, der in die Map gesetzt werden soll
	 * @param value Der zu dem key dagehörige value, der in die Map gesetzt werden soll
	 */
	public void put(String key, Object value) {
		map.put(key, value);
	}

	/**
	 * Gibt das zu einem key dazugehörige Objekt aus der Map zurück, falls es existiert.
	 * @param key Der key, dessen dazugehöriges Objekt zurückgegeben werden soll
	 * @return Das Objekt, dass zu dem angegebenen key dazugehörig ist
	 * @throws DeserialiseException Wird geworfen, falls kein Objekt zu dem key gespeichert
	 * ist.
	 */
	public Object get(String key) throws DeserialiseException {
		if (map.containsKey(key)) {
			return map.get(key);
		}
		throw new DeserialiseException("JSON-Object child not found.");
	}

	/**
	 * Gibt den zu einem key dazugehörigen String zurück, falls der key existiert.
	 * @param key Der key, dessen dazugehöriger String zurückgegeben werden soll
	 * @return Der String, der zu dem angegebenen key dazugehörig ist
	 * @throws DeserialiseException Wird geworfen, falls kein String zu dem key
	 * gespeichert ist.
	 */
	public String getString(String key) throws DeserialiseException {
		Object object = get(key);
		if (object instanceof String) {
			return (String) object;
		}
		throw new DeserialiseException("Type Mismatch: JSON-Object child is not a string.");
	}

	/**
	 * Gibt ein Set zurück, welches alle keys aus der Map beinhaltet.
	 * @return Ein Set, welches alle keys aus der Map beinhaltet
	 */
	public Iterable<String> keySet() {
		return map.keySet();
	}

	/**
	 * Gibt einen formatierten String zurück, der alle Inhalte des JSON-Objektes beinhaltet.
	 * @param indentFactor Die Anzahl an Tabs, die vor den Unterelementen gesetzt werden soll
	 * @return Ein formatierter String, der alle Inhalte des JSON-Objektes beinhaltet
	 */
	public String toString(int indentFactor) {
		if (map.size() == 0) {
			return "{}";
		}
		StringBuilder json = new StringBuilder("{");
		for (String key : keySet()) {
			Object value = map.get(key);
			json.append("\n").append(getTabs(indentFactor)).append("\"").append(key).append("\": ")
					.append(elementToString(value, indentFactor)).append(",");
		}
		return json.substring(0, json.length() - 1) + "\n" + getTabs(indentFactor - 1) + "}";
	}

	/**
	 * Gibt einen formatierten String zurück, der alle Inhalte des JSON-Objektes beinhaltet.
	 * @return Ein formatierter String, der alle Inhalte des JSON-Objektes beinhaltet
	 */
	@Override
	public String toString() {
		return toString(1);
	}

}
