package de.korne127.circularJsonSerialiser.serialiser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SpecialClasses-Enum:<br>
 * Dieses Enum beinhaltet Java-Klassen, die von dem Serialiser auf besondere Weise behandelt werden.
 * Instanzen dieser Klassen werden nicht wie gewöhnlich serialisiert und deserialisiert, indem beim
 * Serialisieren die Inhalte aller Felder gespeichert werden und beim Deserialisieren alle Felder
 * einer neuen Instanz der Klasse beschrieben werden.<br>
 * Dies kann entweder daran liegen, dass die Klasse auch normal (de)serialisierbar ist, aber sehr
 * große Datenstrukturen in den JSON-String schreibt, die für das Objekt nicht nötig sind, oder, dass
 * die Klasse keinen Standardkonstruktor besitzt oder aus anderen Gründen nicht normal erstellbar ist
 * und daher gesondert behandelt werden muss.
 * @author Korne127
 */
enum SpecialClasses {

	//Datumsklassen
	LOCAL_DATE_TIME(LocalDateTime.class,
			object -> {
				LocalDateTime localDateTime = (LocalDateTime) object;
				return localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
			},
			LocalDateTime::parse),
	LOCAL_DATE(LocalDate.class,
			object -> {
				LocalDate localDate = (LocalDate) object;
				return localDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
			},
			LocalDate::parse),
	LOCAL_TIME(LocalTime.class,
			object -> {
				LocalTime localTime = (LocalTime) object;
				return localTime.format(DateTimeFormatter.ISO_LOCAL_TIME);
			},
			LocalTime::parse),
	ZONED_DATE_TIME(ZonedDateTime.class,
			object -> {
				ZonedDateTime zonedDateTime = (ZonedDateTime) object;
				return zonedDateTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
			},
			ZonedDateTime::parse),


	//Alte Datumsklassen
	DATE(Date.class,
			object -> {
				Date date = (Date) object;
				return new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(date);
			},
			string ->  new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(string)),
	GREGORIAN_CALENDAR(GregorianCalendar.class,
			object -> {
				GregorianCalendar gregorianCalendar = (GregorianCalendar) object;
				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
				TimeZone timeZone = gregorianCalendar.getTimeZone();
				return dateFormat.format(gregorianCalendar.getTime()) + "|" +
						timeZone.getRawOffset() + "|" + timeZone.getID();
			},
			string -> {
				String[] calendarInfos = string.split("\\|");
				Date date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(calendarInfos[0]);
				TimeZone timezone = new SimpleTimeZone(Integer.parseInt(calendarInfos[1]), calendarInfos[2]);
				GregorianCalendar gregorianCalendar = new GregorianCalendar(timezone);
				gregorianCalendar.setTime(date);
				return gregorianCalendar;
			}),

	//Anderes
	UUID(java.util.UUID.class,
			Object::toString,
			java.util.UUID::fromString),


	//Unterklassen von Number
	ATOMIC_INTEGER(AtomicInteger.class,
			Object::toString,
			string -> new AtomicInteger(Integer.parseInt(string))),
	ATOMIC_LONG(AtomicLong.class,
			Object::toString,
			string -> new AtomicLong(Integer.parseInt(string))),
	BIG_INTEGER(BigInteger.class,
			Object::toString,
			BigInteger::new),
	BIG_DECIMAL(BigDecimal.class,
			Object::toString,
			BigDecimal::new);

	private static final Map<String, SpecialClasses> classMap = new HashMap<>();
	static {
		for (SpecialClasses specialClass : SpecialClasses.values()) {
			classMap.put(specialClass.specialClassName, specialClass);
		}
	}

	private final String specialClassName;
	private final Function<Object, String> classSerialiser;
	private final Function<String, Object> classDeserialiser;

	/**
	 * Konstruktor für das Enum:<br>
	 * Jedes Element speichert die Klasse, die gesondert behandelt werden soll, eine
	 * Serialisierungs-Funktion, die eine Instanz der Klasse in einen String umwandelt sowie eine
	 * Deserialisierungs-Funktion, die einen solchen String zurück in eine Instanz der Klasse umwandelt.
	 * @param specialClass Die Klasse, die gesondert behandelt werden soll
	 * @param classSerialiser Die Serialisierungs-Funktion, die eine Instanz der Klasse in einen String
	 *                        umwandelt
	 * @param classDeserialiser Die Deserialisierungs-Funktion, die einen String in eine Instanz der
	 *                          Klasse umwandelt
	 */
	SpecialClasses(Class<?> specialClass, Function<Object, String> classSerialiser,
				   Function<String, Object> classDeserialiser) {
		this.specialClassName = specialClass.getName();
		this.classSerialiser = classSerialiser;
		this.classDeserialiser = classDeserialiser;
	}

	/**
	 * Dieses Interface repräsentiert eine Funktion, die ein Argument entgegennimmt und ein Argument
	 * zurückgibt.<br>
	 * Es wird benutzt, um die einzelnen SpecialClasses zu (de)serialisieren, in dem für jede eine Funktion
	 * zur Serialisierung sowie eine zur Deserialisierung bereitgestellt ist.
	 * Der Unterschied zur Java-eigenen {@link java.util.function.Function} ist, dass die
	 * {@link #apply(Object)}-Methode dieser Funktion auch eine Exception werfen kann; somit kann beim Auftreten
	 * eines Fehlers beim (De)Serialisieren die Ursache festgestellt werden.
	 * @param <T> Der Typ der Eingabe der Funktion
	 * @param <R> Der Typ der Ausgabe der Funktion
	 */
	private interface Function<T, R> {
		/**
		 * Wendet die Funktion an das angegebene Argument an.
		 * @param t Das Argument der Funktion
		 * @return Das Ergebnis der Funktion
		 * @throws Exception Eine Exception, die beim Ausführen der Funktion geworfen werden kann
		 */
		R apply(T t) throws Exception;
	}

	/**
	 * Wandelt eine angegebene Instanz der Klasse in einen String um.
	 * @param object Die angegebene Instanz der Klasse
	 * @return Der String, in den die angegebene Instanz der Klasse umgewandelt wurde
	 */
	String serialise(Object object) throws Exception {
		return classSerialiser.apply(object);
	}

	/**
	 * Wandelt einen String in eine Instanz der Klasse um.
	 * @param string Der String, der Informationen über das Objekt enthält
	 * @return Die Instanz der Klasse, in das der String umgewandelt wurde
	 */
	Object deserialise(String string) throws Exception {
		return classDeserialiser.apply(string);
	}

	/**
	 * Gibt den Namen der Klasse, die besonders behandelt werden soll, zurück.
	 * @return Den Namen der Klasse, die besonders behandelt werden soll
	 */
	String getSpecialClassName() {
		return specialClassName;
	}

	/**
	 * Gibt die Map, die alle besonderen Klassen zu dem Klassennamen zugeordnet enthält, zurück.
	 * @return Die Map, die alle besonderen Klassen zu dem Klassennamen zugeordnet enthält
	 */
	static Map<String, SpecialClasses> getClassMap() {
		return classMap;
	}
}
