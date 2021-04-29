package de.korne127.circularJsonSerialiser.serialiser;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.korne127.circularJsonSerialiser.annotations.AfterDeserialise;
import de.korne127.circularJsonSerialiser.annotations.IgnoreCasting;
import de.korne127.circularJsonSerialiser.annotations.Setter;
import de.korne127.circularJsonSerialiser.exceptions.DeserialiseException;
import de.korne127.circularJsonSerialiser.json.JSONArray;
import de.korne127.circularJsonSerialiser.json.JSONElement;
import de.korne127.circularJsonSerialiser.json.JSONObject;

/**
 * Die DeserialiseProcess-Klasse:<br>
 * Diese Klasse wird intern von dem {@link Serialiser} benutzt, um JSON-Elemente zu deserialisieren.
 * @author Korne127
 */
class DeserialiseProcess {

	//Für Deserialisierung allgemein
	private final Map<Integer, Object> hashTable;
	private Map<String, JSONObject> wholeSeparatedJson;
	private JSONElement wholeSingleJson;
	private final boolean multiFile;

	//Für IgnoreCasting Annotations
	private final Deque<Boolean> ignoreCasting;

	//Für Fehlermeldungen
	private final PathInformation fieldInformation;

	//Konfiguration
	private final boolean startSerialisingInSuperclass;
	private final Map<String, Object> methodParameters;
	private final Set<String> ignoreExceptionIDs;
	private final Set<String> ignoreSetterIDs;
	private final Serialiser.CollectionHandling collectionHandling;
	private final Serialiser.NewVariableHandling newVariableHandling;

	/**
	 * Der Konstruktor der DeserialiseProcess-Klasse:<br>
	 * Er erstellt einen neuen Deserialisierungsprozess. Dabei werden die für die Deserialisierung benötigten
	 * Hilfsvariablen auf deren Startwerte gesetzt und die angegebene Konfiguration für die Deserialisierung
	 * gespeichert.
	 * @param multiFile Die Einstellung, ob eine separate Deserialisierung aus verschiedene Dateien stattfinden soll
	 * @param startSerialisingInSuperclass Die Einstellung, ob zunächst Variablen aus den Oberklassen serialisiert
	 *                                     werden sollen
	 * @param methodParameters Die Map, die ParameterIDs auf die zugehörigen Objekte abbildet
	 * @param ignoreExceptionIDs Das Set, das die ignoreExceptionIDs beinhaltet
	 * @param ignoreSetterIDs Das Set, das die ignoreSetterIDs beinhaltet
	 * @param collectionHandling Der
	 * {@link de.korne127.circularJsonSerialiser.serialiser.Serialiser.CollectionHandling}-Konfigurationswert
	 * @param newVariableHandling Der
	 * {@link de.korne127.circularJsonSerialiser.serialiser.Serialiser.NewVariableHandling}-Konfigurationswert
	 */
	DeserialiseProcess(boolean multiFile, boolean startSerialisingInSuperclass,
							  Map<String, Object> methodParameters, Set<String> ignoreExceptionIDs,
							  Set<String> ignoreSetterIDs, Serialiser.CollectionHandling collectionHandling,
							  Serialiser.NewVariableHandling newVariableHandling) {
		hashTable = new LinkedHashMap<>();
		fieldInformation = new PathInformation();
		ignoreCasting = new ArrayDeque<>();
		ignoreCasting.push(false);

		this.multiFile = multiFile;

		this.startSerialisingInSuperclass = startSerialisingInSuperclass;
		this.methodParameters = methodParameters;
		this.ignoreExceptionIDs = ignoreExceptionIDs;
		this.ignoreSetterIDs = ignoreSetterIDs;
		this.collectionHandling = collectionHandling;
		this.newVariableHandling = newVariableHandling;
	}

	/**
	 * Started den Deserialisierungsprozess für ein angegebenes Hauptobjekt mit den angegebenen kompletten
	 * Json-Dateien.<br>
	 * Dabei wird das angegebene Objekt mit den eingestellten Konfigurationen deserialisiert sowie alle
	 * mit {@link AfterDeserialise} annotierte Methoden ausgeführt.
	 * @param json Das angegebene JSON-Element, das in ein Java-Objekt deserialisiert werden soll
	 * @param wholeSeparatedJson Die kompletten Json-Dateien, die beim Deserialisieren aus verschiedenen Dateien
	 *                           eventuell zusätzliche serialisierte Objekte enthalten
	 * @return Ein Java-Objekt, in das das angegebene JSON-Element eventuell mit den angegebene JSON-Dateien
	 * deserialisiert wurde
	 * @throws DeserialiseException Wird geworfen, falls ein Fehler beim Deserialisieren
	 * des Objektes aufgetreten ist.
	 */
	Object deserialise(Object json, Map<String, JSONObject> wholeSeparatedJson) throws DeserialiseException {
		if (!multiFile) {
			if (json instanceof JSONElement) {
				wholeSingleJson = (JSONElement) json;
			}
		} else {
			this.wholeSeparatedJson = wholeSeparatedJson;
		}

		Object result = jsonToObject(json);
		executeAfterDeserialiseMethods();
		return result;
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
			Object result;
			try {
				result = specialClass.deserialise(classValue);
			} catch (Exception e) {
				throw new DeserialiseException(fieldInformation.toString() + ", value: \"" + classValue +
						"\" could not be deserialised.", e);
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
	 * möglichst ähnlicher} Collection- bzw. Map-Typ benutzt und abhängig von der {@link Serialiser.CollectionHandling
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
		if (collectionHandling == Serialiser.CollectionHandling.NO_CASTING) {
			newObject = SerialiseUtils.getNewInstance(newClass, fieldInformation);
		} else {
			try {
				newObject = SerialiseUtils.getNewInstance(newClass, fieldInformation);
			} catch (DeserialiseException e) {
				newObject = SerialiseUtils.getNewAlternativeInstance(newClass, e, fieldInformation);
				if (collectionHandling == Serialiser.CollectionHandling.DEBUG_MODE) {
					e.printStackTrace();
				}
				if (collectionHandling != Serialiser.CollectionHandling.NO_WARNING && !ignoreCasting.getLast() ||
						collectionHandling == Serialiser.CollectionHandling.DEBUG_MODE) {
					System.out.println("WARNING: " + fieldInformation.toString() +
							" has been converted to class " + newObject.getClass().getName() +
							". This might lead to a ClassCastException.\nIt is recommended not to use " +
							"this Collection or Map type (" + newClass.getName() + ")!");
				}
				if (collectionHandling == Serialiser.CollectionHandling.CONVERT_WITH_WARNING &&
						!ignoreCasting.getLast()) {
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
