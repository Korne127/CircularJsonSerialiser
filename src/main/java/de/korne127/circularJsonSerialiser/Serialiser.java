package de.korne127.circularJsonSerialiser;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import de.korne127.circularJsonSerialiser.annotations.SerialiseIgnore;
import org.json.JSONArray;
import org.json.JSONObject;

public class Serialiser {

	private Map<Integer, Object> hashTable;

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

	private Object objectToJson(Object object) throws IllegalAccessException {
		if (object == null) {
			return null;
		}
		Class<?> objectClass = object.getClass();
		if (isSimpleType(objectClass)) {
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

	private boolean isSimpleType(Class<?> objectClass) {
		return (objectClass == Integer.class || objectClass == Byte.class || objectClass == Short.class ||
				objectClass == Long.class || objectClass == Float.class || objectClass == Double.class ||
				objectClass == Character.class || objectClass == Boolean.class || objectClass == String.class);
	}

	private Object[] convertPrimitiveToArray(Object val) {
		int length = Array.getLength(val);
		Object[] outputArray = new Object[length];
		for(int i = 0; i < length; i++){
			outputArray[i] = Array.get(val, i);
		}
		return outputArray;
	}

	public Object deserialiseObject(String content) {

		return null;
	}

}
