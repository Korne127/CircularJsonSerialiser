# CircularJsonSerialiser v1.2
## Allgemein
CircularJsonSerialiser ist ein Serialiser für Java mit dem Ziel, dass ohne manuelle Konfiguration alle Klassen-
und Objektstrukturen korrekt gespeichert und identisch wiederhergestellt werden.<br>
Alle bidirektionalen und zirkulären Verweise zwischen verschiedenen Objekten werden automatisch behandelt und korrekt
gespeichert und aufgelöst.<br>
Außerdem wird die Identität von Objekten erkannt; wenn ein Objekt zum Beispiel in zwei Listen gespeichert wird, werden
beim Deserialisieren nicht zwei verschiedene Instanzen, sondern dasselbe Objekt in beide Listen gespeichert.<br>
Auch die Art primitiver Datentypen wie z. B. Zahlen oder Datenstrukturen wie z. B. Listen wird korrekt gespeichert,
sodass nach dem Deserialisieren keine Cast Exceptions durch falsch wiederhergestellte Objekte zu erwarten sind.

## Benutzen des Serialisers
Um den CircularJsonSerialiser in ein Maven-Projekt einzubinden, muss in der pom.xml die folgende Dependency hinzugefügt
werden:

```xml
<dependency>
    <groupId>de.korne127.circularjsonserialiser</groupId>
    <artifactId>circular-json-serialiser</artifactId>
    <version>1.2</version>
</dependency>
```

Dasselbe gilt äquivalent für ein Gradle-Projekt:

```gradle
implementation 'de.korne127.circularjsonserialiser:circular-json-serialiser:1.2'
```

Falls ein anderes Build-Management-Tool verwendet wird, das ebenfalls auf das zentrale Maven-Repository zugreifen
kann, kann man
[hier](https://search.maven.org/artifact/de.korne127.circularjsonserialiser/circular-json-serialiser/1.2/jar)
den Code nachschauen, um die Dependency entsprechend hinzuzufügen.

Alternativ kann auch die .jar-Datei des neuesten Releases heruntergeladen und manuell dem Projekt in der IDE
hinzugefügt werden.

Um ein Objekt zu serialisieren, muss zunächst ein `Serialiser`-Objekt erstellt werden:
```java
Serialiser serialiser = new Serialiser();
```
Ein Objekt kann auf zwei Arten serialisiert werden:<br>
Die Standardserialisierung verwandelt das Objekt in einen JSON-String, der in einer Datei gespeichert wird:
```java
String jsonString = serialiser.serialiseObject(object);
```
Das Objekt wird wiederhergestellt, indem dieser JSON-String an die Standard-Deserialisierungsmethode gegeben wird:
```java
Object object = serialiser.deserialiseObject(jsonString);
```

Alternativ gibt es auch eine andere Art, ein Objekt zu serialisieren, nämlich die
`serialiseObjectSeparated`-Methode:
```java
Map<String, String> jsonMap = serialiser.serialiseObjectSeparated(object);
```
Diese kann benutzt werden, wenn die serialisierten Objekte in mehrere Dateien unterteilt werden sollen. Dazu können
Klassen und Attribute mit einer `SerialiseFile`-Annotation versehen werden, die den Dateinamen enthält,
zu dem die entsprechenden Objekte zugeordnet werden sollen, siehe
[Annotationen und Exceptions](#annotationen-und-exceptions).
Die einzelnen Strings werden in einer Map ihrem Dateinamen zugeordnet zurückgegeben.

In den Strings werden Verweise zwischen den einzelnen Objekten gespeichert, die beim Deserialisieren wieder
zusammengesetzt werden. Somit können mit der `deserialiseSeparatedObject`-Methode die verschiedenen JSON-Strings
wieder in ein Objekt umgewandelt werden:
```java
Object object = serialiser.deserialiseSeparatedObject(jsonMap);
```

Bei der Serialisierung eines Objektes kann eine `SerialiseException`, bei der Deserialisierung können eine
`DeserialiseException` und eine `JsonParseException` geworfen werden.<br>
Für mehr Informationen zu möglichen Auslösern, siehe [Annotationen und Exceptions](#annotationen-und-exceptions).

## Identische Wiederherstellung der Objektstrukturen
Das Ziel dieses Serialisers ist es, Objektstrukturen ohne manuelle Konfiguration so akkurat wie möglich zu
speichern und wiederherzustellen, sodass das Programm nach dem Deserialisieren identisch funktionieren
kann.

Da andere Serialiser wie Gson oder Jackson Performanz und nicht eine möglichst akkurate Speicherung der
Objektstrukturen priorisieren, hat dieser Serialiser einige zusätzliche Eigenschaften, die eine
bessere Wiederherstellung ermöglichen:

### Bidirektionale Verweise und Zirkelverweise
Objekte können sich gegenseitig referenzieren und ohne manuelle Konfiguration korrekt (de-)serialisiert
werden. Zusätzlich werden die Identitäten von Objekten gespeichert, sodass sich Strukturen beim Deserialisieren
nicht doppelt bilden und Objekte mehrfach gespeichert werden würden.

### Speicherung von eigenen Maps ohne Konfiguration
Es können eigene Klassen als Keys für Maps benutzt werden, ohne dass irgendeine Konfiguration nötig ist, wie mit
diesen Maps umgegangen werden soll. Sie werden einfach beim Serialisieren in den JSON-String gespeichert und beim
Deserialisieren wiederhergestellt.

### Automatische Speicherung und Wiederherstellung von Feldern der Oberklassen
Es werden alle Felder eines Objektes gespeichert, auch solche, die in Oberklassen der eigentlichen Klasse
gespeichert sind. Dies stellt sicher, dass sich das generierte Objekt nach dem Deserialisieren identisch zu dem
ursprünglichen Objekt verhält.

### Speicherung der konkreten Unterklasse der Instanz
In bestimmten Entwicklungsmustern wie etwa der Schablonenmethode oder dem Zustands-Muster wird nur die Instanz
einer Oberklasse in dem Code gespeichert, welche dann dynamisch durch Instanzen der verschiedenen Unterklassen
ausgetauscht werden kann.<br>
Dieser Serialiser speichert die genaue Klasse des Objektes und stellt sie wieder her, sodass die Ausführung des
Programmes nicht verändert wird.

### Chars, Enums und alle Zahlentypen werden sicher gespeichert und deserialisiert
Auch die Art der primitiven Datentypen wird, anders als bei anderen Serialisern, gespeichert. Dies führt dazu,
dass die Zahlen bzw. Buchstaben immer im korrekten Typ wiederhergestellt werden und es beim Benutzen von
selteneren Typen wie `char`, `byte` oder `short` nicht zu einer `ClassCastException` kommt.<br>
Ebenso werden Enums nicht wie in anderen Serialisern als String gespeichert, sondern explizit als Typ des
entsprechenden Enums, sodass sie in jedem Fall wieder korrekt hergestellt werden können.

### Speicherung der konkreten Collection- / Map-Klasse
Die konkrete Information, welche Collection- oder Map-Klasse benutzt wird, wird bei dem Serialisieren ebenfalls
gespeichert. So kann beim Deserialisieren eine identische Collection / Map, die sich weiterhin gleich verhält,
erstellt werden.<br>
(Falls es nicht möglich ist, eine Instanz der speziellen Collection- / Map-Klasse herzustellen, kann eine möglichst
ähnliche alternative Klasse benutzt werden.)


## Funktionen dieses Serialisers
Der Serialiser besitzt einige zusätzliche Funktionen, die dabei helfen, in verschiedenen
Anwendungsgebieten optimale Ergebnisse zu erzielen.<br>
Diese sind hier aufgelistet:

### Unterteilung der JSON-Speicherung in mehrere Dateien
Es kann für jede Klasse und jedes Attribut ein Dateiname annotiert werden, zu dem Objekte aus dieser Klass
bzw. das entsprechende Objekt des Attributes serialisiert gespeichert
werden sollen. Mit `Map<String, String> serialised = serialiser.serialiseObjectSeparated(object)` wird eine Map
zurückgegeben, die die einzelnen JSON-Strings den Dateinamen, zu denen sie gespeichert werden sollen, zugeordnet
beinhaltet. Diese Map kann an den Deserialiser übergeben werden, der daraus erfolgreich das Objekt wiederherstellt.

Dies kann hilfreich sein, falls die serialisierten Strings auch für Menschen einfach überschaubar sein sollen:
Einzelne Klassen und Objekte können in einer anderen Datei gespeichert werden, um die Übersicht zu verbessern
oder den Zugriff auf sensible Informationen zu verhindern.

### Ausführen von Aktionen vor der Serialisierung und nach der Deserialisierung
Es können Methoden mit einer BeforeSerialise-Annotation annotiert werden. Dann werden diese vor dem
Deserialisieren ausgeführt. Diese Methoden können auch ein Argument annehmen, das im Serialiser
spezifiziert wird.<br>
Es können Felder mit einer Setter-Annotation annotiert werden. Dann wird das Feld während dem
Deserialisieren auf ein im Serialiser spezifiziertes Argument überschrieben.<br>
Es können Methoden mit einer AfterDeserialise-Annotation annotiert werden. Dann werden diese nach dem
Deserialisieren ausgeführt. Diese Methoden können auch ein Argument annehmen, das im Serialiser
spezifiziert wird.

Für mehr Informationen zu den entsprechenden Annotationen und die Spezifikation von Argumenten
im Serialiser siehe [Annotationen und Exceptions](#annotationen-und-exceptions) und
[methodParameters](#methodparameters).

### Dynamische Serialisierung
Es ist möglich, dynamisch zu serialisieren, also, dass für verschiedene Fälle verschiedene Felder serialisiert
werden.<br>
Dies ist möglich, da die SerialiseIgnore- und die Setter-Annotationen IDs als Parameter besitzen können und
vor dem (De-)Serialisieren eingestellt werden kann, dass Annotationen mit bestimmten IDs ignoriert
werden.
Für mehr Informationen zu den entsprechenden Annotationen und den Einstellungen zum dynamischen
Serialisieren siehe [Annotationen und Exceptions](#annotationen-und-exceptions),
[ignoreExceptionIDs](#ignoreexceptionids) und [ignoreSetterIDs](#ignoresetterids).

## Konfiguration
Der Serialiser besitzt verschiedene Konfigurationsmöglichkeiten, die das Verhalten des Serialisers anpassen:

### startSerialisingInSuperclass
Standardmäßig werden die Variablen aus der obersten Oberklasse eines Objektes zuerst und die Variablen der
eigentlichen Klasse des Objektes zuletzt serialisiert.<br>
Dieses Verhalten kann mit `setStartSerialisingInSuperclass(boolean)` angepasst werden.

### methodParameters
Die BeforeSerialise-, AfterDeserialise- sowie Setter-Annotationen können einen Parameter besitzen. Diesen
Parametern können Objekte in einer Map zugeordnet werden.<br>
Wenn eine Methode, die eine BeforeSerialise- oder AfterDeserialise-Annotation besitzt, ein Argument entgegennimmt,
wird das zu dem Parameter der entsprechenden Annotation gespeicherte Objekt dafür benutzt.
Ebenso werden Felder, die die Setter-Annotation besitzen, mit dem Objekt überschrieben, das zu dem Parameter
der Annotation gespeichert ist.<br>
Diese Map "methodParameters" ist standardmäßig leer und kann mit `setMethodParameters(Map)` überschrieben werden.

### ignoreExceptionIDs
Falls der Parameter (die ID) einer SerialiseIgnore-Annotation in ignoreExceptionIDs enthalten ist, wird die
Annotation ignoriert, also der Parameter doch serialisiert.<br>
Dies ermöglicht eine dynamische Serialisierung für verschiedene Fälle, in denen unterschiedliche Felder
serialisiert werden sollen.<br>
Das Set "ignoreExceptionIDs" ist standardmäßig leer (es werden also standardmäßig alle Felder mit einer
SerialiseIgnore-Annotation ignoriert) und kann mit `setIgnoreExceptionIDs(Set)` überschrieben werden.

### ignoreSetterIDs
Falls der Parameter (die ID) einer Setter-Annotation in ignoreSetterIDs enthalten ist, wird die Annotation
ignoriert, der Wert des Feldes also nicht überschrieben.<br>
Dies ermöglicht eine dynamische Serialisierung für verschiedene Fälle, in denen unterschiedliche Felder
serialisiert werden sollen.<br>
Das Set "ignoreSetterIDs" ist standardmäßig leer (es werden also standardmäßig alle Felder mit einer
Setter-Annotation überschrieben) und kann mit `setIgnoreSetterIDs(Set)` überschrieben werden.

### collectionHandling
Falls beim Deserialisieren eine Collection oder Map existiert, von der keine Instanz erstellt werden kann,
bestimmt der gesetzte "collectionHandling"-Wert, wie sich der Serialiser verhält:<br>
- NO_WARNING: Falls keine Instanz einer Collection / Map erstellt werden kann, wird eine andere, möglichst
ähnliche Collection- / Map-Klasse genommen, in der die Elemente gespeichert werden.
- CONVERT_WITH_WARNING: Falls keine Instanz einer Collection / Map erstellt werden kann, wird eine andere, möglichst
ähnliche Collection- / Map-Klasse genommen, in der die Elemente gespeichert werden.<br>
Zusätzlich wird eine Warnung, welche Klasse in welche Klasse umgewandelt wurde, ausgegeben.<br>
Dies ist die standardmäßige Einstellung.
- DEBUG_MODE: Falls keine Instanz einer Collection / Map erstellt werden kann, wird eine andere, möglichst
ähnliche Collection- / Map-Klasse genommen, in der die Elemente gespeichert werden.<br>
Zusätzlich wird die komplette Exception, die gefangen wurde, sowie eine Warnung, welche Klasse in welche Klasse
umgewandelt wurde, ausgegeben.
- NO_CASTING: Falls keine Instanz einer Collection / Map erstellt werden kann, wird eine
DeserialiseException geworfen und der Programmablauf unterbrochen.

Falls das Feld direkt mit der IgnoreCasting-Annotation annotiert ist, wird das Casten für
eine Collection / Map aus dem speziellen Feld unabhängig von dem generellen collectionHandling-Wert ohne Warnung
vollzogen.

Die Einstellung collectionHandling lässt sich mit `setCollectionHandling(CollectionHandling)` überschreiben.

### newVariableHandling
Falls beim Deserialisieren eine Klasse eine "neue Variable", also eine Variable, die (de-)serialisiert
werden soll, aber nicht im angegebenen Json gespeichert wurde, besitzt, bestimmt der gesetzte
"newVariableHandling"-Wert, wie sich der Serialiser verhält:<br>
- NO_WARNING: Falls eine Klasse eine neue Variable enthält, wird diese ignoriert.
- WARNING: Falls eine Klasse eine neue Variable enthält, wird diese nicht gesetzt, aber es wird eine Warnung
mit Informationen zu der neuen Variable und in welchem Objekt sie enthalten ist, ausgegeben.
- EXCEPTION: Falls eine Klasse eine neue Variable enthält, wird eine DeserialiseException geworfen
und der Programmablauf unterbrochen.<br>
Dies ist die standardmäßige Einstellung.

Die Einstellung newVariableHandling lässt sich mit `setNewVariableHandling(NewVariableHandling)` überschreiben.

### compressedJson
Standardmäßig werden die generierten JSON-Strings mit Leerzeichen, Zeilenumbrüchen und Tabulatoren versehen,
um die Objekt- und Array-Strukturen hierarchisch anzuordnen und für den Leser übersichtlich und einfach
lesbar zu machen. Alternativ können aber auch komprimierte JSON-Strings generiert werden, die keine Leerzeichen,
Zeilenumbrüche oder Tabulatoren besitzen. Dies kann z.B. in Situationen mit begrenztem Speicherplatz
vorteilhaft sein.<br>
Dieses Verhalten kann mit `setCompressedJson(boolean)` angepasst werden.

## Annotationen und Exceptions
Folgende Annotations und Exceptions existieren:

[**SerialiseIgnore-Annotation:**](src/main/java/de/korne127/circularJsonSerialiser/annotations/SerialiseIgnore.java)
Diese Annotation kann vor ein Attribut gesetzt werden, um es von der Serialisierung und Deserialisierung
auszuschließen.<br>
Die Annotation nimmt einen Parameter an, der als ID fungiert. Vor dem (De-)Serialisieren kann
[eingestellt](#ignoreexceptionids) werden, dass SerialiseIgnore-Annotationen mit bestimmten IDs ignoriert werden,
die entsprechenden Felder also doch serialisiert werden.<br>
Dies erlaubt eine dynamische Serialisierung für verschiedene Fälle, in denen verschiedene Felder serialisiert
werden sollen.

[**SerialiseFile-Annotation:**](src/main/java/de/korne127/circularJsonSerialiser/annotations/SerialiseFile.java)
Diese Annotation kann vor eine Klasse gesetzt werden, um festzulegen, in welcher Datei die Objekte dieser Klasse
deserialisisert werden, wenn die separate Serialisierung genutzt wird. Die Annotation kann auch direkt vor ein
Attribut gesetzt werden; dies überschreibt, falls die Klasse des Objektes annotiert ist.<br>
Ein Array wird standardmäßig in die Datei geschrieben, in die die Elemente des Arrays gehören; eine Collection
oder Map wird standardmäßig in die Datei geschrieben, in der die Elternklasse steht.

[**BeforeSerialise-Annotation:**](src/main/java/de/korne127/circularJsonSerialiser/annotations/BeforeSerialise.java)
Diese Annotation kann vor eine Methode gesetzt werden. Es werden alle Methoden mit einer
BeforeSerialise-Annotation vor dem Serialisieren ausgeführt.<br>
Die Annotation nimmt einen Parameter an. Vor dem (De-)Serialisieren kann für jeden Parameter ein
Objekt [angegeben](#methodparameters) werden. Falls die Methode ein Argument annimmt, wird dann das Objekt,
dass zu dem Parameter der Annotation angegeben wurde, an die Methode angegeben.

[**Setter-Annotation:**](src/main/java/de/korne127/circularJsonSerialiser/annotations/Setter.java)
Diese Annotation kann vor ein Feld gesetzt werden. Sie nimmt einen Parameter an; zu diesem muss vor dem
(De-)Serialisieren ein Objekt [angegeben](#methodparameters) werden. Dann wird bei der Deserialisierung der
Feldwert auf das entsprechende zum Annotations-Parameter angegebene Objekt gesetzt.
Alternativ kann vor dem (De-)Serialisieren [eingestellt](#ignoreexceptionids) werden, dass
Setter-Annotationen mit bestimmten Parametern ignoriert werden, die entsprechenden Felder also nicht auf
angegebene Werte gesetzt, sondern normal deserialisiert werden. Dies erlaubt eine dynamische Serialisierung
für verschiedene Fälle, in denen verschiedene Felder serialisiert werden sollen.

[**AfterDeserialise-Annotation:**](src/main/java/de/korne127/circularJsonSerialiser/annotations/AfterDeserialise.java)
Diese Annotation kann vor eine Methode gesetzt werden. Es werden alle Methoden mit einer
AfterDeserialise-Annotation nach dem Deserialisieren ausgeführt.<br>
Die Annotation nimmt einen Parameter an. Vor dem (De-)Serialisieren kann für jeden Parameter ein
Objekt [angegeben](#methodparameters) werden. Falls die Methode ein Argument annimmt, wird dann das Objekt,
das zu dem Parameter der Annotation angegeben wurde, an die Methode angegeben.

[**IgnoreCasting-Annotation::**](src/main/java/de/korne127/circularJsonSerialiser/annotations/IgnoreCasting.java)
Diese Annotation kann vor ein Feld gesetzt werden. In diesem Fall wird, falls das Feld eine Collection bzw.
Map, die zu einer anderen gecastet werden muss, ist oder enthält, keine Warnung ausgegeben
(es sei denn, der [DEBUG-Modus](#collectionhandling) ist aktiviert).<br>
Dies ist hilfreich, wenn man nicht allgemein alle Warnungen [ausschalten](#collectionhandling), sie aber
für ein bestimmtes Feld unterdrücken will, z. B. falls es beabsichtigt ist, dass ein bestimmtes Feld umgecastet
wird.

[**SerialiseException:**](src/main/java/de/korne127/circularJsonSerialiser/exceptions/SerialiseException.java)
Diese Exception wird geworfen, wenn ein Objekt nicht serialisiert werden konnte, z. B. weil ein Wert nicht
abgefragt werden konnte.

[**DeserialiseException:**](src/main/java/de/korne127/circularJsonSerialiser/exceptions/DeserialiseException.java)
Diese Exception wird geworfen, wenn ein Fehler während der Deserialisierung auftritt, z. B. wenn eine Klasse
oder ein verlinktes Objekt nicht gefunden oder auf einen Wert gesetzt werden konnte.

[**JsonParseException:**](src/main/java/de/korne127/circularJsonSerialiser/exceptions/JsonParseException.java)
Diese Exception wird geworfen, wenn ein Fehler während des Lesens des JSON-Strings auftritt. Sofern die
JSON-Strings nicht editiert werden, sollte diese Exception nicht geworfen werden.

## Zu beachten
Folgende Dinge gibt es bei der Benutzung des CircularJsonSerialisers zu beachten:

### Standardkonstruktor benötigt
Jede Klasse, die serialisiert wird, muss einen Standardkonstruktor, also einen Konstruktor, der im Format
`new Class()` ist und keine Argumente annimmt, besitzen.

### Interfaces
Es ist mit dem CircularJsonSerialiser nicht möglich, Instanzen von Interfaces, also anonyme Klassen oder
Lambda-Funktionen zu serialisieren. Falls eine anonyme Klasse oder eine Lambda-Funktion angegeben wird oder
in einer Klasse zu serialisieren ist, wird eine entsprechende SerialiseException geworfen.<br>
Falls es nötig ist, eine Instanz eines Interfaces zu serialisieren, muss daher eine Unterklasse gebildet werden.
Diese Unterklasse enthält dann den entsprechenden Code, den die anonyme Klasse / Lambda-Funktion ausführen würde
und kann normal als Klasse serialisiert und deserialisiert werden.

### Eventuelles Casten von Collections / Maps
Falls ein spezieller Collection oder Map-Typ benutzt wird, der nicht deserialisiert werden kann, wird der Typ der
Collection in eine möglichst geeignete, der Struktur ähnliche, alternative Klasse umgewandelt. In diesem Fall
wird standardmäßig eine Warnung ausgedruckt, die beschreibt, welche Klasse benutzt wurde. Beim Erstellen der
Serialiser-Instanz lässt sich allerdings einer von vier Modi auswählen, der bestimmt, wie sich der Serialiser
in diesem Fall verhalten soll. Zusätzlich lassen sich mit der IgnoreCasting-Annotation auch die Warnungen für
ein bestimmtes Feld, z. B. wenn ein Casten in diesem Fall beabsichtigt ist, unterdrücken.

### Speichern von besonderen Collections / Maps
Collections oder Maps, die entweder keinen Konstruktor besitzen (z. B. EnumSets) oder, die zusätzlich zu den
eigentlichen Werten weitere Inhalte speichern (z. B. TreeSets), können automatisch nicht 100%ig korrekt
wiederhergestellt werden.<br>
Wie in [Eventuelles Casten von Collections / Maps](#eventuelles-casten-von-collections--maps) gezeigt,
werden Collections oder Maps ohne Konstruktor in eine möglichst ähnliche andere Collection / Map deserialisiert;
zusätzliche Inhalte, die in Collections oder Maps gespeichert sind, werden nicht wiederhergestellt.<br>
Das kann dazu führen, dass sich die Collections / Maps im Nachhinein anders verhalten als vor der Serialisierung.<br>
Im Folgenden sind zwei Beispiele abgebildet, die zeigen, wie man manuell jeden dieser Spezialfälle korrekt
speichern und wiederherstellen kann, um dies zu vermeiden:

#### Collections / Maps ohne Konstruktor (Beispiel: EnumSet)
Einige Collections / Maps besitzen keinen Konstruktor. In diesem Fall wird die Collection / Map
beim Deserialisieren in eine möglichst ähnliche andere Collection / Map umgewandelt.<br>
Im Falle eines EnumSets wird beispielsweise ein LinkedHashSet wiederhergestellt, das deutlich weniger performant
ist als das für Enums optimierte EnumSet.<br>
Häufig ist dies eine ausreichende Alternative, allerdings kann es auch Fälle geben, in denen es notwendig
ist, dass die eigentliche spezielle Collection / Map ohne Konstruktor wiederhergestellt wird.

Daher wird hier am Beispiel eines EnumSets gezeigt, wie man das machen kann:

```java
@SerialiseIgnore
private EnumSet<ExampleEnum> enumSet;
@IgnoreCasting
private Set<ExampleEnum> enumSetForSerialisation;

@BeforeSerialise
private void serialiseEnumSet() {
	enumSetForSerialisation = enumSet;
}

@AfterDeserialise
private void deserialiseEnumSet() {
	enumSet = EnumSet.copyOf(enumSetForSerialisation);
	enumSetForSerialisation = null;
}
```

Der Serialiser macht bei diesem Code das Folgende:

Vor der Serialisierung wird die Variable enumSetForSerialisation auf das enumSet gelegt.<br>
Bei der Serialisierung wird die eigentliche Variable enumSet wegen der SerialiseIgnore-Annotation nicht
serialisiert, sondern nur enumSetForSerialisation.<br>
Bei der Deserialisierung wird enumSetForSerialisation von einem EnumSet zu einem LinkedHashSet umgecastet.
Das ist der Grund, warum enumSetForSerialisation vom allgemeineren Typen "Set" sein muss.
Wegen der IgnoreCasting-Annotation wird keine Warnung geworfen.<br>
Nach der Deserialisierung wird ein neues EnumSet mit den Inhalten des LinkedHashSets enumSetForSerialisation
generiert und auf die Variable enumSet gelegt. Die Variable enumSetForSerialisation wird zurückgesetzt.

Somit wird das EnumSet mit allen Inhalten serialisiert und korrekt als EnumSet wiederhergestellt.<br>
Der Vorteil der zusätzlichen Variable enumSetForSerialisation ist, dass die eigentliche Variable
enumSet als EnumSet gespeichert werden kann (während enumSetForSerialisation das allgemeinere Set sein muss).
Falls das EnumSet selbst nur als Set gespeichert wird, kann man die zusätzliche Variable aber auch weglassen.

#### Collections / Maps mit zusätzlichen Inhalten (Beispiel: TreeSet)
Einige Collections / Maps speichern zusätzlich zu den Elementen noch andere Werte. Beim normalen Serialisieren
werden lediglich die Elemente gespeichert und wiederhergestellt, zusätzliche Werte gehen dabei verloren.<br>
Im Falle eines TreeSets wird beispielsweise der Comparator nicht wiederhergestellt.<br>
Es kann allerdings notwendig sein, dass die Collection / Map auch mit bestimmten zusätzlichen Werten
wiederhergestellt wird, damit sie sich nicht anders verhält als vor der Serialisierung.

Daher wird hier am Beispiel eines TreeSets gezeigt, wie man das machen kann:

```java
private TreeSet<String> treeSet;

@AfterDeserialise
private void deserialiseTreeSet() {
	TreeSet<String> help = new TreeSet<>(Comparator.comparingInt(String::length));
	help.addAll(so);
	so = help;
}
```

Der Serialiser macht bei diesem Code das Folgende:

Das TreeSet wird normal serialisiert und die einzelnen Elemente werden gespeichert.<br>
Das TreeSet wird normal deserialisiert und die einzelnen Elemente werden als Werte des TreeSets gesetzt.<br>
Nach der Deserialisierung wird ein neues TreeSet erstellt, das einen eigenen Comparator, der festgelegt ist
sowie alle Elemente, die gespeichert wurden, besitzt.

Somit wird das TreeSet mit allen Elementen und dem Comparator korrekt wiederhergestellt.

Falls der Comparator nicht, wie in diesem Beispiel, immer gleich ist, sondern dynamisch wechseln kann, kann er
alternativ auch (als Unterklasse) selbst gespeichert und wiederhergestellt werden.

### Benutzen des Serialisers über verschiedene Versionen
Standardmäßig wird eine DeserialiseException geworfen, falls während der Deserialisierung eine "neue Variable",
also eine Variable in einer Klasse, die nicht im JSON-gespeichert wurde, aber (de-)serialisiert werden soll,
entdeckt wird. Im Normalfall ist dies das gewünschte Verhalten, um sicherzustellen, dass die resultierende
Objektstruktur identisch zur Originalen ist.<br>
Falls der Serialiser aber über verschiedene Versionen eines Programmes hinweg benutzt werden soll, wenn z. B. vor
einer Aktualisierung eine Serialisierung und nach der Aktualisierung die Deserialisierung passieren soll, können
Klassen neue Variablen bekommen. In diesem Fall ist es also gewünscht, dass solche neuen Variablen ignoriert
werden. Dies lässt sich in der Konfiguration einstellen.

### Abweichungen von dem JSON-Standard
JSON wurde ursprünglich für Javascript entwickelt und ist auf die Bedürfnisse dieser Sprache angepasst. Der
JSON-Parser dieses Serialisers weicht in einigen Punkten von dem offiziellen JSON-Standard ab, da er für diesen
Serialiser und dessen Aufgaben speziell optimiert ist:
- Chars werden als einzelne Zeichen zwischen einem '-Paar kodiert
- '-Zeichen werden daher mit einem Backslash (\\) escaped
- Schrägstriche (/) werden nicht mit einem Backslash escaped
- Die Zahlenwerte NaN, Infinity und -Infinity werden gespeichert und wiederhergestellt
- Bytes wird am Ende der Zahl ein B, Shorts ein S, Longs ein L und Floats ein F hinzugefügt

### Lizenz
Der CircularJsonSerialiser ist unter der GNU Affero General Public Licence v3 lizenziert.<br>
Wenn der Quellcode verändert oder für ein anderes Projekt in irgendeiner Form benutzt wird und der veränderte
Code bzw. das eigene Projekt geteilt, verkauft, auf einen Server geladen oder anderweitig zur Verfügung gestellt
wird, muss der vollständige Quellcode des Resultats unter dieser Lizenz (GNU AGPNv3) geteilt werden.<br>
Siehe [LICENSE](LICENSE) für mehr Informationen.
