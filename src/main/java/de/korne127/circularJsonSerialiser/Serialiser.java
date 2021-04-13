package de.korne127.circularJsonSerialiser;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;

import de.korne127.circularJsonSerialiser.annotations.AfterDeserialise;
import de.korne127.circularJsonSerialiser.annotations.BeforeSerialise;
import de.korne127.circularJsonSerialiser.annotations.SerialiseFile;
import de.korne127.circularJsonSerialiser.annotations.SerialiseIgnore;
import de.korne127.circularJsonSerialiser.annotations.Setter;
import de.korne127.circularJsonSerialiser.exceptions.DeserialiseException;
import de.korne127.circularJsonSerialiser.exceptions.JsonParseException;
import de.korne127.circularJsonSerialiser.exceptions.SerialiseException;
import de.korne127.circularJsonSerialiser.json.JSONArray;
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

	//Für Fehlermeldungen
	private Field currentField;
	private FieldType currentFieldType;

	private Map<String, JSONObject> wholeSeparatedJson;
	private JSONObject wholeSingleJson;
	private boolean multiFile;

	//Konfiguration
	private final CollectionHandling collectionHandling;
	private final boolean startSerialisingInSuperclass;
	private Map<String, Object> methodParameters;

	/**
	 * Enum, das die verschiedenen Optionen, wie sich der Serialiser verhalten soll, wenn
	 * keine Instanz einer Collection oder Map erstellt werden kann.<br>
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
	 * Enum über den Feldtyp des Feldes, welches aktuell serialisiert wird.<br>
	 * Es wird nur verwendet, falls eine Fehlermeldung auftritt um das aktuelle Feld besser anzugeben.
	 */
	private enum FieldType {
		FIELD,
		ARRAY,
		COLLECTION,
		MAP
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
		methodParameters = new HashMap<>();
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
		currentField = null;
		currentFieldType = null;
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
		currentField = null;
		currentFieldType = null;
		wholeSeparatedJson = new HashMap<>();
		multiFile = true;
		beforeSerialised = new HashSet<>();

		String mainFileName = getFileName(object);
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
	 * Falls das angegebene Objekt {@link #isSimpleType(Class) simpel} ist, wird
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
		if (isSimpleType(objectClass)) {
			return simpleTypeToJson(object, objectClass);
		}
		if (objectClass.isEnum()) {
			return "#" + objectClass.getName() + "=" + object.toString();
		}

		int objectHash = System.identityHashCode(object);
		if (hashTable.containsKey(objectHash)) {
			return "@" + objectHash;
		}
		hashTable.put(objectHash, object);

		if (objectClass.isArray() || object instanceof Collection) {
			return collectionToJson(object, objectClass, objectHash, parentFileName);
		}
		if (object instanceof Map) {
			return mapToJson((Map<?, ?>) object, objectClass, objectHash, parentFileName);
		}
		if (SpecialClasses.getClassMap().containsKey(objectClass.getName())) {
			return specialClassToJson(object, objectClass);
		}
		return javaObjectToJson(object, objectClass, objectHash, parentFileName);
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
			currentFieldType = FieldType.ARRAY;
			Object[] objectArray;
			if (objectClass.getComponentType().isPrimitive()) {
				objectArray = convertPrimitiveToArray(object);
			} else {
				objectArray = (Object[]) object;
			}
			objectIterable = Arrays.asList(objectArray);
		} else {
			currentFieldType = FieldType.COLLECTION;
			objectIterable = (Collection<?>) object;
		}
		JSONArray jsonChild = new JSONArray();
		jsonChild.put(objectClass.getName() + "=" + objectHash);
		for (Object objectChild : objectIterable) {
			String childFileName = getFileName(objectChild);
			if (multiFile && parentFileName.equals(childFileName)) {
				childFileName = null;
			}
			Object childJson = objectToJson(objectChild, childFileName == null ? parentFileName : childFileName);
			if (!multiFile || childJson == null || childFileName == null || isSimpleType(childJson.getClass())) {
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
		currentFieldType = FieldType.MAP;
		JSONArray map = new JSONArray();
		map.put(objectClass.getName() + "=" + objectHash);
		for (Object objectMapKey : objectMap.keySet()) {
			Object objectMapValue = objectMap.get(objectMapKey);
			JSONObject mapChild = new JSONObject();

			String keyFileName = getFileName(objectMapKey);
			if (multiFile && parentFileName.equals(keyFileName)) {
				keyFileName = null;
			}
			Object mapKeyJson = objectToJson(objectMapKey, keyFileName == null ? parentFileName : keyFileName);
			if (!multiFile || mapKeyJson == null || keyFileName == null || isSimpleType(mapKeyJson.getClass())) {
				mapChild.put("key", mapKeyJson);
			} else {
				int childHash = System.identityHashCode(objectMapKey);
				mapChild.put("key", "@" + keyFileName + "#" + childHash);
				putInWholeSeparatedJson(keyFileName, childHash, mapKeyJson);
			}

			String valueFileName = getFileName(objectMapValue);
			if (multiFile && parentFileName.equals(valueFileName)) {
				valueFileName = null;
			}
			Object mapValueJson = objectToJson(objectMapValue, valueFileName == null ? parentFileName : valueFileName);
			if (!multiFile || mapValueJson == null || valueFileName == null || isSimpleType(mapValueJson.getClass())) {
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
			throw new SerialiseException(getCurrentFieldInformation(objectClass.getName()) +
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
		for (Field objectField : getSerialiseFields(objectClass, startSerialisingInSuperclass, false)) {
			currentField = objectField;
			currentFieldType = FieldType.FIELD;
			Object child;
			try {
				child = objectField.get(object);
			} catch (IllegalAccessException e) {
				throw new SerialiseException("Fatal error: The field value of " +
						getCurrentFieldInformation(objectField.getType().getName()) +
						" could not be retrieved", e);
			}
			String currentFileName = getFileName(objectField, child);
			if (multiFile && parentFileName.equals(currentFileName)) {
				currentFileName = null;
			}
			Object childJson = objectToJson(child, currentFileName == null ? parentFileName : currentFileName);
			if (!multiFile || childJson == null || currentFileName == null || isSimpleType(childJson.getClass())) {
				jsonObject.put(objectField.getName(), childJson);
			} else {
				int childHash = System.identityHashCode(child);
				jsonObject.put(objectField.getName(), "@" + currentFileName + "#" + childHash);
				putInWholeSeparatedJson(currentFileName, childHash, childJson);
			}
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
		currentField = null;
		currentFieldType = null;
		wholeSingleJson = new JSONObject(content);
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
		currentField = null;
		currentFieldType = null;
		wholeSeparatedJson = new HashMap<>();
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
	 * Falls das angegebene Objekt der JSON-Datei {@link #isSimpleType(Class) simpel} ist, wird es über
	 * {@link #simpleTypeToObject(Object) simpleTypeToObject} deserialisiert und zurückgegeben.<br>
	 * Ansonsten werden die Informationen abgefragt, wie die Klasse und der beim Serialisieren gespeicherte Hash
	 * des Java-Objektes ist.<br>
	 * Falls das Objekt mit diesem Hash bereits deserialisiert wurde, wird es zurückgegeben.<br>
	 * Falls das angegebene Objekt ein JSONArray ist, wird es über
	 * {@link #jsonArrayToObject(JSONArray, Class, String, int) jsonArrayToObject} deserialisiert und zurückgegeben,
	 * ansonsten über {@link #jsonObjectToObject(JSONObject, Class, int) jsonObjectToObject}.<br>
	 * Zusätzlich werden Variablen des Objektes, die mit der {@link Setter} Annotation annotiert sind, auf das
	 * mit der ParameterID verknüpfte Objekt gesetzt.
	 * @param json Das angegebene Objekt aus der JSON-Datei, das deserialisiert werden soll
	 * @return Ein Java-Objekt, in das das angegebene Objekt deserialisiert wurde
	 * @throws DeserialiseException Wird geworfen, falls ein Fehler beim Deserialisieren des Objektes
	 * auftritt.
	 */
	private Object jsonToObject(Object json) throws DeserialiseException {
		if (json == null) {
			return null;
		}
		if (isSimpleType(json.getClass())) {
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
		Class<?> newClass;
		try {
			newClass = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new DeserialiseException("The specified class " + className + ", used by " +
					getCurrentFieldInformation(className) + " could not be found.", e);
		}

		Object result;
		if (json instanceof JSONArray) {
			result = jsonArrayToObject((JSONArray) json, newClass, className, hash);
		} else {
			result = jsonObjectToObject((JSONObject) json, newClass, hash);
		}

		for (Field field : getSerialiseFields(newClass, true, true)) {
			String parameterID = field.getDeclaredAnnotation(Setter.class).value();
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
				throw new DeserialiseException("The definition of the linked object " +
						string + "(" + getCurrentFieldInformation(null) +
						") could not be found.");
			}
			return linkedObject;
		}
		int hash = Integer.parseInt(string.substring(1));
		if (hashTable.containsKey(hash)) {
			return hashTable.get(hash);
		}
		Object linkedObject = getLinkedObject(hash);
		if (linkedObject == null) {
			throw new DeserialiseException("The definition of the linked object " +
					string + "(" + getCurrentFieldInformation(null) +
					") could not be found.");
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
		String classValue = classInfos[1];
		if (SpecialClasses.getClassMap().containsKey(className)) {
			SpecialClasses specialClass = SpecialClasses.getClassMap().get(className);
			Object result = specialClass.deserialise(classValue);
			if (result == null) {
				throw new DeserialiseException(getCurrentFieldInformation(className) +
						", value: " + classValue + " could not be deserialised.");
			}
			return result;
		}
		Class<?> objectClass;
		try {
			objectClass = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new DeserialiseException("The specified class " + className + ", used by " +
					getCurrentFieldInformation(className) + " could not be found.", e);
		}
		if (objectClass.isEnum()) {
			return getNewEnumInstance(classValue, objectClass);
		}
		throw new DeserialiseException("The definition of the special class " + className +
				", used by " + getCurrentFieldInformation(className) + " could not be found.");
	}

	/**
	 * Deserialisiert ein JSONArray in das entsprechende Java-Objekt: Ein Array, eine Collection oder eine Map.<br>
	 * Falls das Objekt ein Array ist, werden die einzelnen Inhalte des JSONArrays
	 * {@link #jsonToObject(Object) deserialisiert} und einem neuen Array hinzugefügt, das zurückgegeben wird.<br>
	 * Ansonsten wird eine Instanz der Collection bzw. Map des Objektes erstellt; falls dies nicht möglich ist
	 * wird eventuell ein {@link #getNewAlternativeInstance(Class, Exception) möglichst ähnlicher}
	 * Collection- bzw. Map-Typ benutzt und abhängig von der {@link CollectionHandling Konfiguration} eine Warnung
	 * ausgegeben.<br>
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
			currentFieldType = FieldType.ARRAY;
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
			newObject = getNewInstance(newClass);
		} else {
			try {
				newObject = getNewInstance(newClass);
			} catch (DeserialiseException e) {
				newObject = getNewAlternativeInstance(newClass, e);
				if (collectionHandling == CollectionHandling.DEBUG_MODE) {
					e.printStackTrace();
				}
				if (collectionHandling != CollectionHandling.NO_WARNING) {
					System.out.println("WARNING: Class " + newClass.getName() +
							" has been converted to class " + newObject.getClass().getName() +
							". This might lead to a ClassCastException.\nIt is recommended not to use " +
							"this Collection or Map type (" + newClass.getName() + ")!");
				}
				if (collectionHandling == CollectionHandling.CONVERT_WITH_WARNING) {
					System.out.println("This warning can be suppressed by using the NO_WARNING mode " +
							"in the constructor of the serialiser.");
				}
			}
		}
		if (newObject instanceof Collection) {
			currentFieldType = FieldType.COLLECTION;
			@SuppressWarnings("unchecked")
			Collection<Object> newCollection = (Collection<Object>) newObject;
			hashTable.put(hash, newCollection);
			for (Object jsonArrayPart : jsonArray.skipFirst()) {
				newCollection.add(jsonToObject(jsonArrayPart));
			}
			return newCollection;
		}
		if (newObject instanceof Map) {
			currentFieldType = FieldType.MAP;
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
				getCurrentFieldInformation(className) + " could not be deserialised as it's neither an " +
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
		Object newObject = getNewInstance(newClass);
		hashTable.put(hash, newObject);

		for (Field field : getSerialiseFields(newClass, startSerialisingInSuperclass, false)) {
			currentField = field;
			currentFieldType = FieldType.FIELD;
			Object childJson = jsonObject.get(field.getName());
			try {
				field.set(newObject, jsonToObject(childJson));
			} catch (IllegalArgumentException e) {
				throw new DeserialiseException("Type Mismatch: " +
						getCurrentFieldInformation(field.getType().getName()) +
						" is not equal to JSON-Element child type", e);
			} catch (IllegalAccessException e) {
				throw new DeserialiseException("Illegal Access: " +
						getCurrentFieldInformation(field.getType().getName()) +
						" could not be overwritten.", e);
			}
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
	 * Falls das angegebene JSON-Element null oder {@link #isSimpleType(Class) simpel} ist, wird null
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
		if (isSimpleType(json.getClass())) {
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
			throw new DeserialiseException("The specified class " + className + ", used by " +
					getCurrentFieldInformation(className) + " could not be found.", e);
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
		if (isSimpleType(objectClass) || beforeSerialised.contains(hash) || objectClass.isEnum() ||
				SpecialClasses.getClassMap().containsKey(objectClass.getName())) {
			return beforeSerialiseMethods;
		} else {
			beforeSerialised.add(hash);
		}

		if (!beforeSerialiseMethods.containsKey(objectClass)) {
			beforeSerialiseMethods.put(objectClass, getMethodsWithAnnotation(objectClass, BeforeSerialise.class));
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
				objectArray = convertPrimitiveToArray(object);
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
		for (Field field : getSerialiseFields(objectClass, startSerialisingInSuperclass, false)) {
			currentField = field;
			currentFieldType = FieldType.FIELD;
			Object child;
			try {
				child = field.get(object);
			} catch (IllegalAccessException e) {
				throw new SerialiseException("Fatal error: The field value of " +
						getCurrentFieldInformation(field.getType().getName()) + " could not be retrieved", e);
			}
			beforeSerialiseMethods = executeBeforeSerialiseMethods(child, beforeSerialiseMethods);
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
						getMethodsWithAnnotation(objectClass, AfterDeserialise.class));
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

	/**
	 * Hilfsmethode:<br>
	 * Gibt eine neue Instanz einer angegebenen Klasse zurück.<br>
	 * Dafür wird der Standardkonstruktor der Klasse ausgeführt und die neue Instanz zurückgegeben.
	 * @param objectClass Die angegebene Klasse, von der eine neue Instanz zurückgegeben werden soll
	 * @return Die neue Instanz der angegebenen Klasse
	 * @throws DeserialiseException Wird geworfen, falls die neue Instanz der Klasse nicht instanziiert
	 * werden konnte.
	 */
	private Object getNewInstance(Class<?> objectClass) throws DeserialiseException {
		Constructor<?> constructor;
		try {
			constructor = objectClass.getDeclaredConstructor();
		} catch (NoSuchMethodException e) {
			throw new DeserialiseException("No default constructor found for class " + objectClass.getName() +
					", used by " + getCurrentFieldInformation(objectClass.getName()) + ".", e);
		}
		constructor.setAccessible(true);
		try {
			return constructor.newInstance();
		} catch (InstantiationException e) {
			throw new DeserialiseException("Class " + objectClass.getName() + ", used by " +
					getCurrentFieldInformation(objectClass.getName()) + " could not be instantiated.", e);
		} catch (IllegalAccessException e) {
			throw new DeserialiseException("Illegal access: Class " + objectClass.getName() + ", used by " +
					getCurrentFieldInformation(objectClass.getName()) + " could not be instantiated.", e);
		} catch (InvocationTargetException e) {
			throw new DeserialiseException("The default constructor of class " + objectClass.getName() +
					", used by " + getCurrentFieldInformation(objectClass.getName()) + " threw an exception.", e);
		}
	}

	/**
	 * Hilfsmethode:<br>
	 * Sucht nach einer alternativen Klasse, falls keine Instanz der eigentlichen Collection / Map erstellt
	 * werden konnte.
	 * @param objectClass Die eigentliche Klasse, von der keine Instanz erstellt werden konnte
	 * @param cause Die Exception, die geworfen wurde, als versucht wurde, die eigentliche Klasse zu erstellen
	 * @return Eine Instanz aus einer Alternativklasse
	 * @throws DeserialiseException Wird geworfen, wenn keine Alternativklasse gefunden werden konnte.
	 */
	private Object getNewAlternativeInstance(Class<?> objectClass, Exception cause) throws DeserialiseException {
		if (List.class.isAssignableFrom(objectClass)) {
			return new ArrayList<>();
		}
		if (Set.class.isAssignableFrom(objectClass)) {
			if (SortedSet.class.isAssignableFrom(objectClass)) {
				return new TreeSet<>();
			}
			return new LinkedHashSet<>();
		}
		if (Map.class.isAssignableFrom(objectClass)) {
			if (SortedMap.class.isAssignableFrom(objectClass)) {
				if (ConcurrentNavigableMap.class.isAssignableFrom(objectClass)) {
					return new ConcurrentSkipListMap<>();
				}
				return new TreeMap<>();
			}
			if (ConcurrentMap.class.isAssignableFrom(objectClass)) {
				return new ConcurrentHashMap<>();
			}
			if (Bindings.class.isAssignableFrom(objectClass)) {
				return new SimpleBindings();
			}
			return new LinkedHashMap<>();
		}
		if (Queue.class.isAssignableFrom(objectClass)) {
			if (BlockingQueue.class.isAssignableFrom(objectClass)) {
				if (TransferQueue.class.isAssignableFrom(objectClass)) {
					return new LinkedTransferQueue<>();
				}
				if (BlockingDeque.class.isAssignableFrom(objectClass)) {
					return new LinkedBlockingDeque<>();
				}
				return new LinkedBlockingQueue<>();
			}
			if (Deque.class.isAssignableFrom(objectClass)) {
				return new LinkedList<>();
			}
			return new LinkedList<>();
		}
		throw new DeserialiseException("Class " + objectClass + ", used by " +
				getCurrentFieldInformation(objectClass.getName()) +
				" could not be instantiated and no alternative class could be found.", cause);
	}

	/**
	 * Hilfsmethode:<br>
	 * Gibt einen String, der Informationen über das Element, was aktuell serialisiert oder
	 * deserialisiert wird, zurück. Dies wird für Fehlermeldungen benutzt, falls bei der
	 * Serialisierung oder Deserialisiserung ein Fehler auftritt.
	 * @param currentType Die Klasse des Elementes, was aktuell serialisisert wird
	 * @return Ein String, der Informationen über das Element, was aktuell serialisisert oder
	 * deserialisisert wird
	 */
	private String getCurrentFieldInformation(String currentType) {
		String message;
		switch (currentFieldType) {
			case FIELD: message = "Field "; break;
			case ARRAY: message = "Element of array "; break;
			case COLLECTION: message = "Element of collection "; break;
			case MAP: message = "Element of Map "; break;
			default: throw new IllegalStateException("Field type must have a value.");
		}
		if (currentType == null) {
			return message + currentField.getDeclaringClass().getName() + "#" + currentField.getName();
		}
		return message + currentField.getDeclaringClass().getName() + "#" + currentField.getName() +
				" (type: " + currentType + ")";
	}


	//----STATISCHE HILFSMETHODEN----

	/**
	 * Hilfsmethode:<br>
	 * Gibt Felder aus der angegebenen Klasse und aller Oberklassen in einer Liste zurück. Wenn onlySetterFields
	 * true ist, werden nur die entsprechenden Felder, die eine {@link Setter} Annotation besitzen, zurückgegeben;
	 * bei false werden alle Felder, die für das (De-)Serialisieren benutzt werden sollen, zurückgegeben.<br>
	 * Die Reihenfolge, ob die Felder aus den Oberklassen zuerst aufgelistet werden sollen oder ob die Felder
	 * aus der eigentlichen Klasse zuerst aufgelistet werden soll, wird durch einen angegebenen boolean bestimmt.
	 * @param objectClass Die angegebene Klasse, aus der alle Felder, die für das (De-)Serialisieren benutzt werden
	 *                    können, zurückgegeben werden sollen
	 * @param startAtTheTop true - Die Felder der höchsten Oberklasse werden zuerst gelistet und die der
	 *                      eigentlichen Klasse werden als letztes gelistet;<br>
	 *                      false - Die Felder der eigentlichen Klasse werden zuerst gelistet und die der
	 *                      höchsten Oberklasse werden als letztes gelistet
	 * @return Eine Liste mit allen Feldern aus der angegebenen Klasse und aller Oberklassen, die für das
	 * (De-)Serialisieren benutzt werden können
	 */
	private static List<Field> getSerialiseFields(Class<?> objectClass, boolean startAtTheTop,
												  boolean onlySetterFields) {
		List<Field> fieldList = new LinkedList<>();
		LinkedList<Class<?>> classList = getAllSuperclasses(objectClass, startAtTheTop);
		while (!classList.isEmpty()) {
			objectClass = classList.pop();
			for (Field field : objectClass.getDeclaredFields()) {
				field.setAccessible(true);
				if (!isStaticFinal(field) &&
						(!onlySetterFields && !field.isAnnotationPresent(SerialiseIgnore.class) ||
						onlySetterFields && field.isAnnotationPresent(Setter.class))) {
					fieldList.add(field);
				}
			}
		}
		return fieldList;
	}

	/**
	 * Hilfsmethode:<br>
	 * Gibt alle Methoden aus der angegebenen Klasse und aller Oberklassen, die mit der bestimmten angegebenen
	 * Annotation annotiert sind, in einer Liste zurück.
	 * @param objectClass Die angegebene Klasse, aus der und deren Oberklassen alle Methoden mit der bestimmten
	 *                    Annotation zurückgegeben werden sollen
	 * @param annotation Die bestimmte Annotation
	 * @return Eine Liste mit allen Methoden aus der angegebenen Klasse und aller Oberklassen, die mit der
	 * bestimmten angegebenen Annotation annotiert sind
	 */
	private static List<Method> getMethodsWithAnnotation(Class<?> objectClass,
														 Class<? extends Annotation> annotation) {
		List<Method> methodList = new LinkedList<>();
		LinkedList<Class<?>> classList = getAllSuperclasses(objectClass, true);
		while (!classList.isEmpty()) {
			objectClass = classList.pop();
			for (Method method : objectClass.getDeclaredMethods()) {
				method.setAccessible(true);
				if (method.isAnnotationPresent(annotation)) {
					methodList.add(method);
				}
			}
		}
		return methodList;
	}

	/**
	 * Hilfsmethode:<br>
	 * Gibt alle Oberklassen einer angegebenen Klasse (inklusive der angegebenen Klasse selbst) in einer
	 * Liste zurück. Ein angegebener boolean gibt an, ob in der zurückgegebenen Liste die oberste Klasse
	 * oder die unterste Klasse das erste Element ist.
	 * @param objectClass Die angegebene Klasse, für die alle Oberklassen zurückgegeben werden solle
	 * @param startAtTheTop true - Die oberste Klasse ist das erste Element der Liste und die eigentliche Klasse
	 *                      das letzte Element der Liste;<br>
	 *                      false - Die eigentliche Klasse ist das erste Element der Liste und die oberste Klasse
	 *                      das letzte Element der Liste
	 * @return Eine Liste mit allen Oberklassen einer angegebenen Klasse (inklusive der angegebenen Klasse selbst)
	 */
	private static LinkedList<Class<?>> getAllSuperclasses(Class<?> objectClass, boolean startAtTheTop) {
		LinkedList<Class<?>> classList = new LinkedList<>();
		if (startAtTheTop) {
			while (objectClass != null) {
				classList.addFirst(objectClass);
				objectClass = objectClass.getSuperclass();
			}
		} else {
			while (objectClass != null) {
				classList.addLast(objectClass);
				objectClass = objectClass.getSuperclass();
			}
		}
		return classList;
	}

	/**
	 * Hilfsmethode:<br>
	 * Konvertiert ein Array eines primitiven Datentyps (z.B. int) in ein Array des Wrapper-Objektes dieses
	 * Datentyps (z.B. Integer).
	 * @param val Das Array eines primitiven Datentyps, welches konvertiert werden soll
	 * @return Das konvertierte Array eines Wrapper-Objektes
	 */
	private static Object[] convertPrimitiveToArray(Object val) {
		int length = Array.getLength(val);
		Object[] outputArray = new Object[length];
		for(int i = 0; i < length; i++){
			outputArray[i] = Array.get(val, i);
		}
		return outputArray;
	}

	/**
	 * Hilfsmethode:<br>
	 * Gibt eine neue Instanz des angegebenen Enums mit dem angegebenen Wert zurück
	 * @param name Der Wert des Enums, der zurückgegeben werden soll
	 * @param type Die Enum-Klasse
	 * @return Eine neue Instanz des angegebenen Enums mit dem angegebenen Wert
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Enum<T>> T getNewEnumInstance(String name, Class<?> type) {
		return Enum.valueOf((Class<T>) type, name);
	}

	/**
	 * Hilfsmethode:<br>
	 * Gibt den Namen der Datei zurück, in die das angegebene Objekt serialisiert werden soll
	 * @param field Das Feld, in dem das angegebene Objekt gespeichert ist
	 * @param object Das angegebene Objekt
	 * @return Der Name der Datei, in die das angegebene Objekt serialisiert werden soll
	 */
	private static String getFileName(Field field, Object object) {
		if (field.isAnnotationPresent(SerialiseFile.class)) {
			return field.getDeclaredAnnotation(SerialiseFile.class).value();
		}
		return getFileName(object);
	}

	/**
	 * Hilfsmethode:<br>
	 * Gibt den Namen der Datei zurück, in die das angegebene Objekt serialisiert werden soll
	 * @param object Das angegebene Objekt
	 * @return Der Name der Datei, in die das angegebene Objekt serialisiert werden soll
	 */
	private static String getFileName(Object object) {
		if (object == null) {
			return null;
		}
		Class<?> objectClass = object.getClass();
		while (objectClass.isArray()) {
			objectClass = objectClass.getComponentType();
		}
		if (objectClass.isAnnotationPresent(SerialiseFile.class)) {
			return objectClass.getDeclaredAnnotation(SerialiseFile.class).value();
		}
		return null;
	}

	/**
	 * Hilfsmethode:<br>
	 * Testet, ob ein bestimmtes Feld static und final ist
	 * @param field Das Feld, welches getestet werden soll
	 * @return true - das Feld ist static und final;<br>
	 *     false - das Feld ist nicht static und final
	 */
	private static boolean isStaticFinal(Field field) {
		int modifiers = field.getModifiers();
		return (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers));
	}

	/**
	 * Hilfsmethode:<br>
	 * Prüft, ob eine angegebene Klasse die Klasse eines Wrapper-Objektes eines simplen Datentyps oder ein
	 * String ist.
	 * @param objectClass Die Klasse, die geprüft werden soll
	 * @return true, falls die angegebene Klasse die Klasse eines Wrapper-Objektes eines simplen Datentyps
	 * oder ein String ist, ansonsten false
	 */
	public static boolean isSimpleType(Class<?> objectClass) {
		return (objectClass == Integer.class || objectClass == Byte.class || objectClass == Short.class ||
				objectClass == Long.class || objectClass == Float.class || objectClass == Double.class ||
				objectClass == Character.class || objectClass == Boolean.class || objectClass == String.class);
	}
}
