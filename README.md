# CircularJsonSerialiser
## Allgemein
CircularJsonSerialiser ist ein Serialiser für Java mit dem Ziel, dass ohne manuelle Konfiguration alle Klassen-
und Objektstrukturen korrekt gespeichert und identisch wiederhergestellt werden.<br>
Alle bidirektionalen und zirkulären Verweise zwischen verschiedenen Objekten werden automatisch behandelt und korrekt
gespeichert und aufgelöst.<br>
Außerdem wird die Identität von Objekten erkannt; wenn ein Objekt zum Beispiel in zwei Listen gespeichert wird, werden
beim Deserialisieren nicht zwei verschiedene Instanzen, sondern dasselbe Objekt in beide Listen gespeichert.<br>
Auch die Art primitiver Datentypen wie z.B. Zahlen oder Datenstrukturen wie z.B. Listen wird korrekt gespeichert,
sodass nach dem Deserialisieren keine Cast Exceptions durch falsch wiederhergestellte Objekte zu erwarten sind.

## Benutzen des Serialisers
Um den CircularJsonSerialiser in ein Projekt einzubinden, muss die .jar-Datei des neuesten Releases heruntergeladen
und manuell dem Projekt in der IDE hinzugefügt werden.

Um ein Objekt zu serialisieren, muss zunächst ein `Serialiser`-Objekt erstellt werden:
```java
Serialiser serialiser = new Serialiser();
```
Ein Objekt kann auf zwei Arten serialisiert werden:<br>
Die Standardserialisierung verwandelt das Objekt in einen JSON-String, der in einer Datei gespeichert wird:
```java
String jsonString = serialiser.serialiseObject(object);
```
Das Objekt wird wiederhergestellt, in dem dieser JSON-String an die Standard-Deserialisierungsmethode gegeben wird:
```java
Object object = serialiser.deserialiseObject(jsonString);
```

Alternativ kann auch eine andere Art, ein Objekt zu serialisieren, verwendet werden; mithilfe der
`serialiseObjectSeparated`-Methode:
```java
Map<String, String> jsonMap = serialiser.serialiseObjectSeparated(object);
```
Diese kann benutzt werden, wenn die serialisierten Objekte in mehrere Dateien unterteilt werden sollen. Dazu können
Klassen und Attribute mit einer `SerialiseFile`-Annotation versehen werden, die den Dateinamen enthält,
zu dem die entsprechenden Objekte zugeordnet werden sollen, siehe
[Annotations und Exceptions](#annotations-und-exceptions).
Die einzelnen Strings werden in einer Map ihrem Dateinamen zugeordnet zurückgegeben.

In den Strings werden Verweise zwischen den einzelnen Objekten gespeichert, die beim Deserialisieren wieder
zusammengesetzt werden. Somit können mit der `deserialiseSeparatedObject`-Methode die verschiedenen JSON-Strings
wieder in ein Objekt umgewandelt werden:
```
Object object = serialiser.deserialiseSeparatedObject(jsonMap);
```

Bei der Serialisierung eines Objektes kann eine `SerialiseException`, bei der Deserialisierung können eine
`DeserialiseException` und eine `JsonParseException` geworfen werden.<br>
Für mehr Informationen zu möglichen Auslösern, siehe [Annotations und Exceptions](#annotations-und-exceptions).

## Besondere Funktionen dieses Serialisers
Das Ziel dieses Serialisers ist es, Objektstrukturen ohne manuelle Konfiguration so akkurat wie möglich zu
speichern und wiederherzustellen, sodass das Programm nach dem Deserialisierungsprozess identisch funktionieren
kann.

Da andere Serialiser wie Gson oder Jackson Performanz und nicht eine möglichst akkurate Speicherung der
Objektstrukturen als Priorität besitzen, hat dieser Serialiser einige zusätzliche Funktionen:

### Bidirektionale Verweise und Zirkelverweise
Objekte können sich gegenseitig referenzieren und ohne manuelle Konfiguration korrekt (de)serialisiert
werden. Zusätzlich werden die Identitäten von Objekten gespeichert, sodass sich Strukturen beim Deserialisieren
nicht doppelt bilden und Objekte mehrfach gespeichert werden würden.

### Unterteilung der JSON-Speicherung in mehrere Dateien
Es kann für jede Klasse und jedes Attribut ein Dateiname annotiert werden, zu dem Objekte aus dieser Klass
bzw. das entsprechende Objekt des Attributes serialisiert gespeichert
werden sollen. Mit `Map<String, String> serialised = serialiser.serialiseObjectSeparated(object)` wird eine Map
zurückgegeben, die die einzelnen JSON-Strings den Dateinamen, zu denen sie gespeichert werden sollen, zugeordnet
beinhaltet. Diese Map kann an den Deserialiser übergeben werden, der daraus erfolgreich das Objekt wiederherstellt.

Dies kann hilfreich sein, falls die serialisierten Strings auch für Menschen einfach überschaubar sein sollen:<br>
Einzelne Klassen und Objekte können in einer anderen Datei gespeichert werden, um die Übersicht zu verbessern
oder den Zugriff auf sensible Informationen zu verhindern.

### Speicherung von eigenen Maps ohne Konfiguration
Es können eigene Klassen als keys für Maps benutzt werden, ohne dass irgendeine Konfiguration nötig ist, wie mit
diesen Maps umgegangen werden soll. Sie werden einfach beim Serialisieren in den JSON-String gespeichert und beim
Deserialisieren wiederhergestellt.

### Automatische Speicherung und Wiederherstellung von Feldern der Oberklassen
Es werden alle Felder eines Objektes gespeichert, auch solche, die in Oberklassen der eigentlichen Klasse
gespeichert sind. Dies stellt sicher, dass sich das generierte Objekt nach dem Deserialisieren identisch zu dem
ursprünglichen Objekt verhält.

### Speicherung der konkreten Unterklasse der Instanz
In bestimmten Entwicklungsmustern wie etwa der Schablonenmethode oder dem Zustands-Muster wird nur die Instanz
einer Oberklasse in dem Code gespeichert, welche dynamisch durch Instanzen der verschiedenen Unterklassen
ausgetauscht werden kann.<br>
Dieser Serialiser speichert die genaue Klasse des Objektes und stellt sie wieder her, sodass die Ausführung des
Programmes nicht verändert wird.

### Chars und alle Zahlentypen werden sicher deserialisiert
Auch die Art des primitiven Zahlentyps wird, anders als bei anderen Serialisern, gespeichert. Dies führt dazu,
dass die Zahl immer im korrekten Typ wiederhergestellt wird und es beim Benutzen von selteneren Typen wie
`char`, `byte` oder `short` nicht zu einer `ClassCastException` kommt.

### Speicherung der konkreten Collection-/Map-Klasse
Die konkrete Information, welche Collection oder Map-Klasse benutzt wird, wird bei dem Serialisieren ebenfalls
gespeichert. So kann beim Deserialisieren eine identische Collection, die sich weiterhin gleich verhält, erstellt
werden.<br>
(Falls es nicht möglich ist, eine Instanz der spezielle Collection-/Map-Klasse herzustellen, kann eine möglichst
ähnliche alternative Klasse benutzt werden.)

### Gut lesbarer, leichtgewichtiger und gut dokumentierter Code
Der Code des Serialisers ist einfach zu verstehen und die Javadoc-Kommentare erklären die Logik treffend.<br>
Außerdem ist das Projekt mit nur etwas über tausend Codezeilen sehr leichtgewichtig und einfacher zu durchschauen
als der Code größerer Serialiser.

## Annotations und Exceptions
Folgende Annotations und Exceptions existieren:

[**SerialiseIgnore-Annotation:**](src/main/java/de/korne127/circularJsonSerialiser/annotations/SerialiseIgnore.java)
Diese Annotation kann vor ein Attribut gesetzt werden, um es von der Serialisierung und Deserialisierung
auszuschließen.

[**SerialiseFile-Annotation:**](src/main/java/de/korne127/circularJsonSerialiser/annotations/SerialiseFile.java)
Diese Annotation kann vor eine Klasse gesetzt werden, um festzulegen, in welcher Datei die Objekte dieser Klasse
deserialisisert werden, wenn die separate Serialisierung genutzt wird. Die Annotation kann auch direkt vor ein
Attribut gesetzt werden; dies überschreibt, falls die Klasse des Objektes annotiert ist.<br>
Ein Array wird standardmäßig in die Datei geschrieben, in die die Elemente des Arrays gehören; eine Collection
oder Map wird standardmäßig in die Datei geschrieben, in der die Elternklasse steht.

[**SerialiseException:**](src/main/java/de/korne127/circularJsonSerialiser/exceptions/SerialiseException.java)
Diese Exception wird geworfen, wenn ein Objekt nicht serialisiert werden konnte, z.B. weil ein Wert nicht
abgefragt werden konnte.

[**DeserialiseException:**](src/main/java/de/korne127/circularJsonSerialiser/exceptions/DeserialiseException.java)
Diese Exception wird geworfen, wenn ein Fehler während der Deserialisierung auftritt, z.B. wenn eine Klasse
oder ein verlinktes Objekt nicht gefunden oder auf einen Wert gesetzt werden konnte.

[**JsonParseException:**](src/main/java/de/korne127/circularJsonSerialiser/exceptions/JsonParseException.java)
Diese Exception wird geworfen, wenn ein Fehler während des Lesens des JSON-Strings auftritt. Sofern die
JSON-Strings nicht editiert werden, sollte diese Exception nicht geworfen werden.

## Zu beachten
Folgende Dinge gibt es bei der Benutzung des CircularJsonSerialisers zu beachten:

Jede Klasse, die serialisiert wird, muss einen Standardkonstruktor, also einen Konstruktor, der im Format
`new Class()` ist und keine Argumente annimmt, besitzen.

Falls ein spezieller Collection oder Map-Typ benutzt wird, der nicht deserialisiert werden kann, wird der Typ der
Collection in eine möglichst geeignete, der Struktur ähnliche, alternative Klasse umgewandelt. In diesem Fall
wird standardmäßig eine Warnung ausgedruckt, die beschreibt, welche Klasse benutzt wurde. Beim Erstellen der
Serialiser-Instanz lässt sich allerdings einer von vier Modi auswählen, der bestimmt, wie sich der Serialiser
in diesem Fall verhalten soll.

Collections oder Maps, die zusätzlich zu den Inhalten andere Werte speichern, zum Beispiel TreeSets, die zusätzlich
einen Comparator speichern können, könnten nach dem Deserialisieren diese zusätzlichen Werte verlieren und sich
anders verhalten. Daher wird empfohlen, diese nicht ausschließlich durch Deserialisierung wiederherzustellen.

### Abweichungen von dem JSON-Standard
JSON wurde ursprünglich für Javascript entwickelt und ist auf die Bedürfnisse dieser Sprache angepasst. Der
JSON-Parser dieses Serialisers weicht in einigen Punkten von dem offiziellen JSON-Standard ab, da er für diesen
Serialiser und dessen Aufgaben speziell optimiert ist:
- Chars werden als Zeichen zwischen einem '-Paar kodiert
- '-Zeichen innerhalb Strings werden daher mit einem Backslash (\\) escaped
- Schrägstriche (/) in Strings werden nicht mit einem Backslash escaped
- Die Zahlenwerte NaN, Infinity und -Infinity werden gespeichert und wiederhergestellt
- Bytes wird am Ende der Zahl ein B, Shorts ein S, Longs ein L und Floats ein F hinzugefügt

### Lizenz
Der CircularJsonSerialiser ist unter der GNU Affero General Public Licence v3 lizenziert.<br>
Wenn der Quellcode verändert oder für ein anderes Projekt in irgendeiner Form benutzt wird und der veränderte
Code bzw. das eigene Projekt geteilt, verkauft, auf einem Server geladen oder sonst wie zur Verfügung gestellt
wird, muss der vollständige Quellcode des Resultats unter dieser Lizenz (GNU AGPNv3) geteilt werden.<br>
Siehe [LICENSE](LICENSE) für mehr Informationen.
