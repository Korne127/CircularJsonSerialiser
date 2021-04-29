package de.korne127.circularJsonSerialiser.serialiser;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.korne127.circularJsonSerialiser.annotations.BeforeSerialise;
import de.korne127.circularJsonSerialiser.exceptions.SerialiseException;
import de.korne127.circularJsonSerialiser.json.JSONArray;
import de.korne127.circularJsonSerialiser.json.JSONObject;

/**
 * Die SerialiseProcess-Klasse:<br>
 * Diese Klasse wird intern von dem {@link Serialiser} benutzt, um Java-Objekte zu serialisieren.
 * @author Korne127
 */
class SerialiseProcess {

	//Für Serialisierung allgemein
	private final Map<Integer, Object> hashTable;
	private final Map<String, JSONObject> wholeSeparatedJson;
	private final boolean multiFile;

	//Für BeforeSerialise-Annotations
	private final Set<Integer> beforeSerialised;

	//Für Fehlermeldungen
	private final PathInformation fieldInformation;

	//Konfiguration
	private final boolean startSerialisingInSuperclass;
	private final Map<String, Object> methodParameters;
	private final Set<String> ignoreExceptionIDs;

	/**
	 * Der Konstruktor der SerialiseProcess-Klasse:<br>
	 * Er erstellt einen neuen Serialisierungsprozess. Dabei werden die für die Serialisierung benötigten
	 * Hilfsvariablen auf deren Startwerte gesetzt und die angegebene Konfiguration für die Serialisierung
	 * gespeichert.
	 * @param multiFile Die Einstellung, ob eine separate Serialisierung in verschiedene Dateien stattfinden soll
	 * @param startSerialisingInSuperclass Die Einstellung, ob zunächst Variablen aus den Oberklassen serialisiert
	 *                                     werden sollen
	 * @param methodParameters Die Map, die ParameterIDs auf die zugehörigen Objekte abbildet
	 * @param ignoreExceptionIDs Das Set, das die ignoreExceptionIDs beinhaltet
	 */
	SerialiseProcess(boolean multiFile, boolean startSerialisingInSuperclass,
							Map<String, Object> methodParameters, Set<String> ignoreExceptionIDs) {
		hashTable = new HashMap<>();
		fieldInformation = new PathInformation();
		beforeSerialised = new HashSet<>();
		wholeSeparatedJson = new HashMap<>();

		this.multiFile = multiFile;

		this.startSerialisingInSuperclass = startSerialisingInSuperclass;
		this.methodParameters = methodParameters;
		this.ignoreExceptionIDs = ignoreExceptionIDs;
	}

	/**
	 * Startet den Serialisierungsprozess für ein angegebenes Objekt in die angegebene Datei.<br>
	 * Dabei werden alle mit {@link BeforeSerialise} annotierten Methoden ausgeführt sowie das
	 * angegebene Objekt mit den eingestellten Konfigurationen serialisiert und zurückgegeben.
	 * @param object Das angegebene Objekt, das in einen String serialisiert werden soll
	 * @param mainFileName Der Name der Datei, in die (beim Serialisieren in verschiedene Dateien) das
	 *                     Hauptobjekt serialisiert werden soll
	 * @return Ein JSON-Element, in das das angegebene Objekt serialisiert wurde
	 * @throws SerialiseException Wird geworfen, falls ein Fehler beim Serialisieren des Objektes auftritt.
	 */
	Object serialise(Object object, String mainFileName) throws SerialiseException {
		executeBeforeSerialiseMethods(object, new HashMap<>());
		return objectToJson(object, mainFileName);
	}

	/**
	 * Gibt alle JSON-Dateien zurück, die bei eine separaten Serialisierung in verschiedene Dateien zusätzlich
	 * generiert werden
	 * @return Alle JSON-Dateien, die bei einer separaten Serialisierung in verschiedene Dateien zusätzlich
	 * generiert werden
	 */
	Map<String, JSONObject> getWholeSeparatedJson() {
		return wholeSeparatedJson;
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
		for (Map.Entry<?, ?> objectMapEntry : objectMap.entrySet()) {
			Object objectMapKey = objectMapEntry.getKey();
			Object objectMapValue = objectMapEntry.getValue();
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
		String result;
		try {
			result = specialClass.serialise(object);
		} catch (Exception e) {
			throw new SerialiseException("The special class " + fieldInformation.toString() +
					" could not be serialised.", e);
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

}
