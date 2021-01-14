package de.korne127.circularJsonSerialiser;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import de.korne127.circularJsonSerialiser.annotations.SerialiseIgnore;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Serialiser-Klasse<br>
 * Diese Klasse stellt einen Serialiser bereit, der ein angegebenes Objekt in ein JSON-Objekt umwandelt und
 * als String zurückgibt.
 * @author Korne127
 */
public class Serialiser {

	private Map<Integer, Object> hashTable;

	private JSONObject wholeJson;


	/**
	 * Konvertiert das angegebene Objekt zu einem JSON-Objekt, welches als String zurückgegeben wird<br>
	 * Für Implementierungsdetails, siehe {@link #objectToJson(Object object)}
	 * @param object Das Objekt, welches zu einem JSON-Objekt konvertiert werden soll
	 * @return Das aus dem angegebenen Objekt kodierte JSON-Objekt
	 */
	public String serialiseObject(Object object) {
		hashTable = new HashMap<>();
		try {
			return ((JSONObject) objectToJson(object)).toString(4);
		} catch (IllegalAccessException e) {
			System.err.println("Fatal Error: Object could not be serialised.");
			e.printStackTrace();
			throw new IllegalStateException("Fatal Error: Object could not be serialised.");
		}
	}

	/**
	 * Konvertiert rekursiv ein angegebenes Objekt zu einem für JSON benutzbares Objekt<br>
	 * Falls das angegebene Objekt null oder {@link #isSimpleType(Class) simpel} ist, wird es zurückgegeben.<br>
	 * Falls das angegebene Objekt bereits an einer anderen Stelle gespeichert wird, wird nur ein Verweis auf
	 * diese Stelle gespeichert und zurückgegeben.<br>
	 * Falls das angegebene Objekt ein Array oder eine Collection ist, wird die Methode für alle Elemente aufgerufen
	 * und zusammen in einem JSON-Array zurückgegeben.<br>
	 * Falls das angegebene Objekt eine Map ist, wird die Methode für alle Keys und dazugehörigen Values aufgerufen,
	 * je ein Key und der dazugehörige value in einem JSON-Objekt gespeichert und alle JSON-Objekte in einem
	 * JSON-Array zurückgegeben.<br>
	 * Ansonsten werden alle Felder des Objektes (und aller Oberklassen) durchgegangen und aufgerufen und in
	 * einem JSON-Objekt gespeichert, welches dann zurückgegeben wird.
	 * @param object Das Objekt, welches zu einem für JSON benutzbaren Objekt umgewandelt werden soll
	 * @return Das aus dem angegebenen Objekt kodierte für JSON benutzbare Objekt
	 * @throws IllegalAccessException Wird geworfen, falls auf ein Element zugegriffen wird, auf das kein Zugriff
	 * besteht. Dies sollte im korrekten Ablauf nicht passieren.
	 */
	private Object objectToJson(Object object) throws IllegalAccessException {
		if (object == null) {
			return null;
		}
		Class<?> objectClass = object.getClass();
		if (isSimpleType(objectClass)) {
			if (objectClass == String.class) {
				String string = (String) object;
				if (string.matches("(\\\\)*@.*")) {
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
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("class", objectClass.getName() + "=" + objectHash);
		while (objectClass != null) {
			for (Field objectField : objectClass.getDeclaredFields()) {
				objectField.setAccessible(true);
				if (objectField.isAnnotationPresent(SerialiseIgnore.class)) {
					continue;
				}
				Object child = objectField.get(object);
				jsonObject.put(objectField.getName(), objectToJson(child));
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
	private boolean isSimpleType(Class<?> objectClass) {
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

	public Object deserialiseObject(String content) {
		hashTable = new HashMap<>();
		wholeJson = new JSONObject(content);

		try {
			return jsonToObject(wholeJson);
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
				InvocationTargetException | NoSuchMethodException e) {
			System.err.println("Fatal Error: Object could not be serialised");
			e.printStackTrace();
			throw new IllegalStateException("Fatal Error: Object could not be serialised.");
		}
	}

	private Object jsonToObject(Object json) throws ClassNotFoundException, IllegalAccessException,
			InstantiationException, InvocationTargetException, NoSuchMethodException {
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
					int hash = Integer.parseInt(string.substring(1));
					Object linkedObject = getLinkedObject(wholeJson, hash);
					if (linkedObject == null) {
						throw new IllegalStateException("Linked Object: " + string + " could not be found");
					}
					return linkedObject;
				}
				if (string.matches("(\\\\)+@.*")) {
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

			Class<?> newClass = Class.forName(className);
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
			Object newObject = getNewInstance(newClass);
			if (newObject instanceof Collection) {
				@SuppressWarnings("unchecked")
				Collection<Object> newCollection = (Collection<Object>) newObject;
				hashTable.put(hash, newCollection);
				for (Object jsonArrayPart : jsonArray) {
					if (jsonArrayPart == jsonArray.get(0)) continue; //TODO besser
					newCollection.add(jsonToObject(jsonArrayPart));
				}
				return newCollection;
			}
			if (newObject instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<Object, Object> newMap = (Map<Object, Object>) newObject;
				hashTable.put(hash, newMap);
				for (Object jsonArrayPart : jsonArray) {
					if (jsonArrayPart == jsonArray.get(0)) continue; //TODO besser
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

		Class<?> newClass = Class.forName(className);
		Object newObject = getNewInstance(newClass);
		hashTable.put(hash, newObject);

		while (newClass != null) {
			for (Field field : newClass.getDeclaredFields()) {
				field.setAccessible(true);
				if (field.isAnnotationPresent(SerialiseIgnore.class)) {
					continue;
				}
				Object childJson = jsonObject.get(field.getName());
				field.set(newObject, jsonToObject(childJson));
			}
			newClass = newClass.getSuperclass();
		}

		return newObject;
	}

	private Object getLinkedObject(Object json, int searchedHash) throws IllegalAccessException,
			InstantiationException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException {
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
			Class<?> newClass = Class.forName(className);

			if (!newClass.isArray()) {
				Object newObject = getNewInstance(newClass);
				if (newObject instanceof Map) {
					for (Object jsonArrayPart : jsonArray) {
						if (jsonArrayPart == jsonArray.get(0)) continue; //TODO besser machen
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
			for (Object jsonArrayPart : jsonArray) {
				if (jsonArrayPart == jsonArray.get(0)) continue; //TODO besser machen
				Object arrayPart = getLinkedObject(jsonArrayPart, searchedHash);
				if (arrayPart != null) {
					return arrayPart;
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

	private Object getNewInstance(Class<?> objectClass) throws IllegalAccessException,
			InvocationTargetException, InstantiationException, NoSuchMethodException {
		Constructor<?> constructor = objectClass.getDeclaredConstructor();
		constructor.setAccessible(true);
		return constructor.newInstance();
	}
}
