package de.korne127.circularJsonSerialiser.serialiser;

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
import java.util.Deque;
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
import de.korne127.circularJsonSerialiser.annotations.Setter;
import de.korne127.circularJsonSerialiser.exceptions.DeserialiseException;

/**
 * Die statische SerialiseUtils-Klasse:<br>
 * Diese Klasse stellt verschiedene statische Methoden zur Verfügung, die von dem Serialiser genutzt werden.
 * @author Korne127
 */
public final class SerialiseUtils {

	/**
	 * Ein privater Konstruktor der Klasse, um sicherzustellen, dass keine Instanzen der statischen
	 * Klasse erstellt werden.
	 */
	private SerialiseUtils() {}

	/**
	 * Gibt eine neue Instanz einer angegebenen Klasse zurück.<br>
	 * Dafür wird der Standardkonstruktor der Klasse ausgeführt und die neue Instanz zurückgegeben.
	 * @param objectClass Die angegebene Klasse, von der eine neue Instanz zurückgegeben werden soll
	 * @param fieldInformation Die Informationen zum aktuellen Feld, die im Falle einer Fehlermeldung
	 *                         benötigt werden
	 * @return Die neue Instanz der angegebenen Klasse
	 * @throws DeserialiseException Wird geworfen, falls die neue Instanz der Klasse nicht instanziiert
	 * werden konnte.
	 */
	static Object getNewInstance(Class<?> objectClass, PathInformation fieldInformation)
			throws DeserialiseException {
		Constructor<?> constructor;
		try {
			constructor = objectClass.getDeclaredConstructor();
		} catch (NoSuchMethodException e) {
			throw new DeserialiseException("No default constructor found for class " + objectClass.getName() +
					", used by " + fieldInformation.toString() + ".", e);
		}
		constructor.setAccessible(true);
		try {
			return constructor.newInstance();
		} catch (InstantiationException e) {
			throw new DeserialiseException("Class " + objectClass.getName() + ", used by " +
					fieldInformation.toString() + " could not be instantiated.", e);
		} catch (IllegalAccessException e) {
			throw new DeserialiseException("Illegal access: Class " + objectClass.getName() + ", used by " +
					fieldInformation.toString() + " could not be instantiated.", e);
		} catch (InvocationTargetException e) {
			throw new DeserialiseException("The default constructor of class " + objectClass.getName() +
					", used by " + fieldInformation.toString() + " threw an exception.", e);
		}
	}

	/**
	 * Sucht nach einer alternativen Klasse, falls keine Instanz der eigentlichen Collection / Map erstellt
	 * werden konnte.
	 * @param objectClass Die eigentliche Klasse, von der keine Instanz erstellt werden konnte
	 * @param cause Die Exception, die geworfen wurde, als versucht wurde, die eigentliche Klasse zu erstellen
	 * @param fieldInformation Die Informationen zum aktuellen Feld, die im Falle einer Fehlermeldung
	 *                         benötigt werden
	 * @return Eine Instanz aus einer Alternativklasse
	 * @throws DeserialiseException Wird geworfen, wenn keine Alternativklasse gefunden werden konnte.
	 */
	static Object getNewAlternativeInstance(Class<?> objectClass, Exception cause, PathInformation fieldInformation)
			throws DeserialiseException {
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
		throw new DeserialiseException("Class " + objectClass + ", used by " + fieldInformation.toString() +
				" could not be instantiated and no alternative class could be found.", cause);
	}

	/**
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
	static List<Field> getSerialiseFields(Class<?> objectClass, boolean startAtTheTop,
												  boolean onlySetterFields, Set<String> exceptionIDs) {
		List<Field> fieldList = new LinkedList<>();
		LinkedList<Class<?>> classList = getAllSuperclasses(objectClass, startAtTheTop);
		while (!classList.isEmpty()) {
			objectClass = classList.pop();
			for (Field field : objectClass.getDeclaredFields()) {
				field.setAccessible(true);
				if (!isStaticFinal(field) &&
						(!onlySetterFields && (!field.isAnnotationPresent(SerialiseIgnore.class) ||
								exceptionIDs.contains(field.getAnnotation(SerialiseIgnore.class).value())) ||
								onlySetterFields && field.isAnnotationPresent(Setter.class))) {
					fieldList.add(field);
				}
			}
		}
		return fieldList;
	}

	/**
	 * Gibt alle Methoden aus der angegebenen Klasse und aller Oberklassen, die mit der bestimmten angegebenen
	 * Annotation annotiert sind, in einer Liste zurück.
	 * @param objectClass Die angegebene Klasse, aus der und deren Oberklassen alle Methoden mit der bestimmten
	 *                    Annotation zurückgegeben werden sollen
	 * @param annotation Die bestimmte Annotation
	 * @return Eine Liste mit allen Methoden aus der angegebenen Klasse und aller Oberklassen, die mit der
	 * bestimmten angegebenen Annotation annotiert sind
	 */
	static List<Method> getMethodsWithAnnotation(Class<?> objectClass,
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
	static LinkedList<Class<?>> getAllSuperclasses(Class<?> objectClass, boolean startAtTheTop) {
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
	 * Konvertiert ein Array eines primitiven Datentyps (z.B. int) in ein Array des Wrapper-Objektes dieses
	 * Datentyps (z.B. Integer).
	 * @param val Das Array eines primitiven Datentyps, welches konvertiert werden soll
	 * @return Das konvertierte Array eines Wrapper-Objektes
	 */
	static Object[] convertPrimitiveToArray(Object val) {
		int length = Array.getLength(val);
		Object[] outputArray = new Object[length];
		for(int i = 0; i < length; i++){
			outputArray[i] = Array.get(val, i);
		}
		return outputArray;
	}

	/**
	 * Gibt eine neue Instanz des angegebenen Enums mit dem angegebenen Wert zurück
	 * @param name Der Wert des Enums, der zurückgegeben werden soll
	 * @param type Die Enum-Klasse
	 * @return Eine neue Instanz des angegebenen Enums mit dem angegebenen Wert
	 */
	@SuppressWarnings("unchecked")
	static <T extends Enum<T>> T getNewEnumInstance(String name, Class<?> type) {
		return Enum.valueOf((Class<T>) type, name);
	}

	/**
	 * Gibt den Namen der Datei zurück, in die das angegebene Objekt serialisiert werden soll
	 * @param field Das Feld, in dem das angegebene Objekt gespeichert ist
	 * @param object Das angegebene Objekt
	 * @return Der Name der Datei, in die das angegebene Objekt serialisiert werden soll
	 */
	static String getFileName(Field field, Object object) {
		if (field.isAnnotationPresent(SerialiseFile.class)) {
			return field.getDeclaredAnnotation(SerialiseFile.class).value();
		}
		return getFileName(object);
	}

	/**
	 * Gibt den Namen der Datei zurück, in die das angegebene Objekt serialisiert werden soll
	 * @param object Das angegebene Objekt
	 * @return Der Name der Datei, in die das angegebene Objekt serialisiert werden soll
	 */
	static String getFileName(Object object) {
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
	 * Testet, ob ein bestimmtes Feld static und final ist
	 * @param field Das Feld, welches getestet werden soll
	 * @return true - das Feld ist static und final;<br>
	 *     false - das Feld ist nicht static und final
	 */
	static boolean isStaticFinal(Field field) {
		int modifiers = field.getModifiers();
		return (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers));
	}

	/**
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
