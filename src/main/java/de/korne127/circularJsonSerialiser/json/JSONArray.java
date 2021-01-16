package de.korne127.circularJsonSerialiser.json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.korne127.circularJsonSerialiser.exceptions.DeserialiseException;

/**
 * JSONArray-Klasse:<br>
 * Diese Klasse speichert eine Liste und bietet Methoden, die Liste in einen JSON-String
 * umzuwandeln.
 * @author Korne127
 */
public class JSONArray implements JSONElement {

	private final List<Object> list;

	/**
	 * Der standardmäßige Konstruktor der Klasse:<br>
	 * Er erstellt eine leere Liste.
	 */
	public JSONArray() {
		list = new ArrayList<>();
	}

	/**
	 * Ein Konstruktor der Klasse:<br>
	 * Er setzt die Liste auf eine angegebene Liste.
	 * @param list Die angegebene Liste
	 */
	public JSONArray(List<Object> list) {
		this.list = list;
	}

	/**
	 * Fügt einen Wert der Liste hinzu.
	 * @param object Der Wert, der der Liste hinzugefügt werden soll
	 */
	public void put(Object object) {
		list.add(object);
	}

	/**
	 * Gibt das Objekt an der angegebene Position in der Liste zurück.
	 * @param index Die Position in der Liste, an der sich das Objekt befindet, dass zurückgegeben
	 *              werden soll
	 * @return Das Objekt, das an der angegebene Position in der Liste ist
	 * @throws DeserialiseException Wird geworfen, falls das gesuchte Objekt nicht gefunden wird.
	 */
	public Object get(int index) throws DeserialiseException {
		try {
			return list.get(index);
		} catch (IndexOutOfBoundsException e) {
			throw new DeserialiseException("JSON-Array child not found.", e);
		}
	}

	/**
	 * Gibt den String an der angegebene Position in der Liste zurück.
	 * @param index Die Position in der Liste, an der sich der String befindet, der zurückgegeben
	 *              werden soll
	 * @return Der String, das an der angegebene Position in der Liste ist
	 * @throws DeserialiseException Wird geworfen, falls das gesuchte Objekt nicht gefunden wird oder
	 * kein String ist.
	 */
	public String getString(int index) throws DeserialiseException {
		Object object = get(index);
		if (object instanceof String) {
			return (String) object;
		}
		throw new DeserialiseException("Type Mismatch: JSON-Array child is not a string.");
	}

	/**
	 * Gibt die Anzahl der Objekte in der Liste zurück
	 * @return Die Anzahl der Objekte in der Liste
	 */
	public int length() {
		return list.size();
	}

	/**
	 * Gibt ein Iterable zurück, der einen durch die Liste iterierenden Iterator, der das erste
	 * Element in der Liste überspringt, gibt.
	 * @return Ein Iterable, der einen durch die Liste iterierenden Iterator, der das erste
	 * Element in der Liste überspringt, gibt
	 */
	public Iterable<Object> skipFirst() {
		return () -> {
			Iterator<Object> iterator = list.iterator();
			iterator.next();
			return iterator;
		};
	}

	/**
	 * Gibt einen formatierten String zurück, der alle Inhalte des JSON-Arrays beinhaltet.
	 * @param indentFactor Die Anzahl an Tabs, die vor den Unterelementen gesetzt werden soll
	 * @return Ein formatierter String, der alle Inhalte des JSON-Arrays beinhaltet
	 */
	public String toString(int indentFactor) {
		if (list.size() == 0) {
			return "[]";
		}
		StringBuilder json = new StringBuilder("[");
		for (Object object : list) {
			json.append("\n").append(getTabs(indentFactor))
					.append(elementToString(object, indentFactor)).append(",");
		}
		return json.substring(0, json.length() - 1) + "\n" + getTabs(indentFactor - 1) + "]";
	}

	/**
	 * Gibt einen formatierten String zurück, der alle Inhalte des JSON-Arrays beinhaltet.
	 * @return Ein formatierter String, der alle Inhalte des JSON-Arrays beinhaltet
	 */
	@Override
	public String toString() {
		return toString(1);
	}
}
