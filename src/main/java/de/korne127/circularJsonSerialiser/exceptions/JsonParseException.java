package de.korne127.circularJsonSerialiser.exceptions;

/**
 * JsonParseException:<br>
 * Wird geworfen, wenn ein Fehler beim Parsen des JSON-Strings aufgetreten ist.
 * @author Korne127
 */
public class JsonParseException extends Exception {

	/**
	 * Erstellt eine JsonParseException mit einer angegebenen Fehlernachricht.
	 * @param message Die angegebene Fehlernachricht
	 */
	public JsonParseException(String message) {
		super(message);
	}

	/**
	 * Erstellt eine JsonParseException mit einer angegebenen Fehlernachricht und einem Verursacher
	 * der Exception.
	 * @param message Die angegebene Fehlernachricht
	 * @param cause Der Verursacher der Exception
	 */
	public JsonParseException(String message, Throwable cause) {
		super(message, cause);
	}

}
