package de.korne127.circularJsonSerialiser.serialiser;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import de.korne127.circularJsonSerialiser.annotations.AfterDeserialise;
import de.korne127.circularJsonSerialiser.annotations.BeforeSerialise;
import de.korne127.circularJsonSerialiser.annotations.IgnoreCasting;
import de.korne127.circularJsonSerialiser.annotations.SerialiseIgnore;
import de.korne127.circularJsonSerialiser.annotations.Setter;
import de.korne127.circularJsonSerialiser.exceptions.DeserialiseException;
import de.korne127.circularJsonSerialiser.exceptions.JsonParseException;
import de.korne127.circularJsonSerialiser.exceptions.SerialiseException;
import de.korne127.circularJsonSerialiser.json.JSONArray;
import de.korne127.circularJsonSerialiser.json.JSONElement;
import de.korne127.circularJsonSerialiser.json.JSONObject;

/**
 * Serialiser-Klasse:<br>
 * Diese Klasse stellt einen Serialiser bereit, der ein angegebenes Objekt in ein JSON-Objekt umwandelt und
 * als String zurückgibt, sowie einen Deserialiser, der ein angegebenes JSON-Objekt, welches durch den
 * Serialiser berechnet wurde, zurück in ein Objekt umwandelt.
 * @author Korne127
 */
public class Serialiser {

	private Map<Integer, Object> hashTable;

	//Für BeforeSerialise-Annotations
	private Set<Integer> beforeSerialised;

	//Für IgnoreCasting Annotations
	private Stack<Boolean> ignoreCasting;

	//Für Fehlermeldungen
	private PathInformation fieldInformation;

	//Für Serialisierung allgemein
	private Map<String, JSONObject> wholeSeparatedJson;
	private JSONElement wholeSingleJson;
	private boolean multiFile;

	//Konfiguration
	private final CollectionHandling collectionHandling;
	private NewVariableHandling newVariableHandling;
	private final boolean startSerialisingInSuperclass;
	private Map<String, Object> methodParameters;
	private Set<String> ignoreExceptionIDs;
	private Set<String> ignoreSetterIDs;

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
		 * wird eine Warnung ausgegeben.
		 */
		WARNING,
		/**
		 * Der Standard-Modus:<br>
		 * Falls eine Klasse eine neue Variable enthält, wird eine Exception geworfen.
		 */
		EXCEPTION
	}

	/**
	 * Der standardmäßige Konstruktor der Serialiser-Klasse:<br>
	 * Erstellt einen neuen Serialiser.<br>
	 * Setzt dabei den Modus, wie Collections behandelt werden auf
	 * {@link CollectionHandling#CONVERT_WITH_WARNING CONVERT_WITH_WARNING} und stellt ein, dass zuerst die
	 * Variablen der Oberklasse serialisiert werden.
	 */
	public Serialiser() {
		this(CollectionHandling.CONVERT_WITH_WARNING, true);
	}

	/**
	 * Ein Konstruktor der Serialiser-Klasse:<br>
	 * Erstellt einen neuen Serialiser und setzt den Modus, wie Collections behandelt werden auf den
	 * angegebenen Modus sowie die Einstellung, ob zuerst Variablen der Oberklassen oder der eigentlichen Klasse
	 * serialisiert werden sollen auf die angegebene.
	 * @param collectionHandling Der angegebene Modus, wie sich der Serialiser verhalten soll, wenn keine
	 *                           Instanz einer Collection oder Map erstellt werden kann
	 * @param startSerialisingInSuperclass Die angegebene Einstellung, ob der Serialiser zunächst die Variablen
	 *                                     der Oberklassen serialisieren soll und dann die der eigentlichen Klasse
	 *                                     oder umgekehrt
	 */
	public Serialiser(CollectionHandling collectionHandling, boolean startSerialisingInSuperclass) {
		this.collectionHandling = collectionHandling;
		this.startSerialisingInSuperclass = startSerialisingInSuperclass;
		newVariableHandling = NewVariableHandling.EXCEPTION;
		methodParameters = new HashMap<>();
		ignoreExceptionIDs = new HashSet<>();
		ignoreSetterIDs = new HashSet<>();
	}

	/**
	 * Überschreibt den Konfigurationswert von {@link NewVariableHandling}, also wie sich der Serialiser verhalten
	 * soll, wenn beim Deserialisierungsprozess eine Klasse eine "neue Variable", also eine Variable die
	 * (de)serialisiert werden soll, aber nicht im angegebenen Json gespeichert wurde, besitzt.
	 * @param newVariableHandling Der neue Konfigurationswert von {@link NewVariableHandling}, auf den überschrieben
	 *                            werden soll
	 */
	public void setNewVariableHandling(NewVariableHandling newVariableHandling) {
		this.newVariableHandling = newVariableHandling;
	}

	/**
	 * Nimmt die Map an, die ParameterIDs zu Objekten abbildet und überschreibt sie.<br>
	 * Diese wird genutzt, um Objekte als Parameter an Methoden, die die {@link AfterDeserialise} oder
	 * {@link BeforeSerialise} Annotation besitzen, anzugeben oder Variablen, die die {@link Setter} Annotation
	 * besitzen, zu überschreiben.
	 * @param methodParameters Die Map, die ParameterIDs zu Objekten abbildet
	 */
	public void setParameters(Map<String, Object> methodParameters) {
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
	public void ignoreExceptionIDs(Set<String> ignoreExceptionIDs) {
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
	public void ignoreSetterIDs(Set<String> ignoreSetterIDs) {
		this.ignoreSetterIDs = ignoreSetterIDs;
	}

	//----SERIALISE METHODEN----

	/**
	 * Serialisiert das angegebene Objekt in ein JSON-Objekt, welches als String zurückgegeben wird.<br>
	 * Es werden zunächst beim (De-)Serialisieren benutzte Hilfsvariablen geleert bzw. gesetzt.<br>
	 * Dann wird {@link #objectToJson(Object, String) objectToJson} benutzt, um das angegebene Objekt zu
	 * serialisieren und die toString-Methode von {@link JSONArray} bzw. {@link JSONObject}, um ein entsprechendes
	 * Objekt in einen String umzuwandeln, der dann zurückgegeben wird.
	 * @param object Das angegebene Objekt, das in einen String serialisiert werden soll
	 * @return Ein JSON-String, in den das angegebene Objekt serialisiert wurde
	 * @throws SerialiseException Wird geworfen, falls ein Fehler beim Serialisieren des Objektes
	 * aufgetreten ist.
	 */
	public String serialiseObject(Object object) throws SerialiseException {
		hashTable = new HashMap<>();
		fieldInformation = new PathInformation();
		multiFile = false;
		beforeSerialised = new HashSet<>();

		executeBeforeSerialiseMethods(object, new HashMap<>());
		return objectToJson(object, null).toString();
	}

	/**
	 * Serialisiert das angegebene Objekt in mehrere JSON-Objekte, welche als verschiedene Strings in einer
	 * Map zu Dateinamen zugeordnet zurückgegeben werden. Dabei bestimmt die SerialiseFile Annotation an einer
	 * Klasse oder Variable, in welcher Datei / zu welchem Dateinamen zugeordnet ein Objekt der Klasse bzw.
	 * die Variable zurückgegeben wird.<br>
	 * Es werden zunächst beim (De-)Serialisieren benutzte Hilfsvariablen geleert bzw. gesetzt.<br>
	 * Dann wird {@link #objectToJson(Object, String) objectToJson} benutzt, um das angegebene Objekt zu
	 * serialisieren. Durch die gesetzten Einstellungen wird auch eine Map von dem Dateinamen zu je einem
	 * JSONObject erstellt, in die das serialisierte Hauptobjekt dann auch gesetzt wird.<br>
	 * Jedes Element aus der Map wird über die toString-Methode von {@link JSONObject} in einen String umgewandelt
	 * und die neue daraus resultierende Map wird zurückgegeben.
	 * @param object Das angegebene Objekt, das in mehrere Strings serialisiert werden soll
	 * @return Eine Map von Dateinamen zu JSON-Strings, in die das angegebene Objekt serialisiert wurde
	 * @throws SerialiseException Wird geworfen, falls ein Fehler beim Serialisieren des Objektes
	 * aufgetreten ist.
	 */
	public Map<String, String> serialiseObjectSeparated(Object object) throws SerialiseException {
		hashTable = new HashMap<>();
		fieldInformation = new PathInformation();
		wholeSeparatedJson = new HashMap<>();
		multiFile = true;
		beforeSerialised = new HashSet<>();

		String mainFileName = SerialiseUtils.getFileName(object);
		if (mainFileName == null) {
			mainFileName = "Standard";
		}

		executeBeforeSerialiseMethods(object, new HashMap<>());
		Object mainObject = objectToJson(object, mainFileName);

		if (wholeSeparatedJson.containsKey(mainFileName)) {
			wholeSeparatedJson.get(mainFileName).putFirst("Main", mainObject);
		} else {
			JSONObject newObject = new JSONObject();
			newObject.put("Main", mainObject);
			wholeSeparatedJson.put(mainFileName, newObject);
		}

		Map<String, String> resultMap = new HashMap<>();
		for (String key : wholeSeparatedJson.keySet()) {
			resultMap.put(key, wholeSeparatedJson.get(key).toString());
		}
		return resultMap;
	}

	/**
	 * Serialisiert ein angegebenes Java-Objekt in ein für Json benutzbares Objekt.<br>
	 * Falls das angegebene Objekt null ist, wird null zurückgegeben.<br>
	 * Falls das angegebene Objekt {@link SerialiseUtils#isSimpleType(Class) simpel} ist, wird
	 * {@link #simpleTypeToJson(Object, Class) simpleTypeToJson} zurückgegeben.<br>
	 * Falls das angegebene Objekt ein Enum ist, wird es als String kodiert zurückgegeben.<br>
	 * Falls das angegebene Objekt bereits serialisiert ist, wird ein als String kodierter Verweis zurückgegeben.<br>
	 * Falls das angegebene Objekt eine Methode mit {@link BeforeSerialise} Annotation hat, wird sie ausgeführt.<br>
	 * Falls das angegebene Objekt ein Array oder eine Collection ist, wird
	 * {@link #collectionToJson(Object, Class, int, String) collectionToJson} zurückgegeben.<br>
	 * Falls das angegebene Objekt eine Map ist, wird
	 * {@link #mapToJson(Map, Class, int, String) mapToJson} zurückgegeben.<br>
	 * Falls das angegebene Objekt von einer Klasse aus {@link SpecialClasses} ist, wird
	 * {@link #specialClassToJson(Object, Class) specialClassToJson} zurückgegeben.<br>
	 * Ansonsten wird {@link #javaObjectToJson(Object, Class, int, String) javaObjectToJson} zurückgegeben.
	 * @param object Das angegebene Objekt, das serialisiert werden soll.
	 * @param parentFileName Der Name der Datei, in die das Elternobjekt gespeichert werden soll
	 * @return Ein für Json benutzbares Objekt, in das das angegebene Objekt serialisiert wurde
	 * @throws SerialiseException Wird geworfen, falls ein Fehler beim Serialisieren des Objektes auftritt.
	 */
	private Object objectToJson(Object object, String parentFileName) throws SerialiseException {
		if (object == null) {
			return null;
		}
		Class<?> objectClass = object.getClass();
		if (SerialiseUtils.isSimpleType(objectClass)) {
			return simpleTypeToJson(object, objectClass);
		}
		if (objectClass.isEnum()) {
			return "#" + objectClass.getName() + "=" + object.toString();
		}

		if (objectClass.isSynthetic() || objectClass.isAnonymousClass()) {
			fieldInformation.addClass(objectClass);
			if (fieldInformation.hasNoPath()) {
				throw new SerialiseException("The given object is an anonymous class or lambda and therefore " +
						"cannot be serialised.");
			} else {
				throw new SerialiseException("The " + fieldInformation.toString() +
						" is an anonymous class or lambda and therefore cannot be serialised.\n" +
						"To prevent this, use a subclass instead of an anonymous class/lambda or mark the " +
						"object with the @SerialiseIgnore Annotation");
			}
		}

		int objectHash = System.identityHashCode(object);
		if (hashTable.containsKey(objectHash)) {
			return "@" + objectHash;
		}
		hashTable.put(objectHash, object);

		fieldInformation.addClass(objectClass);
		Object result;
		if (objectClass.isArray() || object instanceof Collection) {
			result = collectionToJson(object, objectClass, objectHash, parentFileName);
		} else if (object instanceof Map) {
			result = mapToJson((Map<?, ?>) object, objectClass, objectHash, parentFileName);
		} else if (SpecialClasses.getClassMap().containsKey(objectClass.getName())) {
			result = specialClassToJson(object, objectClass);
		} else {
			result = javaObjectToJson(object, objectClass, objectHash, parentFileName);
		}
		fieldInformation.remove();
		return result;
	}

	/**
	 * Serialisiert ein simples Objekt (Wrapper-Objekt eines simplen Datentyps oder String) in ein für Json
	 * benutzbares Objekt.<br>
	 * Dabei werden Wrapper-Objekte direkt zurückgegeben, Strings, die mit einem @ oder # anfangen werden vorher
	 * escaped, da ein String, der mit einem dieser Zeichen anfängt, als bestimmte Kodierung fungiert.
	 * @param object Das Objekt, das serialisiert werden soll
	 * @param objectClass Die Klasse des Objektes, das serialisiert werden soll
	 * @return Ein für Json benutzbares Objekt, in das das angegebene Objekt serialisiert wurde
	 */
	private Object simpleTypeToJson(Object object, Class<?> objectClass) {
		if (objectClass == String.class) {
			String string = (String) object;
			if (string.matches("(\\\\)*[@#].*")) {
				string = "\\" + string;
				return string;
			}
		}
		return object;
	}

	/**
	 * Serialisiert ein Array bzw. eine Collection in ein JSONArray.<br>
	 * Dabei werden alle Inhalte des Arrays bzw. der Collection abgefragt und ebenfalls in JSON-Elemente
	 * {@link #objectToJson(Object, String) serialisiert} und dem JSONArray dann hinzugefügt.
	 * @param object Das Objekt, das serialisiert werden soll
	 * @param objectClass Die Klasse des Objektes, das serialisiert werden soll
	 * @param objectHash Der Hash des Objektes, das serialisiert werden soll
	 * @param parentFileName Der Name der Datei, in die das Elternobjekt gespeichert werden soll
	 * @return Ein JSONArray, in das das angegebene Objekt serialisiert wurde
	 * @throws SerialiseException Wird geworfen, falls ein Fehler beim Serialisieren des Objektes auftritt.
	 */
	private JSONArray collectionToJson(Object object, Class<?> objectClass, int objectHash, String parentFileName)
			throws SerialiseException {
		Iterable<?> objectIterable;
		if (objectClass.isArray()) {
			Object[] objectArray;
			if (objectClass.getComponentType().isPrimitive()) {
				objectArray = SerialiseUtils.convertPrimitiveToArray(object);
			} else {
				objectArray = (Object[]) object;
			}
			objectIterable = Arrays.asList(objectArray);
		} else {
			objectIterable = (Collection<?>) object;
		}
		JSONArray jsonChild = new JSONArray();
		jsonChild.put(objectClass.getName() + "=" + objectHash);
		for (Object objectChild : objectIterable) {
			String childFileName = SerialiseUtils.getFileName(objectChild);
			if (multiFile && parentFileName.equals(childFileName)) {
				childFileName = null;
			}
			Object childJson = objectToJson(objectChild, childFileName == null ? parentFileName : childFileName);
			if (!multiFile || childJson == null || childFileName == null ||
					SerialiseUtils.isSimpleType(childJson.getClass())) {
				jsonChild.put(childJson);
			} else {
				int childHash = System.identityHashCode(objectChild);
				jsonChild.put("@" + childFileName + "#" + childHash);
				putInWholeSeparatedJson(childFileName, childHash, childJson);
			}
		}
		return jsonChild;
	}

	/**
	 * Serialisiert eine Map in ein JSONArray.<br>
	 * Dabei werden alle keys und values der Map abgefragt und ebenfalls in JSON-Elemente
	 * {@link #objectToJson(Object, String) serialisiert} und dem JSONArray dann je als ein JSONObject hinzugefügt.
	 * @param objectMap Die Map, die serialisiert werden soll
	 * @param objectClass Die Klasse der Map, die serialisiert werden soll
	 * @param objectHash Der Hash der Map, die serialisiert werden soll
	 * @param parentFileName Der Name der Datei, in die das Elternobjekt gespeichert werden soll
	 * @return Ein JSONArray, in das die angegebene Map serialisiert wurde
	 * @throws SerialiseException Wird geworfen, falls ein Fehler beim Serialisieren des Objektes auftritt.
	 */
	private JSONArray mapToJson(Map<?, ?> objectMap, Class<?> objectClass, int objectHash, String parentFileName)
			throws SerialiseException {
		JSONArray map = new JSONArray();
		map.put(objectClass.getName() + "=" + objectHash);
		for (Object objectMapKey : objectMap.keySet()) {
			Object objectMapValue = objectMap.get(objectMapKey);
			JSONObject mapChild = new JSONObject();

			String keyFileName = SerialiseUtils.getFileName(objectMapKey);
			if (multiFile && parentFileName.equals(keyFileName)) {
				keyFileName = null;
			}
			Object mapKeyJson = objectToJson(objectMapKey, keyFileName == null ? parentFileName : keyFileName);
			if (!multiFile || mapKeyJson == null || keyFileName == null ||
					SerialiseUtils.isSimpleType(mapKeyJson.getClass())) {
				mapChild.put("key", mapKeyJson);
			} else {
				int childHash = System.identityHashCode(objectMapKey);
				mapChild.put("key", "@" + keyFileName + "#" + childHash);
				putInWholeSeparatedJson(keyFileName, childHash, mapKeyJson);
			}

			String valueFileName = SerialiseUtils.getFileName(objectMapValue);
			if (multiFile && parentFileName.equals(valueFileName)) {
				valueFileName = null;
			}
			Object mapValueJson = objectToJson(objectMapValue, valueFileName == null ? parentFileName : valueFileName);
			if (!multiFile || mapValueJson == null || valueFileName == null ||
					SerialiseUtils.isSimpleType(mapValueJson.getClass())) {
				mapChild.put("value", mapValueJson);
			} else {
				int childHash = System.identityHashCode(objectMapValue);
				mapChild.put("value", "@" + valueFileName + "#" + childHash);
				putInWholeSeparatedJson(valueFileName, childHash, mapValueJson);
			}
			map.put(mapChild);
		}
		return map;
	}

	/**
	 * Serialisiert ein Objekt einer Klasse aus {@link SpecialClasses} in einen für Json benutzbaren String.<br>
	 * Dabei wird die Serialisierungs-Methode der entsprechenden Klasse des Objektes mit dem Objekt aufgerufen und
	 * der daraus entstandene String kodiert zurückgegeben.
	 * @param object Das Objekt einer Klasse aus {@link SpecialClasses}, das serialisiert werden soll
	 * @param objectClass Die Klasse des Objektes, das serialisiert werden soll
	 * @return Ein String, in den das Objekt serialisiert wurde
	 * @throws SerialiseException Wird geworfen, falls ein Fehler beim Serialisieren des Objektes auftritt.
	 */
	private String specialClassToJson(Object object, Class<?> objectClass) throws SerialiseException {
		SpecialClasses specialClass = SpecialClasses.getClassMap().get(objectClass.getName());
		String result = specialClass.serialise(object);
		if (result == null) {
			throw new SerialiseException("The special class " + fieldInformation.toString() +
					" could not be deserialised.");
		}
		return "#" + specialClass.getSpecialClassName() + "=" + result;
	}

	/**
	 * Serialisiert ein Java-Objekt in ein JSONObject.<br>
	 * Dabei werden alle Attribute des Objektes abgefragt und ebenfalls in JSON-Elemente
	 * {@link #objectToJson(Object, String) serialisiert} und dem JSONObject dann hinzugefügt.
	 * @param object Das Objekt, das serialisiert werden soll
	 * @param objectClass Die Klasse des Objektes, das serialisiert werden soll
	 * @param objectHash Der Hash des Objektes, das serialisiert werden soll
	 * @param parentFileName Der Name der Datei, in die das Elternobjekt gespeichert werden soll
	 * @return Ein JSONObject, in die das angegebene Objekt serialisiert wurde
	 * @throws SerialiseException Wird geworfen, falls ein Fehler beim Serialisieren des Objektes auftritt.
	 */
	private JSONObject javaObjectToJson(Object object, Class<?> objectClass, int objectHash, String parentFileName)
			throws SerialiseException {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("class", objectClass.getName() + "=" + objectHash);
		for (Field objectField : SerialiseUtils.getSerialiseFields(objectClass, startSerialisingInSuperclass,
				false, ignoreExceptionIDs)) {
			fieldInformation.addField(objectField);
			Object child;
			try {
				child = objectField.get(object);
			} catch (IllegalAccessException e) {
				throw new SerialiseException("Fatal error: The field value of " + fieldInformation.toString() +
						" could not be retrieved", e);
			}
			String currentFileName = SerialiseUtils.getFileName(objectField, child);
			if (multiFile && parentFileName.equals(currentFileName)) {
				currentFileName = null;
			}
			Object childJson = objectToJson(child, currentFileName == null ? parentFileName : currentFileName);
			if (!multiFile || childJson == null || currentFileName == null ||
					SerialiseUtils.isSimpleType(childJson.getClass())) {
				jsonObject.put(objectField.getName(), childJson);
			} else {
				int childHash = System.identityHashCode(child);
				jsonObject.put(objectField.getName(), "@" + currentFileName + "#" + childHash);
				putInWholeSeparatedJson(currentFileName, childHash, childJson);
			}
			fieldInformation.removeField(objectField);
		}
		return jsonObject;
	}


	//----DESERIALISE METHODEN----

	/**
	 * Deserialisiert den angegebenen JSON-String in ein Java-Objekt, welches zurückgegeben wird.<br>
	 * Es werden zunächst beim (De-)Serialisieren benutzte Hilfsvariablen geleert bzw. gesetzt.<br>
	 * Es wird dabei aus dem angegebenen String ein JSONObject erstellt und danach
	 * {@link #jsonToObject(Object) jsonToObject} benutzt, um das JSONObject zu deserialisieren und
	 * das Resultat zurückzugeben.
	 * @param content Der angegeben JSON-String, der in ein Java-Objekt deserialisiert werden soll
	 * @return Ein Java-Objekt, in das der angegebene JSON-String deserialisiert wurde
	 * @throws JsonParseException Wird geworfen, falls der JSON-String nicht geparst werden
	 * konnte.
	 * @throws DeserialiseException Wird geworfen, falls ein Fehler beim Deserialisieren
	 * des Objektes aufgetreten ist.
	 */
	public Object deserialiseObject(String content) throws JsonParseException, DeserialiseException {
		hashTable = new LinkedHashMap<>();
		fieldInformation = new PathInformation();
		ignoreCasting = new Stack<>();
		ignoreCasting.push(false);
		if (content.charAt(0) == '[') {
			wholeSingleJson = new JSONArray(content);
		} else {
			wholeSingleJson = new JSONObject(content);
		}
		multiFile = false;

		Object result = jsonToObject(wholeSingleJson);
		executeAfterDeserialiseMethods();
		return result;
	}

	/**
	 * Deserialisiert die in einer Map zu Dateinamen zugeordneten JSON-Strings in ein Java-Objekt.<br>
	 * Es werden zunächst beim (De-)Serialisieren benutzte Hilfsvariablen geleert bzw. gesetzt.<br>
	 * Es wird dabei aus der angegebenen Map eine neue Map gebildet, in die für jeden JSON-String ein
	 * entsprechendes JSONObject erstellt und hinzugefügt wurde.
	 * Dann wird das Hauptobjekt aus den JSONObjects aus der neugebildeten Map gesucht und es wird
	 * {@link #jsonToObject(Object) jsonToObject} benutzt, um das JSONObject zu deserialisieren und das Resultat
	 * zurückzugeben.
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
		hashTable = new LinkedHashMap<>();
		fieldInformation = new PathInformation();
		wholeSeparatedJson = new HashMap<>();
		ignoreCasting = new Stack<>();
		ignoreCasting.push(false);
		for (String key : content.keySet()) {
			wholeSeparatedJson.put(key, new JSONObject(content.get(key)));
		}
		multiFile = true;

		for (String key : wholeSeparatedJson.keySet()) {
			if (wholeSeparatedJson.get(key).containsKey("Main")) {
				Object result = jsonToObject(wholeSeparatedJson.get(key).get("Main"));
				executeAfterDeserialiseMethods();
				return result;
			}
		}
		throw new DeserialiseException("The main object to deserialise could not be found.");
	}

	/**
	 * Deserialisiert ein Objekt aus der JSON-Datei in ein Java-Objekt.<br>
	 * Falls das angegebene Objekt der JSON-Datei null ist, wird null zurückgegeben.<br>
	 * Falls das angegebene Objekt der JSON-Datei {@link SerialiseUtils#isSimpleType(Class) simpel} ist, wird es über
	 * {@link #simpleTypeToObject(Object) simpleTypeToObject} deserialisiert und zurückgegeben.<br>
	 * Ansonsten werden die Informationen abgefragt, wie die Klasse und der beim Serialisieren gespeicherte Hash
	 * des Java-Objektes ist.<br>
	 * Falls das Objekt mit diesem Hash bereits deserialisiert wurde, wird es zurückgegeben.<br>
	 * Falls das angegebene Objekt ein JSONArray ist, wird es über
	 * {@link #jsonArrayToObject(JSONArray, Class, String, int) jsonArrayToObject} deserialisiert und zurückgegeben,
	 * ansonsten über {@link #jsonObjectToObject(JSONObject, Class, int) jsonObjectToObject}.<br>
	 * Zusätzlich werden Variablen des Objektes, die mit der {@link Setter} Annotation annotiert sind (sofern die
	 * ParameterID nicht in ignoreSetterIDs ist), auf das mit der ParameterID verknüpfte Objekt gesetzt.
	 * @param json Das angegebene Objekt aus der JSON-Datei, das deserialisiert werden soll
	 * @return Ein Java-Objekt, in das das angegebene Objekt deserialisiert wurde
	 * @throws DeserialiseException Wird geworfen, falls ein Fehler beim Deserialisieren des Objektes
	 * auftritt.
	 */
	private Object jsonToObject(Object json) throws DeserialiseException {
		if (json == null) {
			return null;
		}
		if (SerialiseUtils.isSimpleType(json.getClass())) {
			return simpleTypeToObject(json);
		}

		String[] classInfos;
		if (json instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray) json;
			classInfos = jsonArray.getString(0).split("=");
		} else {
			JSONObject jsonObject = (JSONObject) json;
			classInfos = jsonObject.getString("class").split("=");
		}
		String className = classInfos[0];
		int hash = Integer.parseInt(classInfos[1]);
		if (hashTable.containsKey(hash)) {
			return hashTable.get(hash);
		}
		fieldInformation.addClass(className);
		Class<?> newClass;
		try {
			newClass = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new DeserialiseException("The specified class " + className + ", used by " +
					fieldInformation.toString() + " could not be found.", e);
		}

		Object result;
		if (json instanceof JSONArray) {
			result = jsonArrayToObject((JSONArray) json, newClass, className, hash);
		} else {
			result = jsonObjectToObject((JSONObject) json, newClass, hash);
		}

		for (Field field : SerialiseUtils.getSerialiseFields(newClass, true, true,
				ignoreExceptionIDs)) {
			String parameterID = field.getDeclaredAnnotation(Setter.class).value();
			if (ignoreSetterIDs.contains(parameterID)) {
				continue;
			}
			String fieldClassName = field.getDeclaringClass().getName();
			if (!methodParameters.containsKey(parameterID)) {
				throw new DeserialiseException("Error: The field " + fieldClassName + "#" +
						field.getName() + " is annotated with the Setter annotation and " +
						"requires a parameter but there has not been set a value for the " +
						"parameterID: " + parameterID);
			}
			try {
				field.set(result, methodParameters.get(parameterID));
			} catch (IllegalAccessException | IllegalArgumentException e) {
				throw new DeserialiseException("Error: The field " + fieldClassName + "#" +
						field.getName() + " is annotated with the Setter annotation but " +
						"could not be set.", e);
			}
		}
		fieldInformation.remove();
		return result;
	}


	/**
	 * Deserialisiert ein simples Objekt (Wrapper-Objekt eines simplen Datentyps oder String) aus der JSON-Datei
	 * in ein Java-Objekt.<br>
	 * Dabei werden Wrapper-Objekte direkt zurückgegeben.<br>
	 * Falls der String mit einem @ anfängt ist er ein kodiertes verlinktes Objekt und wird über
	 * {{@link #jsonToLinkedObject(String)} jsonToLinkedObject} deserialisiert und zurückgegeben.<br>
	 * Falls der String mit einem # anfängt ist er ein kodiertes Objekt aus einer {@link SpecialClasses SpecialClass}
	 * oder ein Enum und wird über {@link #jsonToSpecialClassOrEnum(String) jsonToSpecialClassOrEnum} deserialisiert
	 * und zurückgegeben.<br>
	 * Ansonsten wird, falls der String mit einem escapeten @ oder # anfängt, das Escapen rückgängig gemacht und der
	 * String zurückgegeben.
	 * @param json Das simple Objekt aus der JSON-Datei, das deserialisiert werden soll
	 * @return Ein Java-Objekt, in das das angegebene Objekt deserialisiert wurde
	 * @throws DeserialiseException Wird geworfen, falls ein Fehler beim Deserialisieren des Objektes
	 * auftritt.
	 */
	private Object simpleTypeToObject(Object json) throws DeserialiseException {
		if (String.class == json.getClass()) {
			String string = (String) json;
			if (string.isEmpty()) {
				return string;
			}
			if (string.charAt(0) == '@') {
				return jsonToLinkedObject(string);
			}
			if (string.charAt(0) == '#') {
				return jsonToSpecialClassOrEnum(string);
			}
			if (string.matches("(\\\\)+[@#].*")) {
				return string.substring(1);
			}
			return string;
		}
		return json;
	}

	/**
	 * Deserialisiert ein als String kodiertes verlinktes Objekt aus der JSON-Datei in das entsprechende
	 * Java-Objekt.<br>
	 * Falls das verlinkte Objekt nicht in der aktuellen Datei serialisiert wurde, wird das Objekt über
	 * {@link #getLinkedFileObject(String, int) getLinkedFileObject} deserialisiert und zurückgegeben.<br>
	 * Ansonsten wird das Objekt über {@link #getLinkedObject(Object, int) getLinkedObject} deserialisiert und
	 * zurückgegeben.
	 * @param string Das als String kodierte verlinkte Objekt aus der JSON-Datei
	 * @return Das Java-Objekt, in das das angegebene Objekt deserialisiert wurde
	 * @throws DeserialiseException Wird geworfen, falls ein Fehler beim Deserialisieren des Objektes
	 * auftritt.
	 */
	private Object jsonToLinkedObject(String string) throws DeserialiseException {
		if (multiFile && string.contains("#")) {
			String[] linkInfo = string.substring(1).split("#");
			String fileName = linkInfo[0];
			int hash = Integer.parseInt(linkInfo[1]);
			if (hashTable.containsKey(hash)) {
				return hashTable.get(hash);
			}
			Object linkedObject = getLinkedFileObject(fileName, hash);
			if (linkedObject == null) {
				boolean isInside = fieldInformation.isFieldAdded();
				throw new DeserialiseException("The definition of the linked object " + string +
						(isInside ? " in " : " (") + fieldInformation.toString() +
						(isInside ? "" : ")") + " could not be found.");
			}
			return linkedObject;
		}
		int hash = Integer.parseInt(string.substring(1));
		if (hashTable.containsKey(hash)) {
			return hashTable.get(hash);
		}
		Object linkedObject = getLinkedObject(hash);
		if (linkedObject == null) {
			boolean isInside = fieldInformation.isFieldAdded();
			throw new DeserialiseException("The definition of the linked object " + string +
					(isInside ? " in " : " (") + fieldInformation.toString() +
					(isInside ? "" : ")") + " could not be found.");
		}
		return linkedObject;
	}

	/**
	 * Deserialisiert ein als String kodiertes Objekt aus einer {@link SpecialClasses SpecialClass} oder Enum
	 * aus der JSON-Datei in das entsprechende Java-Objekt.<br>
	 * Falls der String ein kodiertes Objekt aus einer {@link SpecialClasses SpecialClass} ist, wird die
	 * Deserialisierungs-Methode der entsprechenden Klasse des Objektes mit dem String aufgerufen und das daraus
	 * entstandene Objekt zurückgegeben.<br>
	 * Falls der String ein kodiertes Enum ist, wird eine Instanz des entsprechenden Enum-Wertes erstellt und
	 * zurückgegeben.
	 * @param string Das als String kodierte Objekt aus einer {@link SpecialClasses SpecialClass} bzw. Enum aus
	 *               der JSON-Datei
	 * @return Das Java-Objekt, in das das angegebene Objekt deserialisiert wurde
	 * @throws DeserialiseException Wird geworfen, falls ein Fehler beim Deserialisieren des Objektes
	 * auftritt.
	 */
	private Object jsonToSpecialClassOrEnum(String string) throws DeserialiseException {
		String[] classInfos = string.split("=");
		String className = classInfos[0].substring(1);
		fieldInformation.addClass(className);
		String classValue = classInfos[1];
		if (SpecialClasses.getClassMap().containsKey(className)) {
			SpecialClasses specialClass = SpecialClasses.getClassMap().get(className);
			Object result = specialClass.deserialise(classValue);
			if (result == null) {
				throw new DeserialiseException(fieldInformation.toString() + ", value: " + classValue +
						" could not be deserialised.");
			}
			fieldInformation.remove();
			return result;
		}
		Class<?> objectClass;
		try {
			objectClass = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new DeserialiseException("The specified class " + className + ", used by " +
					fieldInformation.toString() + " could not be found.", e);
		}
		if (objectClass.isEnum()) {
			fieldInformation.remove();
			return SerialiseUtils.getNewEnumInstance(classValue, objectClass);
		}
		throw new DeserialiseException("The definition of the special class " + className +
				", used by " + fieldInformation.toString() + " could not be found.");
	}

	/**
	 * Deserialisiert ein JSONArray in das entsprechende Java-Objekt: Ein Array, eine Collection oder eine Map.<br>
	 * Falls das Objekt ein Array ist, werden die einzelnen Inhalte des JSONArrays
	 * {@link #jsonToObject(Object) deserialisiert} und einem neuen Array hinzugefügt, das zurückgegeben wird.<br>
	 * Ansonsten wird eine Instanz der Collection bzw. Map des Objektes erstellt; falls dies nicht möglich ist
	 * wird eventuell ein {@link SerialiseUtils#getNewAlternativeInstance(Class, Exception, PathInformation)
	 * möglichst ähnlicher} Collection- bzw. Map-Typ benutzt und abhängig von der {@link CollectionHandling
	 * Konfiguration} sowie eventuell einer {@link IgnoreCasting} Annotation eine Warnung ausgegeben.<br>
	 * Wenn das Objekt eine Collection ist, werden alle Inhalte des JSONArrays
	 * {@link #jsonToObject(Object) deserialisiert} und der Collection-Instanz hinzugefügt, die dann zurückgegeben
	 * wird. Wenn das Objekt eine Map ist, werden alle keys und values, die je als JSONObject in dem JSONArray
	 * gespeichert sind {@link #jsonToObject(Object) deserialisiert} und der Map-Instanz hinzugefügt, die dann
	 * zurückgegeben wird.
	 * @param jsonArray Das JSONArray, das deserialisiert werden soll
	 * @param newClass Die Klasse des Objektes, in das deserialisiert werden soll
	 * @param className Der Name der Klasse des Objektes, in das deserialisiert werden soll
	 * @param hash Der beim Serialisieren abgefragte Hash des Objektes, in das deserialisiert werden soll
	 * @return Das Java-Objekt, in das das angegebene JSONArray serialisiert wurde
	 * @throws DeserialiseException Wird geworfen, falls ein Fehler beim Deserialisieren des Objektes
	 * auftritt.
	 */
	private Object jsonArrayToObject(JSONArray jsonArray, Class<?> newClass, String className, int hash)
			throws DeserialiseException {
		if (newClass.isArray()) {
			Object newArray = Array.newInstance(newClass.getComponentType(), jsonArray.length() - 1);
			hashTable.put(hash, newArray);
			for (int arrayCreateIterator = 1; arrayCreateIterator < jsonArray.length();
				 arrayCreateIterator++) {
				Array.set(newArray, arrayCreateIterator - 1,
						jsonToObject(jsonArray.get(arrayCreateIterator)));
			}
			return newArray;
		}
		Object newObject;
		if (collectionHandling == CollectionHandling.NO_CASTING) {
			newObject = SerialiseUtils.getNewInstance(newClass, fieldInformation);
		} else {
			try {
				newObject = SerialiseUtils.getNewInstance(newClass, fieldInformation);
			} catch (DeserialiseException e) {
				newObject = SerialiseUtils.getNewAlternativeInstance(newClass, e, fieldInformation);
				if (collectionHandling == CollectionHandling.DEBUG_MODE) {
					e.printStackTrace();
				}
				if (collectionHandling != CollectionHandling.NO_WARNING && !ignoreCasting.peek() ||
						collectionHandling == CollectionHandling.DEBUG_MODE) {
					System.out.println("WARNING: " + fieldInformation.toString() +
							" has been converted to class " + newObject.getClass().getName() +
							". This might lead to a ClassCastException.\nIt is recommended not to use " +
							"this Collection or Map type (" + newClass.getName() + ")!");
				}
				if (collectionHandling == CollectionHandling.CONVERT_WITH_WARNING && !ignoreCasting.peek()) {
					System.out.println("This warning can be suppressed by annotating the field with the " +
							"IgnoreCasting annotation or by using the NO_WARNING mode in the constructor of " +
							"the serialiser.");
				}
			}
		}
		if (newObject instanceof Collection) {
			@SuppressWarnings("unchecked")
			Collection<Object> newCollection = (Collection<Object>) newObject;
			hashTable.put(hash, newCollection);
			for (Object jsonArrayPart : jsonArray.skipFirst()) {
				newCollection.add(jsonToObject(jsonArrayPart));
			}
			return newCollection;
		}
		if (newObject instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<Object, Object> newMap = (Map<Object, Object>) newObject;
			hashTable.put(hash, newMap);
			for (Object jsonArrayPart : jsonArray.skipFirst()) {
				JSONObject jsonArrayChild = (JSONObject) jsonArrayPart;
				Object childKey = jsonToObject(jsonArrayChild.get("key"));
				Object childValue = jsonToObject(jsonArrayChild.get("value"));
				newMap.put(childKey, childValue);
			}
			return newMap;
		}
		throw new DeserialiseException("The specified class " + className + ", used by " +
				fieldInformation.toString() + " could not be deserialised as it's neither an " +
				"array, a collection or a map.");
	}

	/**
	 * Deserialisiert ein JSONObject in das entsprechende Java-Objekt.<br>
	 * Dafür wird eine neue Instanz der Klasse des Java-Objektes erstellt. Dann wird für jede Variable des Objektes,
	 * die nicht ignoriert werden soll, das entsprechende JSON-Element aus dem JSONObject
	 * {@link #jsonToObject(Object) deserialisiert} und das entstandene Java-Objekt als Variablenwert gesetzt.
	 * @param jsonObject Das JSONObject, das deserialisiert werden soll
	 * @param newClass Die Klasse des Objektes, in das deserialisiert werden soll
	 * @param hash Der beim Serialisieren abgefragte Hash des Objektes, in das deserialisiert werden soll
	 * @return Das Java-Objekt, in das das angegebene JSONObject serialisiert wurde
	 * @throws DeserialiseException Wird geworfen, falls ein Fehler beim Deserialisieren des Objektes
	 * auftritt.
	 */
	private Object jsonObjectToObject(JSONObject jsonObject, Class<?> newClass, int hash) throws DeserialiseException {
		Object newObject = SerialiseUtils.getNewInstance(newClass, fieldInformation);
		hashTable.put(hash, newObject);

		for (Field field : SerialiseUtils.getSerialiseFields(newClass, startSerialisingInSuperclass,
				false, ignoreExceptionIDs)) {
			fieldInformation.addField(field);
			Object childJson;
			try {
				childJson = jsonObject.get(field.getName());
			} catch (DeserialiseException e) {
				String message = "JSON-Object child " + fieldInformation.toString() +
						" (from object " + newClass.getName() + " " + hash + ") could not be found.\n" +
						"This happens if this variable has been added to the class after serialisation " +
						"or if the Json has been manually edited.";
				switch (newVariableHandling) {
					case WARNING:
						System.out.println("WARNING: " + message); break;
					case EXCEPTION:
						throw new DeserialiseException(message + "\nYou can prevent this exception " +
								"by setting the newVariableHandling to WARNING or NO_WARNING", e);
				}
				continue;
			}
			ignoreCasting.push(field.isAnnotationPresent(IgnoreCasting.class));
			try {
				field.set(newObject, jsonToObject(childJson));
			} catch (IllegalArgumentException e) {
				fieldInformation.addField(field);
				throw new DeserialiseException("Type Mismatch: " + fieldInformation.toString() +
						" is not equal to JSON-Element child type", e);
			} catch (IllegalAccessException e) {
				fieldInformation.addField(field);
				throw new DeserialiseException("Illegal Access: " + fieldInformation.toString() +
						" could not be overwritten.", e);
			}
			ignoreCasting.pop();
			fieldInformation.removeField(field);
		}

		return newObject;
	}


	//----GET LINKED OBJECT METHODEN----
	//Solange die Variablen in der gleichen Reihenfolge deserialisiert werden wie sie serialisiert und
	//gespeichert wurden (und JSON nicht manuell verändert wurde), werden diese niemals aufgerufen

	/**
	 * Gibt das Objekt mit dem angegebenen Hash zurück, falls es in einer der JSON-Dateien existiert.<br>
	 * Falls es mehrere JSON-Dateien gibt, wird für jedes Objekt aus jeder JSON-Datei
	 * {@link #getLinkedObject(Object, int) getLinkedObject} aufgerufen; falls einer der Aufrufe einen
	 * Rückgabewert liefert wird dieser zurückgegeben, ansonsten null.<br>
	 * Falls es nur eine JSON-Datei gibt, wird {@link #getLinkedObject(Object, int) getLinkedObject} für diese
	 * Datei aufgerufen und der Rückgabewert zurückgegeben.
	 * @param searchedHash Der Hash, dessen zugehöriges Objekt in den JSON-Dateien zurückgegeben werden soll
	 * @return Das Objekt, das zu dem angegeben Hash in den JSON-Dateien gespeichert ist, falls es existiert;<br>
	 * ansonsten null
	 * @throws DeserialiseException Wird geworfen, falls ein Fehler aufgetreten ist, während das verlinkte
	 * Objekt gesucht wurde.
	 */
	private Object getLinkedObject(int searchedHash) throws DeserialiseException {
		if (multiFile) {
			for (JSONObject value : wholeSeparatedJson.values()) {
				for (Object object : value.values()) {
					Object linkedObject = getLinkedObject(object, searchedHash);
					if (linkedObject != null) {
						return linkedObject;
					}
				}
			}
			return null;
		} else {
			return getLinkedObject(wholeSingleJson, searchedHash);
		}
	}

	/**
	 * Gibt das Objekt mit dem angegebenen Hash zurück, falls es in dem angegebenen JSON-Element existiert.<br>
	 * Falls das angegebene JSON-Element null oder {@link SerialiseUtils#isSimpleType(Class) simpel} ist, wird null
	 * zurückgegeben.<br>
	 * Falls das angegebene JSON-Element ein JSONArray ist, wird
	 * {@link #getLinkedObjectFromJSONArray(JSONArray, int) getLinkedObjectFromJSONArray} zurückgegeben,
	 * ansonsten {@link #getLinkedObjectFromJSONObject(JSONObject, int) getLinkedObjectFromJSONObject}.
	 * @param json Das JSON-Element, in dem nach dem Objekt mit dem angegebenen Hash gesucht werden soll
	 * @param searchedHash Der Hash, dessen zugehöriges Objekt in dem angegebenen JSON-Element gesucht werden soll
	 * @return Das Objekt, das zu dem angegebenen Hash in dem JSON-Element gespeichert ist, falls es existiert;<br>
	 * ansonsten null
	 * @throws DeserialiseException Wird geworfen, falls ein Fehler aufgetreten ist, während das verlinkte
	 * Objekt gesucht wurde.
	 */
	private Object getLinkedObject(Object json, int searchedHash) throws DeserialiseException {
		if (json == null) {
			return null;
		}
		if (SerialiseUtils.isSimpleType(json.getClass())) {
			return null;
		}
		if (json instanceof JSONArray) {
			return getLinkedObjectFromJSONArray((JSONArray) json, searchedHash);
		}
		return getLinkedObjectFromJSONObject((JSONObject) json, searchedHash);
	}

	/**
	 * Gibt das Objekt mit dem angegebenen Hash zurück, falls es in dem angegebenen JSONArray existiert.<br>
	 * Falls das angegebene JSONArray den angegebenen Hash als gespeicherten Hash besitzt, wird es zurückgegeben.<br>
	 * Falls das JSONArray eine serialisierte Map ist, wird für jeden key und value
	 * {@link #getLinkedObject(Object, int) geprüft}, ob sich das gesuchte Objekt dort befindet. Falls einer der
	 * Aufrufe einen Rückgabewert liefert wird dieser zurückgegeben, ansonsten null.<br>
	 * Ansonsten wird für jedes Element aus dem JSONArray {@link #getLinkedObject(Object, int) geprüft}, ob sich das
	 * gesuchte Objekt dort befindet. Falls einer der Aufrufe einen Rückgabewert liefert wird dieser zurückgegeben,
	 * ansonsten null.
	 * @param jsonArray Das JSONArray, in dem nach dem Objekt mit dem angegebenen Hash gesucht werden soll
	 * @param searchedHash Der Hash, dessen zugehöriges Objekt in dem angegebenen JSONArray gesucht werden soll
	 * @return Das Objekt, das zu dem angegebenen Hash in dem JSONArray gespeichert ist, falls es existiert;<br>
	 * ansonsten null
	 * @throws DeserialiseException Wird geworfen, falls ein Fehler aufgetreten ist, während das verlinkte
	 * Objekt gesucht wurde.
	 */
	private Object getLinkedObjectFromJSONArray(JSONArray jsonArray, int searchedHash) throws DeserialiseException {
		String[] classInfos = jsonArray.getString(0).split("=");

		int hash = Integer.parseInt(classInfos[1]);
		if (hash == searchedHash) {
			return jsonToObject(jsonArray);
		}

		String className = classInfos[0];
		Class<?> newClass;
		try {
			newClass = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new DeserialiseException("The class " + className +
					", specified in the JSON-String could not be found.", e);
		}

		if (Map.class.isAssignableFrom(newClass)) {
			for (Object jsonArrayPart : jsonArray.skipFirst()) {
				JSONObject mapPart = (JSONObject) jsonArrayPart;
				Object keyPart = getLinkedObject(mapPart.get("key"), searchedHash);
				if (keyPart != null) {
					return keyPart;
				}
				Object valuePart = getLinkedObject(mapPart.get("value"), searchedHash);
				if (valuePart != null) {
					return valuePart;
				}
			}
			return null;
		}
		for (Object jsonArrayPart : jsonArray.skipFirst()) {
			Object part = getLinkedObject(jsonArrayPart, searchedHash);
			if (part != null) {
				return part;
			}
		}
		return null;
	}

	/**
	 * Gibt das Objekt mit dem angegebenen Hash zurück, falls es in dem angegebene JSONObject existiert.<br>
	 * Falls das angegebene JSONObject den angegebenen Hash als gespeicherten Hash besitzt, wird es zurückgegeben.<br>
	 * Ansonsten wird für jeden im JSONObject gespeicherten Wert {@link #getLinkedObject(Object, int) geprüft},
	 * ob sich das Objekt dort befindet. Falls einer der Aufrufe einen Rückgabewert liefert wird dieser
	 * zurückgegeben, ansonsten null.
	 * @param jsonObject Das JSONObject, in dem nach dem Objekt mit dem angegebenen Hash gesucht werden soll
	 * @param searchedHash Der angegebene Hash, dessen dazugehöriges Objekt zurückgegeben werden soll
	 * @return Das Objekt, das zu dem angegebenen Hash in dem JSONObject gespeichert ist, falls es existiert;<br>
	 * ansonsten null
	 * @throws DeserialiseException Wird geworfen, falls ein Fehler aufgetreten ist, während das verlinkte
	 * Objekt gesucht wurde.
	 */
	private Object getLinkedObjectFromJSONObject(JSONObject jsonObject, int searchedHash) throws DeserialiseException {
		int hash = Integer.parseInt(jsonObject.getString("class").split("=")[1]);
		if (hash == searchedHash) {
			return jsonToObject(jsonObject);
		}
		for (String jsonObjectPart : jsonObject.keySet()) {
			Object objectPart = getLinkedObject(jsonObject.get(jsonObjectPart), searchedHash);
			if (objectPart != null) {
				return objectPart;
			}
		}
		return null;
	}


	//----WEITERE HILFSMETHODEN----

	/**
	 * Gibt das Objekt mit dem angegebenen Hash in der angegebenen Datei zurück, falls es dort gespeichert
	 * wurde, ansonsten null.
	 * @param fileName Der Name der Datei, in der gesucht werden soll
	 * @param searchedHash Der Hash des Objektes, das zurückgegeben werden soll
	 * @return Das Objekt mit dem angegebenen Hash in der angegebenen Datei, falls es dort gespeichert
	 * wurde, ansonsten null
	 * @throws DeserialiseException Wird geworfen, falls ein Fehler aufgetreten ist, während das verlinkte
	 * Objekt gesucht wurde
	 */
	private Object getLinkedFileObject(String fileName, int searchedHash) throws DeserialiseException {
		JSONObject value = wholeSeparatedJson.get(fileName);
		if (value.containsKey(String.valueOf(searchedHash))) {
			return jsonToObject(value.get(String.valueOf(searchedHash)));
		}
		return null;
	}

	/**
	 * Hilfsmethode:<br>
	 * Setzt ein bestimmtes Objekt mit einem bestimmten Hash in das wholeSeparatedJson-Objekt.
	 * @param fileName Der Name der Datei, in die Das Objekt gesetzt werden soll
	 * @param childHash Der Hash des Objektes, unter dem er gespeichert werden soll
	 * @param childObject Das Objekt, welches gespeichert werden soll
	 */
	private void putInWholeSeparatedJson(String fileName, int childHash, Object childObject) {
		if (wholeSeparatedJson.containsKey(fileName)) {
			wholeSeparatedJson.get(fileName).put(String.valueOf(childHash), childObject);
		} else {
			JSONObject newObject = new JSONObject();
			newObject.put(String.valueOf(childHash), childObject);
			wholeSeparatedJson.put(fileName, newObject);
		}
	}

	/**
	 * Führt alle Methoden, die mit der {@link BeforeSerialise} Annotation annotiert wurde, aus.<br>
	 * Es wird das Objekt, für das und für alle von ihm verlinkten Objekte alle annotierten Methoden
	 * ausgeführt werden sollen, sowie eine Map, die für Klassen speichert, welche Methoden für diese Klasse
	 * sowie alle Oberklassen annotiert sind (sodass die Prüfung nicht mehrmals durchgeführt werden muss).<br>
	 * Sofern diese Methode für dieses Objekt noch nicht aufgerufen wurde, werden werden alle Methoden des
	 * Objektes geprüft und, falls annotiert ausgeführt. Danach wird diese Methode für alle verlinkten Objekte
	 * aufgerufen.
	 * @param object Das Objekt, für das und für alle von ihm verlinkten Objekte alle annotierten Methoden
	 *               ausgeführt werden sollen
	 * @param beforeSerialiseMethods Eine Map, die für Klassen speichert, welche Methoden für diese Klasse
	 *                               sowie alle Oberklassen annotiert sind
	 * @return Die aktualisierte Map, die für Klassen speichert, welche Methoden für diese Klasse sowie
	 * alle Oberklassen annotiert sind
	 * @throws SerialiseException Wird geworfen, falls ein Fehler beim Serialisieren des Objektes auftritt.
	 */
	private Map<Class<?>, List<Method>> executeBeforeSerialiseMethods(Object object,
				Map<Class<?>, List<Method>> beforeSerialiseMethods) throws SerialiseException {
		if (object == null) {
			return beforeSerialiseMethods;
		}
		int hash = System.identityHashCode(object);
		Class<?> objectClass = object.getClass();
		if (SerialiseUtils.isSimpleType(objectClass) || beforeSerialised.contains(hash) || objectClass.isEnum() ||
				SpecialClasses.getClassMap().containsKey(objectClass.getName())) {
			return beforeSerialiseMethods;
		} else {
			beforeSerialised.add(hash);
		}

		if (!beforeSerialiseMethods.containsKey(objectClass)) {
			beforeSerialiseMethods.put(objectClass,
					SerialiseUtils.getMethodsWithAnnotation(objectClass, BeforeSerialise.class));
		}

		for (Method objectMethod : beforeSerialiseMethods.get(objectClass)) {
			String methodClassName = objectMethod.getDeclaringClass().getName();
			if (objectMethod.getParameterCount() > 1) {
				throw new SerialiseException("Error: The method " + methodClassName + "#" +
						objectMethod.getName() + " is annotated with the BeforeSerialise annotation but " +
						"requires more than one parameter.\nMethods annotated with the BeforeSerialise " +
						"annotation can't use more than one parameter.");
			}
			try {
				if (objectMethod.getParameterCount() == 1) {
					String methodParameterID = objectMethod.getDeclaredAnnotation(BeforeSerialise.class).value();
					if (!methodParameters.containsKey(methodParameterID)) {
						throw new SerialiseException("Error: The method " + methodClassName + "#" +
								objectMethod.getName() + " is annotated with the BeforeSerialise annotation and " +
								"requires a parameter but there has not been set a value for the " +
								"parameterID: " + methodParameterID);
					}
					objectMethod.invoke(object, methodParameters.get(methodParameterID));
				} else {
					objectMethod.invoke(object);
				}
			} catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
				throw new SerialiseException("Error: The method " + methodClassName + "#" +
						objectMethod.getName() + " is annotated with the BeforeSerialise annotation but " +
						"could not be invoked.", e);
			}
		}

		if (object instanceof Collection) {
			for (Object child : (Collection<?>) object) {
				beforeSerialiseMethods = executeBeforeSerialiseMethods(child, beforeSerialiseMethods);
			}
			return beforeSerialiseMethods;
		}
		if (objectClass.isArray()) {
			Object[] objectArray;
			if (objectClass.getComponentType().isPrimitive()) {
				objectArray = SerialiseUtils.convertPrimitiveToArray(object);
			} else {
				objectArray = (Object[]) object;
			}
			for (Object child : objectArray) {
				beforeSerialiseMethods = executeBeforeSerialiseMethods(child, beforeSerialiseMethods);
			}
			return beforeSerialiseMethods;
		}
		if (object instanceof Map) {
			for (Object childKey : ((Map<?, ?>) object).keySet()) {
				Object childValue = ((Map<?, ?>) object).get(childKey);
				beforeSerialiseMethods = executeBeforeSerialiseMethods(childKey, beforeSerialiseMethods);
				beforeSerialiseMethods = executeBeforeSerialiseMethods(childValue, beforeSerialiseMethods);
			}
			return beforeSerialiseMethods;
		}
		for (Field field : SerialiseUtils.getSerialiseFields(objectClass, startSerialisingInSuperclass,
				false, ignoreExceptionIDs)) {
			fieldInformation.addField(field);
			Object child;
			try {
				child = field.get(object);
			} catch (IllegalAccessException e) {
				throw new SerialiseException("Fatal error: The field value of " +
						fieldInformation.toString() + " could not be retrieved", e);
			}
			beforeSerialiseMethods = executeBeforeSerialiseMethods(child, beforeSerialiseMethods);
			fieldInformation.removeField(field);
		}
		return beforeSerialiseMethods;
	}

	/**
	 * Führt alle Methoden, die mit der {@link AfterDeserialise} Annotation annotiert wurden, aus.<br>
	 * Es wird die komplette Hashtabelle mit allen Objekten, die deserialisiert wurden (in der Reihenfolge
	 * einer Tiefensuche) durchgegangen. Dabei wird für jedes Objekt geprüft, welche Methoden aus der
	 * entsprechenden Klasse und allen Oberklassen mit der Annotation versehen sind und die entsprechenden
	 * Methoden werden (von der allgemeinsten Klasse beginnend aus) ausgeführt.
	 * @throws DeserialiseException Wird geworfen, falls ein Fehler beim Deserialisieren des Objektes
	 * auftritt.
	 */
	private void executeAfterDeserialiseMethods() throws DeserialiseException {
		Map<Class<?>, List<Method>> afterDeserialiseMethods = new HashMap<>();

		for (Object object : hashTable.values()) {
			Class<?> objectClass = object.getClass();
			if (!afterDeserialiseMethods.containsKey(objectClass)) {
				afterDeserialiseMethods.put(objectClass,
						SerialiseUtils.getMethodsWithAnnotation(objectClass, AfterDeserialise.class));
			}
			for (Method method : afterDeserialiseMethods.get(objectClass)) {
				String methodClassName = method.getDeclaringClass().getName();
				if (method.getParameterCount() > 1) {
					throw new DeserialiseException("Error: The method " + methodClassName + "#" +
							method.getName() + " is annotated with the AfterDeserialise annotation but " +
							"requires more than one parameter.\nMethods annotated with the AfterDeserialise " +
							"annotation can't use more than one parameter.");
				}
				try {
					if (method.getParameterCount() == 1) {
						String methodParameterID = method.getDeclaredAnnotation(AfterDeserialise.class).value();
						if (!methodParameters.containsKey(methodParameterID)) {
							throw new DeserialiseException("Error: The method " + methodClassName + "#" +
									method.getName() + " is annotated with the AfterDeserialise annotation and " +
									"requires a parameter but there has not been set a value for the " +
									"parameterID: " + methodParameterID);
						}
						method.invoke(object, methodParameters.get(methodParameterID));
					} else {
						method.invoke(object);
					}
				} catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
					throw new DeserialiseException("Error: The method " + methodClassName + "#" +
							method.getName() + " is annotated with the AfterDeserialise annotation but " +
							"could not be invoked.", e);
				}
			}
		}
	}
}
