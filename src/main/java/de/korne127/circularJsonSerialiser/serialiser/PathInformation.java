package de.korne127.circularJsonSerialiser.serialiser;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Die PathInformation-Klasse:<br>
 * Diese Klasse ist eine Hilfsklasse, die von dem {@link Serialiser} benutzt wird, um den Pfad der durchlaufenen
 * Objekte zu speichern. Dies wird benötigt, um bei Fehlermeldungen genau anzugeben, in welchem Objekt sich der
 * (De)Serialisierungsprozess befindet.<br>
 * Die Klasse speichert eine verkettete Liste an {@link ObjectInformation Objekt-Informationen} sowie ein boolean,
 * ob als letztes ein Feld hinzugefügt wurde.
 * @author Korne127
 */
class PathInformation {

	private boolean fieldAdded;
	private final LinkedList<ObjectInformation> fieldPath;

	/**
	 * Der Konstruktor der PathInformation-Klasse:<br>
	 * Er erstellt ein neues PathInformation-Objekt mit einer leeren verketteten Liste ohne bislang hinzugefügte
	 * Objekte.
	 */
	PathInformation() {
		fieldAdded = false;
		fieldPath = new LinkedList<>();
	}

	/**
	 * Fügt ein Feld zu dem Pfad der durchlaufenen Objekte hinzu.
	 * Dazu wird in die verkettete Liste ein neues {@link ObjectInformation}-Objekt mit entsprechenden Informationen
	 * der verketteten Liste hinzugefügt und der boolean, der besagt, ob als letztes ein Feld hinzugefügt wurde,
	 * auf true gesetzt.
	 * @param field Das Feld, dass zu dem Pfad der durchlaufenen Objekte hinzugefügt werden soll
	 */
	void addField(Field field) {
		fieldPath.add(new ObjectInformation(field));
		fieldAdded = true;
	}

	/**
	 * Fügt eine Klasse zu dem Pfad der durchlaufenen Objekte hinzu.<br>
	 * Ruft dafür {@link #addClass(String)} mit dem Namen der angegebenen Klasse auf
	 * @param objectClass Die Klasse, die zu dem Pfad der durchlaufenen Objekte hinzugefügt werden soll
	 */
	void addClass(Class<?> objectClass) {
		addClass(objectClass.getName());
	}

	/**
	 * Fügt eine Klasse zu dem Pfad der durchlaufenen Objekte hinzu.<br>
	 * Wenn als letztes ein Feld hinzugefügt wurde, wird der entsprechende boolean auf false gesetzt.
	 * Dies wird getan, da alle Felder, die hinzugefügt werden, zusätzlich nochmal als die Klasse des Objektes
	 * selbst hinzugefügt werden.<br>
	 * WWenn dies nicht der Fall ist, wird ein neues {@link ObjectInformation}-Objekt mit entsprechenden Informationen
	 * der verketteten Liste hinzugefügt.
	 * @param className Der Name der Klasse, die zu dem Pfad der durchlaufenen Objekte hinzugefügt werden soll
	 */
	void addClass(String className) {
		if (fieldAdded) {
			fieldAdded = false;
		} else {
			fieldPath.add(new ObjectInformation(className));
		}
	}

	/**
	 * Entfernt das letzte Objekt von dem Pfad der durchlaufenen Objekte.<br>
	 * Dazu wird auch der Wert, ob als letztes ein Feld hinzugefügt wurde, auf false gesetzt.
	 */
	void remove() {
		fieldPath.removeLast();
		fieldAdded = false;
	}

	/**
	 * Entfernt das letzte Objekt von dem Pfad der durchlaufenen Objekte, wenn es identisch zu dem angegebene Feld
	 * ist.<br>
	 * Dies wird benötigt, da für manche Felder nach dem Deserialisieren nicht die {@link #remove()}-Methode
	 * aufgerufen wird, z.B. falls das Objekt ein String ist. Um sicher zu gehen, dass ein Feld, nach dem es fertig
	 * (de)serialisiert wurde, auch nicht mehr in dem Pfad der durchlaufenen Objekte existiert, wird daher zusätzlich
	 * diese Methode aufgerufen.<br>
	 * Falls das Feld entfernt wird, wird auch der Wert, ob als letztes ein Feld hinzugefügt wurde, auf false gesetzt.
	 * @param field Das Feld, dass entfernt werden soll, falls es das letzte Objekt auf dem Pfad der durchlaufenen
	 *              Objekte ist
	 */
	void removeField(Field field) {
		if (fieldPath.getLast().isField() &&
				fieldPath.getLast().toString().equals(new ObjectInformation(field).toString())) {
			fieldPath.removeLast();
			fieldAdded = false;
		}
	}

	/**
	 * Gibt zurück, ob der Pfad der durchlaufenen Objekte ein oder weniger Objekte beinhaltet (also nur das
	 * an den Serialiser angegebene Objekt und keine weiteren).
	 * @return true - Der Pfad der durchlaufenen Objekte beinhaltet nur ein oder weniger Objekte;<br>
	 *     false - Der Pfad der durchlaufenen Objekte beinhaltet mehr als ein Objekt
	 */
	boolean hasNoPath() {
		return fieldPath.size() <= 1;
	}

	/**
	 * Gibt zurück, ob als letztes ein Feld hinzugefügt wurde.
	 * @return true - Als letztes wurde ein Feld hinzugefügt;<br>
	 *     false - Es wurde nicht als letztes ein Feld hinzugefügt
	 */
	boolean isFieldAdded() {
		return fieldAdded;
	}

	/**
	 * Gibt die Position des aktuellen Objektes zurück.<br>
	 * Dies ist der Pfad der durchlaufenen Objekte von hinten bis zu dem letzten Feld.<br>
	 * Dabei wird für jedes Objekt die entsprechende String-Repräsentation benutzt und die einzelnen Strings mit
	 * einem " in " konkateniert.
	 * @return Die Position des aktuellen Objektes
	 */
	@Override
	public String toString() {
		StringBuilder message = new StringBuilder();
		Iterator<ObjectInformation> infoIterator = fieldPath.descendingIterator();
		while (infoIterator.hasNext()) {
			ObjectInformation info = infoIterator.next();
			message.append(info.toString());
			if (info.isField()) {
				break;
			}
			message.append(" in ");
		}
		return message.toString();
	}
}
