package de.korne127.circularJsonSerialiser;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
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

import de.korne127.circularJsonSerialiser.annotations.SerialiseFile;
import de.korne127.circularJsonSerialiser.annotations.SerialiseIgnore;
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

	private Map<String, JSONObject> wholeSeperatedJson;
	private JSONObject wholeSingleJson;
	private boolean multiFile;

	private final boolean autoConvertCollections;

	/**
	 * Der standardmäßige Konstruktor der Serialiser-Klasse:<br>
	 * Erstellt einen neuen Serialiser.<br>
	 * Ruft intern {@link #Serialiser(boolean) Serialiser(true)} auf.
	 */
	public Serialiser() {
		this(true);
	}

	/**
	 * Ein Konstruktor der Serialiser-Klasse:<br>
	 * Erstellt einen neuen Serialiser und stellt ein, ob wenn eine Collection oder Map nicht deserialisiert
	 * werden konnte, automatisch versucht werden soll, einen andere passende Collection/Map-Klasse zu benutzen,
	 * oder ob eine Exception geworfen werden soll
	 * @param autoConvertCollections true - Falls eine Collection oder Map nicht deserialisiert werden kann,
	 *                               soll versucht werden, eine andere passende Collection/Map-Klasse zu
	 *                               benutzen;<br>
	 *                               false - Falls eine Collection oder Map nicht deserialisiert werden kann,
	 *                               soll eine {@link DeserialiseException} geworfen werden.
	 */
	public Serialiser(boolean autoConvertCollections) {
		this.autoConvertCollections = autoConvertCollections;
	}

	/**
	 * Konvertiert das angegebene Objekt zu einem JSON-Objekt, welches als String zurückgegeben wird.<br>
	 * Für Implementierungsdetails, siehe {@link #objectToJson(Object object)}.
	 * @param object Das Objekt, welches zu einem JSON-Objekt konvertiert werden soll
	 * @return Das aus dem angegebenen Objekt kodierte JSON-Objekt
	 * @throws SerialiseException Wird geworfen, falls ein Fehler beim Serialisieren des Objektes
	 * aufgetreten ist.
	 */
	public String serialiseObject(Object object) throws SerialiseException {
		hashTable = new HashMap<>();
		multiFile = false;

		return objectToJson(object).toString();
	}

	/**
	 * Konvertiert das angegebene Objekt zu mehreren JSON-Objekten, welche in Dateien unterteilt als Strings
	 * in einer Map zurückgegeben werden. Dabei bestimmen die SerialiseFile Annotations an einer Klasse in
	 * welcher Datei ein Objekt der Klasse zurückgegeben wird.<br>
	 * Für Implementierungsdetails, siehe {@link #objectToJson(Object object)}.
	 * @param object Das Objekt, welches zu JSON-Objekten konvertiert werden soll
	 * @return Die aus dem angegebenen Objekt kodierten JSON-Objekte in einer Map zum Dateinamen zugeordnet
	 * @throws SerialiseException Wird geworfen, falls ein Fehler beim Serialisen des Objektes
	 * aufgetreten ist.
	 */
	public Map<String, String> serialiseObjectSeparated(Object object) throws SerialiseException {
		hashTable = new HashMap<>();
		wholeSeperatedJson = new HashMap<>();
		multiFile = true;

		Object mainObject = objectToJson(object);
		String mainFileName = getFileName(object.getClass());
		if (wholeSeperatedJson.containsKey(mainFileName)) {
			wholeSeperatedJson.get(mainFileName).put("Main", mainObject);
		} else {
			JSONObject newObject = new JSONObject();
			newObject.put("Main", mainObject);
			wholeSeperatedJson.put(mainFileName, newObject);
		}

		Map<String, String> resultMap = new HashMap<>();
		for (String key : wholeSeperatedJson.keySet()) {
			resultMap.put(key, wholeSeperatedJson.get(key).toString());
		}
		return resultMap;
	}

	/**
	 * Konvertiert rekursiv ein angegebenes Objekt zu einem für JSON benutzbares Objekt:<br>
	 * Falls das angegebene Objekt null oder {@link #isSimpleType(Class) simpel} ist, wird es zurückgegeben.<br>
	 * Falls das angegebene Objekt bereits an einer anderen Stelle gespeichert wird, wird nur ein Verweis auf
	 * diese Stelle gespeichert und zurückgegeben.<br>
	 * Falls das angegebene Objekt in einer anderen Datei gespeichert werden soll, wird nur ein Verweis auf die
	 * Stelle in der anderen Datei gespeichert und das Objekt an dieser Stelle gespeichert.<br>
	 * Falls das angegebene Objekt ein Array oder eine Collection ist, wird die Methode für alle Elemente
	 * aufgerufen und zusammen in einem JSON-Array zurückgegeben.<br>
	 * Falls das angegebene Objekt eine Map ist, wird die Methode für alle Keys und dazugehörigen Values
	 * aufgerufen, je ein Key und der dazugehörige value in einem JSON-Objekt gespeichert und alle JSON-Objekte
	 * in einem JSON-Array zurückgegeben.<br>
	 * Ansonsten werden alle Felder des Objektes (und aller Oberklassen) durchgegangen und aufgerufen und in
	 * einem JSON-Objekt gespeichert, welches dann zurückgegeben wird.
	 * @param object Das Objekt, welches zu einem für JSON benutzbaren Objekt umgewandelt werden soll
	 * @return Das aus dem angegebenen Objekt kodierte für JSON benutzbare Objekt
	 * @throws SerialiseException Wird geworfen, falls ein Fehler beim Serialisieren des Objektes auftritt.
	 */
	private Object objectToJson(Object object) throws SerialiseException {
		if (object == null) {
			return null;
		}
		Class<?> objectClass = object.getClass();
		if (isSimpleType(objectClass)) {
			if (objectClass == String.class) {
				String string = (String) object;
				if (string.matches("(\\\\)*[@#].*")) {
					string = "\\" + string;
					return string;
				}
			}
			return object;
		}

		int objectHash = System.identityHashCode(object);
		if (hashTable.containsKey(objectHash)) {
			return "@" + objectHash;
		}
		hashTable.put(objectHash, object);
		if (objectClass.isArray()) {
			JSONArray array = new JSONArray();
			array.put(objectClass.getName() + "=" + objectHash);
			Object[] objectArray;
			if (objectClass.getComponentType().isPrimitive()) {
				objectArray = convertPrimitiveToArray(object);
			} else {
				objectArray = (Object[]) object;
			}
			for (Object objectArrayChild : objectArray) {
				array.put(objectToJson(objectArrayChild));
			}
			return array;
		}
		if (object instanceof Collection) {
			JSONArray collection = new JSONArray();
			collection.put(objectClass.getName() + "=" + objectHash);
			Collection<?> objectCollection = (Collection<?>) object;
			for (Object objectCollectionChild : objectCollection) {
				collection.put(objectToJson(objectCollectionChild));
			}
			return collection;
		}
		if (object instanceof Map) {
			JSONArray map = new JSONArray();
			map.put(objectClass.getName() + "=" + objectHash);
			Map<?, ?> objectMap = (Map<?, ?>) object;
			for (Object objectMapKey : objectMap.keySet()) {
				Object objectMapValue = objectMap.get(objectMapKey);
				JSONObject mapChild = new JSONObject();
				mapChild.put("key", objectToJson(objectMapKey));
				mapChild.put("value", objectToJson(objectMapValue));
				map.put(mapChild);
			}
			return map;
		}
		if (SpecialClasses.getClassMap().containsKey(objectClass.getName())) {
			SpecialClasses specialClass = SpecialClasses.getClassMap().get(objectClass.getName());
			String result = specialClass.serialise(object);
			if (result == null) {
				throw new SerialiseException("Object from class "
						+ objectClass.getName() + " could not be deserialised.");
			}
			return "#" + specialClass.getSpecialClassName() + "=" + result;
		}
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("class", objectClass.getName() + "=" + objectHash);
		while (objectClass != null) {
			for (Field objectField : objectClass.getDeclaredFields()) {
				objectField.setAccessible(true);
				if (objectField.isAnnotationPresent(SerialiseIgnore.class)) {
					continue;
				}
				Object child;
				try {
					child = objectField.get(object);
				} catch (IllegalAccessException e) {
					throw new SerialiseException("Fatal error: The field value of the field " +
							objectField.getName() +" in the class " + objectField.getDeclaringClass() +
							" could not be retrieved.", e);
				}
				String fileName = getFileName(objectField.getType());
				Object childObject = objectToJson(child);
				if (!multiFile || childObject == null || isSimpleType(childObject.getClass()) ||
						fileName.equals(getFileName(objectField.getDeclaringClass()))) {
					jsonObject.put(objectField.getName(), childObject);
				} else {
					int childHash = System.identityHashCode(child);
					jsonObject.put(objectField.getName(), "@" + fileName + "#" + childHash);
					if (wholeSeperatedJson.containsKey(fileName)) {
						wholeSeperatedJson.get(fileName).put(String.valueOf(childHash), childObject);
					} else {
						JSONObject newObject = new JSONObject();
						newObject.put(String.valueOf(childHash), childObject);
						wholeSeperatedJson.put(fileName, newObject);
					}
				}
			}
			objectClass = objectClass.getSuperclass();
		}
		return jsonObject;
	}

	/**
	 * Hilfsmethode<br>
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

	/**
	 * Hilfsmethode<br>
	 * Konvertiert ein Array eines primitiven Datentyps (z.B. int) in ein Array des Wrapper-Objektes dieses
	 * Datentyps (z.B. Integer).
	 * @param val Das Array eines primitiven Datentyps, welches konvertiert werden soll
	 * @return Das konvertierte Array eines Wrapper-Objektes
	 */
	private Object[] convertPrimitiveToArray(Object val) {
		int length = Array.getLength(val);
		Object[] outputArray = new Object[length];
		for(int i = 0; i < length; i++){
			outputArray[i] = Array.get(val, i);
		}
		return outputArray;
	}

	/**
	 * Berechnet aus dem angegebenen JSON-String ein Objekt, welches zurückgegeben wird.<br>
	 * Für Implementierungsdetails, siehe {@link #jsonToObject(Object object)}.
	 * @param content Der JSON-String, aus dem ein Objekt berechnet werden soll
	 * @return Das aus dem angegebenen JSON-String berechnete Objekt
	 * @throws JsonParseException Wird geworfen, falls der JSON-String nicht geparst werden
	 * konnte.
	 * @throws DeserialiseException Wird geworfen, falls ein Fehler beim Deserialisieren
	 * des Objektes aufgetreten ist.
	 */
	public Object deserialiseObject(String content) throws JsonParseException, DeserialiseException {
		hashTable = new HashMap<>();
		wholeSingleJson = new JSONObject(content);
		multiFile = false;

		return jsonToObject(wholeSingleJson);
	}

	/**
	 * Berechnet aus den angegebenen JSON-Strings, die zum Dateinamen in einer Map zugeordnet sind ein
	 * Objekt, welches zurückgegeben wird.<br>
	 * Für Implementierungsdetails, siehe {@link #jsonToObject(Object object)}.
	 * @param content Die JSON-Strings, die zum Dateinamen in einer Map geordnet sind, aus denen ein
	 *                Objekt berechnet werden soll
	 * @return Das aus den angegebenen JSON-Strings berechnete Objekt
	 * @throws JsonParseException Wird geworfen, falls einer der JSON-Strings nicht geparst werden konnte.
	 * @throws DeserialiseException Wird geworfen, falls ein Fehler beim Deserialisieren des Objektes
	 * aufgetreten ist.
	 */
	public Object deserialiseSeparatedObject(Map<String, String> content)
			throws JsonParseException, DeserialiseException {
		hashTable = new HashMap<>();
		wholeSeperatedJson = new HashMap<>();
		for (String key : content.keySet()) {
			wholeSeperatedJson.put(key, new JSONObject(content.get(key)));
		}
		multiFile = true;

		for (String key : wholeSeperatedJson.keySet()) {
			if (wholeSeperatedJson.get(key).containsKey("Main")) {
				return jsonToObject(wholeSeperatedJson.get(key).get("Main"));
			}
		}
		throw new DeserialiseException("The main object could not be found.");
	}

	/**
	 * Konvertiert rekursiv ein angegebenes für JSON benutzbares Objekt zu einem Objekt:<br>
	 * Falls das angegebene Objekt null oder {@link #isSimpleType(Class) simpel} und kein Verweis ist,
	 * wird es zurückgegeben.<br>
	 * Falls das angegebene Objekt ein Verweis ist, wird das Objekt, auf das verwiesen wird berechnet
	 * und zurückgegeben.<br>
	 * Falls das angegebene Objekt ein kodiertes Array oder eine kodierte Collection ist, wird diese
	 * Methode für jedes Element aufgerufen, die resultierenden Objekte in ein erstelltes Array /
	 * eine erstellte Collection des ursprünglichen Typen gesetzt und dies zurückgegeben.<br>
	 * Falls das angegebene Objekt eine kodierte Map ist, wird diese Methode für jeden key und
	 * dazugehörigen value aufgerufen, die resultierenden Objekte passend in eine erstellte Map des
	 * ursprünglichen Typen gesetzt und diese zurückgegeben.<br>
	 * Ansonsten wird für jeden kodierten Feldinhalt dieses Objektes diese Methode aufgerufen und die
	 * resultierenden Objekte in ein passendes erstelltes Objekt gesetzt und dies zurückgegeben.
	 * @param json Das für JSON benutzbare Objekt, welches in ein Objekt umgewandelt werden soll
	 * @return Das aus dem angegebenen für JSON benutzbare Objekt berechnete Objekt
	 * @throws DeserialiseException Wird geworfen, falls ein Fehler beim Deserialisieren des Objektes
	 * auftritt.
	 */
	private Object jsonToObject(Object json) throws DeserialiseException {
		if (json == null) {
			return null;
		}
		if (isSimpleType(json.getClass())) {
			if (String.class == json.getClass()) {
				String string = (String) json;
				if (string.isEmpty()) {
					return string;
				}
				if (string.charAt(0) == '@') {
					if (multiFile && string.contains("#")) {
						String[] linkInfo = string.substring(1).split("#");
						String fileName = linkInfo[0];
						int hash = Integer.parseInt(linkInfo[1]);
						Object linkedObject = getLinkedFileObject(fileName, hash);
						if (linkedObject == null) {
							throw new DeserialiseException("The definition of the linked object " +
									string + " could not be found.");
						}
						return linkedObject;
					}
					int hash = Integer.parseInt(string.substring(1));
					Object linkedObject = getLinkedObject(multiFile ? wholeSeperatedJson : wholeSingleJson, hash);
					if (linkedObject == null) {
						throw new DeserialiseException("The definition of the linked object " +
								string + " could not be found.");
					}
					return linkedObject;
				}
				if (string.charAt(0) == '#') {
					String[] classInfos = string.split("=");
					String className = classInfos[0].substring(1);
					String classValue = classInfos[1];
					if (SpecialClasses.getClassMap().containsKey(className)) {
						SpecialClasses specialClass = SpecialClasses.getClassMap().get(className);
						Object result = specialClass.deserialise(classValue);
						if (result == null) {
							throw new DeserialiseException("Instance " + classValue + " of class "
									+ className + " could not be deserialised.");
						}
						return result;
					}
					throw new DeserialiseException("The definition of the special class " +
							className + " could not be found.");
				}
				if (string.matches("(\\\\)+[@#].*")) {
					return string.substring(1);
				}
				return string;
			}
			return json;
		}
		if (json instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray) json;
			String[] classInfos = jsonArray.getString(0).split("=");
			String className = classInfos[0];
			int hash = Integer.parseInt(classInfos[1]);

			if (hashTable.containsKey(hash)) {
				return hashTable.get(hash);
			}

			Class<?> newClass;
			try {
				newClass = Class.forName(className);
			} catch (ClassNotFoundException e) {
				throw new DeserialiseException("The specified class " + className + " could not be found.", e);
			}
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
			if (autoConvertCollections) {
				try {
					newObject = getNewInstance(newClass);
				} catch (DeserialiseException e) {
					newObject = getNewAlternativeInstance(newClass, e);
					e.printStackTrace();
					System.err.println("WARNING: Class " + newClass +
							" has been converted to class " + newObject.getClass() +
							".\nThis might lead to a ClassCastException!\nIt is recommended not to use " +
							"this Collection or Map type (" + newClass + ")!");
				}
			} else {
				newObject = getNewInstance(newClass);
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
					Object chilValue = jsonToObject(jsonArrayChild.get("value"));
					newMap.put(childKey, chilValue);
				}
				return newMap;
			}
		}
		JSONObject jsonObject = (JSONObject) json;

		String[] classInfos = jsonObject.getString("class").split("=");
		String className = classInfos[0];
		int hash = Integer.parseInt(classInfos[1]);

		if (hashTable.containsKey(hash)) {
			return hashTable.get(hash);
		}

		Class<?> newClass;
		try {
			newClass = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new DeserialiseException("The specified class " + className + " could not be found.", e);
		}
		Object newObject = getNewInstance(newClass);
		hashTable.put(hash, newObject);

		while (newClass != null) {
			for (Field field : newClass.getDeclaredFields()) {
				field.setAccessible(true);
				if (field.isAnnotationPresent(SerialiseIgnore.class)) {
					continue;
				}
				Object childJson = jsonObject.get(field.getName());
				try {
					field.set(newObject, jsonToObject(childJson));
				} catch (IllegalArgumentException e) {
					throw new DeserialiseException("Type Mismatch: " +
							"Field type of field " + field.getName() + " in class" + field.getDeclaringClass() +
							" (" + field.getType() + ") is not equal to JSON-Element child type.", e);
				} catch (IllegalAccessException e) {
					throw new DeserialiseException("Illegal Access: " +
							"Field " + field.getName() + " in class " + field.getDeclaringClass() +
							" could not be overwritten.", e);
				}
			}
			newClass = newClass.getSuperclass();
		}

		return newObject;
	}

	/**
	 * Gibt ein Objekt mit dem angegebenen Hash zurück, falls es im angegebenen für Json benutzbaren
	 * Objekt enthalten ist:<br>
	 * Falls das angegebene Objekt null oder {@link #isSimpleType(Class) simpel} ist, wird null zurückgegeben.<br>
	 * Falls das angegebene Objekt eine Map ist und nicht das Objekt mit dem gesuchten Hash ist, wird diese
	 * Methode für jeden key und jeden value der Map aufgerufen und null zurückgegeben, falls kein Aufruf das
	 * Objekt findet.<br>
	 * Falls das angegebene Objekt ein Array oder Iterable ist und nicht das Objekt mit dem gesuchten Hash
	 * ist, wird diese Methode für jedes Unterobjekt aufgerufen und null zurückgegeben, falls kein Aufruf das
	 * Objekt findet.<br>
	 * Falls das angegebene Objekt die globale Json Map ist, wird diese Methode für jedes Objekt in jeder
	 * Datei dieser Map aufgerufen und null zurückgegeben, falls kein Aufruf das Objekt findet.<br>
	 * Ansonsten wird diese Methode für jedes Feld des Objektes aufgerufen und null zurückgegeben, fassl kein
	 * Aufruf das Objekt findet.
	 * @param json Das für Json benutzbare Objekt, welches nach dem Objekt mit dem angegebenen Hash durchsucht
	 *             werden soll
	 * @param searchedHash Der angegebene Hash, dessen dazugehöriges Objekt zurückgegeben werden soll
	 * @return Das Objekt mit dem angegebenen Hash, falls es im angegebenen für Json benutzbarem Objekt enthalten
	 * ist, ansonsten null
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
			JSONArray jsonArray = (JSONArray) json;
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
				throw new DeserialiseException("The specified class " + className + " could not be found.", e);
			}

			if (!newClass.isArray()) {
				Object newObject = getNewInstance(newClass);
				if (newObject instanceof Map) {
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
			}
			for (Object jsonArrayPart : jsonArray.skipFirst()) {
				Object arrayPart = getLinkedObject(jsonArrayPart, searchedHash);
				if (arrayPart != null) {
					return arrayPart;
				}
			}
			return null;
		}
		if (json instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, JSONObject> jsonMap = (Map<String, JSONObject>) json;
			for (JSONObject value : jsonMap.values()) {
				for (Object object : value.values()) {
					Object linkedObject = getLinkedObject(object, searchedHash);
					if (linkedObject != null) {
						return linkedObject;
					}
				}
			}
			return null;
		}
		JSONObject jsonObject = (JSONObject) json;
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

	/**
	 * Gibt das Objekt mit dem angegebenen Hash in der angegebenen Datei zurück, falls es dort gespeichert
	 * wurde, ansonsten null.
	 * @param fileName Der Name der Datei, in der gesucht werden soll
	 * @param searchedHash Der Hash des Objektes, das zurückgegeben werden soll
	 * @return Das Objekt mit dem angegebenen Hash in der angegebenen Datek, falls es dort gespeichert
	 * wurde, ansonsten null
	 * @throws DeserialiseException Wird geworfen, falls ein Fehler aufgetreten ist, während das verlinkte
	 * Objekt gesucht wurde
	 */
	private Object getLinkedFileObject(String fileName, int searchedHash) throws DeserialiseException {
		JSONObject value = wholeSeperatedJson.get(fileName);
		if (value.containsKey(String.valueOf(searchedHash))) {
			return jsonToObject(value.get(String.valueOf(searchedHash)));
		}
		return null;
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
			throw new DeserialiseException("No default constructor found for class " +
					objectClass.getName() + ".", e);
		}
		constructor.setAccessible(true);
		try {
			return constructor.newInstance();
		} catch (InstantiationException e) {
			throw new DeserialiseException("Class " + objectClass.getName() + " could not be instantiated.", e);
		} catch (IllegalAccessException e) {
			throw new DeserialiseException("Illegal access: Class " +
					objectClass.getName() + " could not be instantiated.", e);
		} catch (InvocationTargetException e) {
			throw new DeserialiseException("The default constructor of class " +
					objectClass.getName() + " threw an exception.", e);
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
		throw new DeserialiseException("Class " + objectClass + " could not be instantiated and no alternative " +
				"class could be found.", cause);
	}

	/**
	 * Hilfsmethode:<br>
	 * Gibt den Namen der Datei zurück, in die eine Instanz der angegebenen Klasse gespeichert werden soll
	 * @param fieldClass Die angegebene Klasse
	 * @return Der Name der Datei, in die eine Instanz der angegebenen Klasse gespeichert werden soll
	 */
	private String getFileName(Class<?> fieldClass) {
		String fileName = "Standard";
		if (fieldClass.isAnnotationPresent(SerialiseFile.class)) {
			fileName = fieldClass.getDeclaredAnnotation(SerialiseFile.class).value();
		}
		return fileName;
	}
}
