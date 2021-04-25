package de.korne127.circularJsonSerialiser.serialiser;

import java.lang.reflect.Field;

/**
 * Die ObjectInformation-Klasse:<br>
 * Diese Klasse ist eine Hilfsklasse, die von {@link PathInformation} benutzt wird.<br>
 * Die Klasse speichert Informationen über ein Objekt, nämlich ob es sich bei dem Objekt um das Feld eines
 * anderen Objektes handelt und einen String, der den Klassennamen des Objektes und eventuell weitere Feld-bezogene
 * Namen beinhaltet.<br>
 * Dabei existieren Konstruktoren der Klasse, die diese Informationen setzen, sowie Methoden, um die Informationen
 * abzufragen.
 * @author Korne127
 */
class ObjectInformation {

	private final boolean isField;
	private final String objectInformation;

	/**
	 * Ein Konstruktor der ObjectInformation-Klasse:<br>
	 * Er wird für Objekte benutzt, bei denen es sich nicht um Felder anderer Objekte handelt.<br>
	 * isField wird dabei auf false gesetzt und der String auf den angegebenen Klassennamen.
	 * @param className Der Name der Klasse des Objektes, zu dem Informationen gespeichert werden sollen
	 */
	ObjectInformation(String className) {
		isField = false;
		objectInformation = className;
	}

	/**
	 * Ein Konstruktor der ObjectInformation-Klasse:<br>
	 * Er wird für Objekte benutzt, bei denen es sich um Felder anderer Objekte handelt.<br>
	 * isField wird dabei auf true gesetzt und der String auf Informationen des angegebenen Feldes.
	 * @param field Das zu dem Objekt, dessen Informationen gespeichert werden sollen, zugehörige Feld
	 */
	ObjectInformation(Field field) {
		isField = true;
		objectInformation = field.getType().getName() + " " +
				field.getDeclaringClass().getName() + "#" + field.getName();
	}

	/**
	 * Gibt zurück, ob das Objekt, zu dem Informationen gespeichert wurden, ein Feld ist
	 * @return true - Das Objekt, dessen Informationen gespeichert wurden, ist ein Feld;<br>
	 *     false - Das Objekt, dessen Informationen gespeichert wurden, ist kein Feld
	 */
	boolean isField() {
		return isField;
	}

	/**
	 * Gibt den String, der den Namen der Klasse des Objektes und eventuell weitere Informationen enthält, zurück
	 * @return Der String, der den Namen der Klasse des Objektes und eventuell weitere Informationen enthält
	 */
	@Override
	public String toString() {
		return objectInformation;
	}
}
