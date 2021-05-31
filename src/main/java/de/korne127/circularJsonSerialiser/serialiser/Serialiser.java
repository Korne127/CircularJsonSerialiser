package de.korne127.circularJsonSerialiser.serialiser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.korne127.circularJsonSerialiser.annotations.AfterDeserialise;
import de.korne127.circularJsonSerialiser.annotations.BeforeSerialise;
import de.korne127.circularJsonSerialiser.annotations.SerialiseIgnore;
import de.korne127.circularJsonSerialiser.annotations.Setter;
import de.korne127.circularJsonSerialiser.exceptions.DeserialiseException;
import de.korne127.circularJsonSerialiser.exceptions.JsonParseException;
import de.korne127.circularJsonSerialiser.exceptions.SerialiseException;
import de.korne127.circularJsonSerialiser.json.JSONArray;
import de.korne127.circularJsonSerialiser.json.JSONObject;
import de.korne127.circularJsonSerialiser.json.JSONWriter;

/**
 * Serialiser-Klasse:<br>
 * Diese Klasse fungiert als Fassade des Serialisers.<br>
 * Es werden Methoden bereitgestellt, um die Konfiguration, wie sich der (De)Serialiser verhält, einzustellen.<br>
 * Zusätzlich wird Zugriff auf den Serialiser, der ein angegebenes Objekt in ein JSON-Objekt umwandelt und als
 * String zurückgibt, sowie auf den Deserialiser, der ein angegebenes JSON-Objekt, welches durch den Serialiser
 * berechnet wurde, zurück in ein Objekt umwandelt, bereitgestellt.
 * @author Korne127
 */
public class Serialiser {

	//Konfiguration
	private boolean startSerialisingInSuperclass;
	private Map<String, Object> methodParameters;
	private Set<String> ignoreExceptionIDs;
	private Set<String> ignoreSetterIDs;
	private CollectionHandling collectionHandling;
	private NewVariableHandling newVariableHandling;

	/**
	 * Enum, das die verschiedenen Optionen, wie sich der Serialiser verhalten soll, wenn
	 * keine Instanz einer Collection oder Map erstellt werden kann, enthält.<br>
	 * Siehe {@link #NO_WARNING}, {@link #CONVERT_WITH_WARNING}, {@link #DEBUG_MODE}, {@link #NO_CASTING}.
	 */
	public enum CollectionHandling {
		/**
		 * Falls keine Instanz einer Collection/Map erstellt werden kann, wird eine andere, möglichst
		 * ähnliche Collection/Map Klasse genommen, in der die Elemente gespeichert werden.
		 */
		NO_WARNING,
		/**
		 * Der Standard-Modus<br>
		 * Falls keine Instanz einer Collection/Map erstellt werden kann, wird eine andere, möglichst
		 * ähnliche Collection/Map Klasse genommen, in der die Elemente gespeichert werden.<br>
		 * Zusätzlich wird eine Warnung, welche Klasse in welche Klasse umgewandelt wurde, ausgegeben.
		 */
		CONVERT_WITH_WARNING,
		/**
		 * Falls keine Instanz einer Collection/Map erstellt werden kann, wird eine andere, möglichst
		 * ähnliche Collection/Map Klasse genommen, in der die Elemente gespeichert werden.<br>
		 * Zusätzlich wird die komplette Exception, die gefangen wurde, sowie eine Warnung, welche Klasse
		 * in welche Klasse umgewandelt wurde, ausgegeben.
		 */
		DEBUG_MODE,
		/**
		 * Falls keine Instanz einer Collection/Map erstellt werden kann, wird eine
		 * {@link DeserialiseException} geworfen und der Programmablauf unterbrochen.
		 */
		NO_CASTING
	}

	/**
	 * Enum, das die verschiedenen Optionen, wie sich der Serialiser verhalten soll, wenn
	 * beim Deserialisierungsprozess eine Klasse eine "neue Variable", also eine Variable,
	 * die (de)serialisiert werden soll, aber nicht im angegebenen Json gespeichert wurde,
	 * besitzt, enthält.<br>
	 * Siehe {@link #NO_WARNING}, {@link #WARNING}, {@link #EXCEPTION}.
	 */
	public enum NewVariableHandling {
		/**
		 * Falls eine Klasse eine neue Variable enthält, wird diese ignoriert.
		 */
		NO_WARNING,
		/**
		 * Falls eine Klasse eine neue Variable enthält, wird diese nicht gesetzt, aber es
		 * wird eine Warnung mit Informationen über die neue Variable und in welchem Objekt sie
		 * enthalten ist, ausgegeben.
		 */
		WARNING,
		/**
		 * Der Standard-Modus<br>
		 * Falls eine Klasse eine neue Variable enthält, wird eine {@link DeserialiseException} geworfen
		 * und der Programmablauf unterbrochen.
		 */
		EXCEPTION
	}

	/**
	 * Der Konstruktor der Serialiser-Klasse:<br>
	 * Erstellt einen neuen Serialiser mit den folgenden Konfigurationswerten:<br>
	 * - startSerialisingInSuperclass: true - Zunächst werden die Variablen der obersten Oberklasse serialisiert.<br>
	 * Dies kann über {@link #setStartSerialisingInSuperclass(boolean)} geändert werden.<br>
	 * - methodParameters: leer - Es gibt keine Parameter, zu denen Objekte angegeben werden.<br>
	 * Dies kann über {@link #setMethodParameters(Map)} geändert werden.<br>
	 * - ignoreExceptionIDs: leer - Es werden keine IgnoreException-Annotations ignoriert.<br>
	 * Dies kann über {@link #setIgnoreExceptionIDs(Set)} geändert werden.<br>
	 * - ignoreSetterIDs: leer - Es werden keine Setter-Annotations ignoriert.<br>
	 * Dies kann über {@link #setIgnoreSetterIDs(Set)} geändert werden.<br>
	 * - collectionHandling: {@link CollectionHandling#CONVERT_WITH_WARNING}.<br>
	 * Dies kann über {@link #setCollectionHandling(CollectionHandling)} geändert werden.<br>
	 * - newVariableHandling: {@link NewVariableHandling#EXCEPTION}.<br>
	 * Dies kann über {@link #setNewVariableHandling(NewVariableHandling)} geändert werden.
	 */
	public Serialiser() {
		startSerialisingInSuperclass = true;
		methodParameters = new HashMap<>();
		ignoreExceptionIDs = new HashSet<>();
		ignoreSetterIDs = new HashSet<>();
		collectionHandling = CollectionHandling.CONVERT_WITH_WARNING;
		newVariableHandling = NewVariableHandling.EXCEPTION;
	}

	/**
	 * Überschreibt die Einstellung startSerialisingInSuperclass, die bestimmt, ob der Serialiser zunächst die
	 * Variablen der obersten Oberklasse serialisieren soll und die der eigentlichen Klasse als letztes (true),
	 * oder umgekehrt.
	 * @param startSerialisingInSuperclass true - Der Serialiser soll zunächst die Variablen der obersten Oberklasse
	 *                                     serialisieren und als letztes die der eigentlichen Klasse;<br>
	 *                                     false - Der Serialiser soll zunächst die Variablen der eigentlichen Klasse
	 *                                     serialisieren und als letztes die der obersten Oberklasse
	 */
	public void setStartSerialisingInSuperclass(boolean startSerialisingInSuperclass) {
		this.startSerialisingInSuperclass = startSerialisingInSuperclass;
	}

	/**
	 * Nimmt die Map an, die ParameterIDs zu Objekten abbildet und überschreibt sie.<br>
	 * Diese wird genutzt, um Objekte als Parameter an Methoden, die die {@link AfterDeserialise} oder
	 * {@link BeforeSerialise} Annotation besitzen, anzugeben oder Variablen, die die {@link Setter} Annotation
	 * besitzen, zu überschreiben.
	 * @param methodParameters Die Map, die ParameterIDs zu Objekten abbildet
	 */
	public void setMethodParameters(Map<String, Object> methodParameters) {
		this.methodParameters = methodParameters;
	}

	/**
	 * Nimmt ein Set an, das die ignoreExceptionIDs beinhaltet.
	 * Ist eine {@link SerialiseIgnore} Annotation mit einer entsprechenden ID angegeben, wird diese Annotation
	 * ausgenommen und das entsprechende Feld trotzdem (de)serialisiert.<br>
	 * Dies erlaubt eine dynamische Serialisierung für verschiedene Fälle, in denen verschiedene Felder serialisiert
	 * werden sollen.
	 * @param ignoreExceptionIDs Das Set, das die ignoreExceptionIDs beinhaltet
	 */
	public void setIgnoreExceptionIDs(Set<String> ignoreExceptionIDs) {
		this.ignoreExceptionIDs = ignoreExceptionIDs;
	}

	/**
	 * Nimmt ein Set an, das die ignoreSetterIDs beinhaltet.
	 * Ist eine {@link Setter} Annotation mit einer entsprechenden ID angegebene, wird diese Annotation
	 * ausgenommen und das entsprechende Feld nach Deserialisierung nicht gesetzt.<br>
	 * Dies erlaubt eine dynamische Serialisierung für verschiedene Fälle, in denen verschiedene Felder serialisiert
	 * werden sollen.
	 * @param ignoreSetterIDs Das Set, das die ignoreSetterIDs beinhaltet
	 */
	public void setIgnoreSetterIDs(Set<String> ignoreSetterIDs) {
		this.ignoreSetterIDs = ignoreSetterIDs;
	}

	/**
	 * Überschreibt den Konfigurationswert von {@link CollectionHandling}, also wie sich der Serialiser verhalten
	 * soll, wenn keine Instanz einer Collection oder Map erstellt werden kann.
	 * @param collectionHandling Der neue Konfigurationswert von {@link NewVariableHandling}, wie sich der
	 *                           Serialiser verhalten soll, wenn keine Instanz einer Collection oder Map erstellt
	 *                           werden kann
	 */
	public void setCollectionHandling(CollectionHandling collectionHandling) {
		this.collectionHandling = collectionHandling;
	}

	/**
	 * Überschreibt den Konfigurationswert von {@link NewVariableHandling}, also wie sich der Serialiser verhalten
	 * soll, wenn beim Deserialisierungsprozess eine Klasse eine "neue Variable", also eine Variable die
	 * (de)serialisiert werden soll, aber nicht im angegebenen Json gespeichert wurde, besitzt.
	 * @param newVariableHandling Der neue Konfigurationswert von {@link NewVariableHandling}, wie sich der
	 *                            Serialiser verhalten soll, wenn beim Deserialisierungsprozess eine Klasse
	 *                            eine neues Variable enthält
	 */
	public void setNewVariableHandling(NewVariableHandling newVariableHandling) {
		this.newVariableHandling = newVariableHandling;
	}



	/**
	 * Serialisiert das angegebene Objekt in ein JSON-Objekt, welches als String zurückgegeben wird.<br>
	 * Es wird ein {@link SerialiseProcess} benutzt, um das angegebene Objekt zu serialisieren und die
	 * toString-Methode von {@link JSONArray} bzw. {@link JSONObject}, um ein entsprechendes
	 * Objekt in einen String umzuwandeln, der dann zurückgegeben wird.
	 * @param object Das angegebene Objekt, das in einen String serialisiert werden soll
	 * @return Ein JSON-String, in den das angegebene Objekt serialisiert wurde
	 * @throws SerialiseException Wird geworfen, falls ein Fehler beim Serialisieren des Objektes
	 * aufgetreten ist.
	 */
	public String serialiseObject(Object object) throws SerialiseException {
		SerialiseProcess process = new SerialiseProcess(false, startSerialisingInSuperclass, methodParameters,
				ignoreExceptionIDs);
		return JSONWriter.writeElement(process.serialise(object, null));
	}

	/**
	 * Serialisiert das angegebene Objekt in mehrere JSON-Objekte, welche als verschiedene Strings in einer
	 * Map zu Dateinamen zugeordnet zurückgegeben werden. Dabei bestimmt die SerialiseFile Annotation an einer
	 * Klasse oder Variable, in welcher Datei / zu welchem Dateinamen zugeordnet ein Objekt der Klasse bzw.
	 * die Variable zurückgegeben wird.<br>
	 * Es wird ein {@link SerialiseProcess} benutzt, um das angegebene Objekt zu serialisieren und das Ergebnis
	 * sowie die zusätzlich generierten JSON-Dateien zu bekommen. Dann wird eine Map von dem Dateinamen zu je einem
	 * JSONObject erstellt, in die das serialisierte Hauptobjekt dann auch gesetzt wird.<br>
	 * Jedes Element aus der Map wird über die toString-Methode von {@link JSONObject} in einen String umgewandelt
	 * und die neue daraus resultierende Map wird zurückgegeben.
	 * @param object Das angegebene Objekt, das in mehrere Strings serialisiert werden soll
	 * @return Eine Map von Dateinamen zu JSON-Strings, in die das angegebene Objekt serialisiert wurde
	 * @throws SerialiseException Wird geworfen, falls ein Fehler beim Serialisieren des Objektes
	 * aufgetreten ist.
	 */
	public Map<String, String> serialiseObjectSeparated(Object object) throws SerialiseException {
		SerialiseProcess process = new SerialiseProcess(true, startSerialisingInSuperclass, methodParameters,
				ignoreExceptionIDs);

		String mainFileName = SerialiseUtils.getFileName(object);
		if (mainFileName == null) {
			mainFileName = "Standard";
		}

		Object mainObject = process.serialise(object, mainFileName);
		Map<String, JSONObject> json = process.getWholeSeparatedJson();

		if (json.containsKey(mainFileName)) {
			json.get(mainFileName).putFirst("Main", mainObject);
		} else {
			JSONObject newObject = new JSONObject();
			newObject.put("Main", mainObject);
			json.put(mainFileName, newObject);
		}

		Map<String, String> resultMap = new HashMap<>();
		for (Map.Entry<String, JSONObject> entry : json.entrySet()) {
			resultMap.put(entry.getKey(), entry.getValue().toString());
		}
		return resultMap;
	}

	/**
	 * Deserialisiert den angegebenen JSON-String in ein Java-Objekt, welches zurückgegeben wird.<br>
	 * Es wird dabei aus dem angegebenen String ein JSON-Element erstellt und danach ein
	 * {@link DeserialiseProcess} benutzt, um das JSON-Element zu deserialisieren und
	 * das Resultat zurückzugeben.
	 * @param content Der angegebene JSON-String, der in ein Java-Objekt deserialisiert werden soll
	 * @return Ein Java-Objekt, in das der angegebene JSON-String deserialisiert wurde
	 * @throws JsonParseException Wird geworfen, falls der JSON-String nicht geparst werden
	 * konnte.
	 * @throws DeserialiseException Wird geworfen, falls ein Fehler beim Deserialisieren
	 * des Objektes aufgetreten ist.
	 */
	public Object deserialiseObject(String content) throws JsonParseException, DeserialiseException {
		DeserialiseProcess process = new DeserialiseProcess(false, startSerialisingInSuperclass,
				methodParameters, ignoreExceptionIDs, ignoreSetterIDs, collectionHandling, newVariableHandling);
		Object json;
		if (content.charAt(0) == '[') {
			json = new JSONArray(content);
		} else if (content.charAt(0) == '{') {
			json = new JSONObject(content);
		} else {
			json = content;
		}
		return process.deserialise(json, null);
	}

	/**
	 * Deserialisiert die in einer Map zu Dateinamen zugeordneten JSON-Strings in ein Java-Objekt.<br>
	 * Es wird dabei aus der angegebenen Map eine neue Map gebildet, in die für jeden JSON-String ein
	 * entsprechendes JSONObject erstellt und hinzugefügt wurde.
	 * Dann wird das Hauptobjekt aus den JSONObjects aus der neugebildeten Map gesucht und es wird ein
	 * {@link DeserialiseProcess} benutzt, um das JSONObject zu deserialisieren und das Resultat zurückzugeben.
	 * @param content Die angegebene Map von Dateinamen zu JSON-Strings, die in ein Java-Objekt deserialisiert
	 *                werden sollen
	 * @return Ein Java-Objekt, in das die angegebene Map mit JSON-Strings zu Dateinamen zugeordnet deserialisiert
	 * wurde
	 * @throws JsonParseException Wird geworfen, falls einer der JSON-Strings nicht geparst werden konnte.
	 * @throws DeserialiseException Wird geworfen, falls ein Fehler beim Deserialisieren des Objektes
	 * aufgetreten ist.
	 */
	public Object deserialiseSeparatedObject(Map<String, String> content)
			throws JsonParseException, DeserialiseException {
		DeserialiseProcess process = new DeserialiseProcess(true, startSerialisingInSuperclass,
				methodParameters, ignoreExceptionIDs, ignoreSetterIDs, collectionHandling, newVariableHandling);

		Map<String, JSONObject> json = new HashMap<>();
		for (Map.Entry<String, String> entry : content.entrySet()) {
			json.put(entry.getKey(), new JSONObject(entry.getValue()));
		}

		for (JSONObject value : json.values()) {
			if (value.containsKey("Main")) {
				return process.deserialise(value.get("Main"), json);
			}
		}
		throw new DeserialiseException("The main object to deserialise could not be found.");
	}
}
