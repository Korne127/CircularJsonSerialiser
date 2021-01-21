package de.korne127.circularJsonSerialiser.exceptions;

/**
 * SerialiseException:<br>
 * Wird geworfen, wenn ein Fehler beim Serialisieren des Objektes aufgetreten ist.
 * @author Korne127
 */
public class SerialiseException extends Exception {

	/**
	 * Erstellt eine SerialiseException mit einer angegebenen Fehlernachricht.
	 * @param message Die angegebene Fehlernachricht
	 */
	public SerialiseException(String message) {
		super(message);
	}

	/**
	 * Erstellt eine SerialiseException mit einer angegebenen Fehlernachricht und einem Verursacher
	 * der Exception.
	 * @param message Die angegebene Fehlernachricht
	 * @param cause Der Verursacher der Exception
	 */
	public SerialiseException(String message, Throwable cause) {
		super(message, cause);
	}

}
