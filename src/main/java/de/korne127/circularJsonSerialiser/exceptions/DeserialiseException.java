package de.korne127.circularJsonSerialiser.exceptions;

/**
 * DeserialiseException:<br>
 * Wird geworfen, wenn ein Fehler beim Deserialisen des Objektes aufgetreten ist.
 * @author Korne127
 */
public class DeserialiseException extends Exception {

	/**
	 * Erstellt eine DeserialiseException mit einer angegebenen Fehlernachricht.
	 * @param message Die angegebene Fehlernachricht
	 */
	public DeserialiseException(String message) {
		super(message);
	}

	/**
	 * Erstellt eine DeserialiseException mit einer angegebenen Fehlernachricht und einem Verursacher
	 * der Exception.
	 * @param message Die angegebene Fehlernachricht
	 * @param cause Der Verursacher der Exception
	 */
	public DeserialiseException(String message, Throwable cause) {
		super(message, cause);
	}

}
