package de.korne127.circularJsonSerialiser.json;

/**
 * JSONResult-Klasse:<br>
 * Diese Klasse speichert einen Wert sowie eine Zahl.<br>
 * Sie wird im Deserialisierungsprozess benutzt, indem der Wert eines Unterelementes sowie die
 * als Zahl kodierte Position, an der das Unterelement endet, gespeichert wird.
 * @author Korne127
 */
class JSONResult {
	private final Object value;
	private final int endPosition;

	/**
	 * Der standardmäßige Konstruktor der Klasse:<br>
	 * Er erstellt ein JSONResult Objekt mit einem Wert und einer als Zahl kodierten Position.
	 * @param value Der Inhalt des Unterelementes, der gespeichert werden soll
	 * @param endPosition Die als Zahl kodierte Endposition des Unterelementes
	 */
	JSONResult(Object value, int endPosition) {
		this.value = value;
		this.endPosition = endPosition;
	}

	/**
	 * Gibt den gespeicherten Wert des Unterelementes zurück.
	 * @return Der gespeicherte Wert des Unterelementes
	 */
	Object getValue() {
		return value;
	}

	/**
	 * Gibt die gespeicherte als Zahl kodierte Endposition des Unterelementes zurück.
	 * @return Die gespeicherte als Zahl kodierte Endposition des Unterelementes
	 */
	int getEndPosition() {
		return endPosition;
	}
}
