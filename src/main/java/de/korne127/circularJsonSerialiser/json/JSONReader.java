package de.korne127.circularJsonSerialiser.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSONReader-Klasse:<br>
 * Diese Klasse verfügt über statische Methoden um Inhalte eines JSON-Strings einzulesen
 * und in JSON-Elemente umzuwandeln.
 * @author Korne127
 */
class JSONReader {

	/**
	 * Enum, das den Status des Einlesens eines Arrays im JSON-String repräsentiert
	 */
	private enum ObjectReadingState {
		BEFORE_KEY,
		KEY,
		AFTER_KEY,
		VALUE,
		AFTER_VALUE
	}

	/**
	 * Enum, das den Status des Einlesens eines Arrays im JSON-String repräsentiert
	 */
	private enum ArrayReadingState {
		VALUE,
		AFTER_VALUE
	}

	/**
	 * Transformiert einen bestimmten String mit der angegebenen Position, an der die
	 * Kodierung eines JSON-Objektes startet in das JSON-Objekt
	 * @param content Der String, in dem sich das kodierte JSON-Objekt befindet
	 * @param iteratorStart Die Position im String, an der das kodierte JSON-Objekt beginnt
	 * @return Ein JSONResult, welches aus dem JSON-Objekt, welches aus der Kodierung hergestellt
	 * wurde sowie der Position im String, an der die Kodierung des JSON-Objektes endet, besteht
	 */
	static JSONResult readObject(String content, int iteratorStart) {
		if (content.charAt(iteratorStart) != '{') {
			//TODO Hier eine Exception werfen
		}

		ObjectReadingState state = ObjectReadingState.BEFORE_KEY;
		StringBuilder key = new StringBuilder();
		Object value = null;
		Map<String, Object> map = new LinkedHashMap<>();

		for (int characterIterator = iteratorStart + 1; characterIterator < content.length(); characterIterator++) {
			char character = content.charAt(characterIterator);
			switch (state) {
				case BEFORE_KEY:
					switch (character) {
						case '\"': state = ObjectReadingState.KEY; break;
						case ' ': break;
						default: break; //TODO Hier eine Exception werfen
					}
					break;
				case KEY:
					if (character == '\"') {
						state = ObjectReadingState.AFTER_KEY;
					} else {
						key.append(character);
					}
					break;
				case AFTER_KEY:
					if (character == ':') {
						state = ObjectReadingState.VALUE;
					} else if (character != ' ') {
						//TODO Hier eine Exception werfen
					}
					break;
				case VALUE:
					JSONResult result;
					switch (character) {
						case ' ': continue;
						case '{': result = readObject(content, characterIterator); break;
						case '[': result = readArray(content, characterIterator); break;
						case '"': result = readString(content, characterIterator); break;
						default: result = readBasicType(content, characterIterator); break;
					}
					value = result.getValue();
					characterIterator = result.getEndPosition() - 1;
					state = ObjectReadingState.AFTER_VALUE;
					break;
				case AFTER_VALUE:
					switch (character) {
						case ' ': break;
						case ',':
							map.put(key.toString(), value);
							key = new StringBuilder();
							value = null;
							state = ObjectReadingState.BEFORE_KEY;
							break;
						case '}':
							map.put(key.toString(), value);
							return new JSONResult(new JSONObject(map), characterIterator + 1);
						default: break; //TODO Hier eine Exception werfen
					}
					break;
			}
		}
		//TODO Hier eine Exception werfen
		return null;
	}

	/**
	 * Transformiert einen bestimmten String mit der angegebenen Position, an der die
	 * Kodierung eines JSON-Arrays startet in das JSON-Array
	 * @param content Der String, in dem sich das kodierte JSON-Array befindet
	 * @param iteratorStart Die Position im String, an der das kodierte JSON-Array beginnt
	 * @return Ein JSONResult, welches aus dem JSON-Array, welches aus der Kodierung hergestellt
	 * wurde sowie der Position im String, an der die Kodierung des JSON-Arrays endet, besteht
	 */
	static JSONResult readArray(String content, int iteratorStart) {
		if (content.charAt(iteratorStart) != '[') {
			//TODO Hier eine Exception werfen
		}

		ArrayReadingState state = ArrayReadingState.VALUE;
		Object value = null;
		List<Object> list = new ArrayList<>();

		for (int characterIterator = iteratorStart + 1; characterIterator < content.length(); characterIterator++) {
			char character = content.charAt(characterIterator);
			switch (state) {
				case VALUE:
					JSONResult result;
					switch (character) {
						case ' ': continue;
						case '{': result = readObject(content, characterIterator); break;
						case '[': result = readArray(content, characterIterator); break;
						case '"': result = readString(content, characterIterator); break;
						default: result = readBasicType(content, characterIterator); break;
					}
					value = result.getValue();
					characterIterator = result.getEndPosition() - 1;
					state = ArrayReadingState.AFTER_VALUE;
					break;
				case AFTER_VALUE:
					switch (character) {
						case ' ': break;
						case ',':
							list.add(value);
							value = null;
							state = ArrayReadingState.VALUE;
							break;
						case ']':
							list.add(value);
							return new JSONResult(new JSONArray(list), characterIterator + 1);
						default: break; //TODO Hier eine Exception werfen
					}
					break;
			}
		}
		//TODO Hier eine Exception werfen
		return null;
	}

	/**
	 * Transformiert einen bestimmten String mit der angegebenen Position, an der die
	 * Kodierung eines Strings startet in diesen String
	 * @param content Der String, in dem sich der kodierte String befindet
	 * @param iteratorStart Die Position im String, an der der kodierte String beginnt
	 * @return Ein JSONResult, welches aus dem String, welcher aus der Kodierung hergestellt
	 * wurde sowie der Position im String, an der die Kodierung des Strings endet, besteht
	 */
	private static JSONResult readString(String content, int iteratorStart) {
		if (content.charAt(iteratorStart) != '\"') {
			//TODO Hier eine Exception werfen
		}

		StringBuilder builder = new StringBuilder();
		char lastCharacter = 0;

		for (int characterIterator = iteratorStart + 1; characterIterator < content.length(); characterIterator++) {
			char character = content.charAt(characterIterator);
			if (lastCharacter == '\\') {
				switch (character) {
					case 't': builder.append("\t"); break;
					case 'b': builder.append("\b"); break;
					case 'n': builder.append("\n"); break;
					case 'r': builder.append("\r"); break;
					case 'f': builder.append("\f"); break;
					case '\"': builder.append("\""); break;
					case '\'': builder.append("'"); break;
					case '\\': builder.append("\\"); break;
					default: break; //TODO Hier eine Exception werfen
				}
				lastCharacter = 0;
			} else {
				if (character == '\"') {
					return new JSONResult(builder.toString(), characterIterator + 1);
				} else if (character != '\\') {
					builder.append(character);
				}
				lastCharacter = character;
			}
		}
		//TODO Hier eine Exception werfen
		return  null;
	}

	/**
	 * Transformiert einen bestimmten String mit der angegebenen Position, an der die
	 * Kodierung eines primitiven Typens startet in den primitiven Datentypen.
	 * @param content Der String, in dem sich der primitive Datentyp befindet
	 * @param iteratorStart Die Position im String, an der der primitive Datentyp beginnt
	 * @return Ein JSONResult, welches aus dem primitiven Datentypen, welches aus der Kodierung
	 * hergestellt wurde sowie der Position im String, an der die Kodierung des Typen endet, besteht
	 */
	private static JSONResult readBasicType(String content, int iteratorStart) {
		Object value;
		int iteratorSkip;

		content = content.substring(iteratorStart);
		if (content.startsWith("true")) {
			value = true;
			iteratorSkip = 4;
		} else if (content.startsWith("false")) {
			value = false;
			iteratorSkip = 5;
		} else if (content.startsWith("null")) {
			value = null;
			iteratorSkip = 4;
		} else if (content.startsWith("'")) {
			value = content.charAt(1);
			iteratorSkip = 3;
		} else {
			StringBuilder builder = new StringBuilder();
			iteratorSkip = content.length();
			for (int characterIterator = 0; characterIterator < content.length(); characterIterator++) {
				char character = content.charAt(characterIterator);
				if (character == ',' || character == '}' || character == ']') {
					iteratorSkip = characterIterator;
					break;
				} else {
					builder.append(character);
				}
			}
			String numberString = builder.toString().trim();
			value = 0;
			if (numberString.equals("")) {
				//TODO Hier eine Exception werfen
			}

			char lastLetter = numberString.charAt(numberString.length() - 1);
			switch (lastLetter) {
				case 'B':
					try {
						value = Byte.parseByte(numberString.substring(0, numberString.length() - 1));
					} catch (NumberFormatException e) {
						//TODO Hier eine Exception werfen
					}
					break;
				case 'S':
					try {
						value = Short.parseShort(numberString.substring(0, numberString.length() - 1));
					} catch (NumberFormatException e) {
						//TODO Hier eine Exception werfen
					}
					break;
				case 'L':
					try {
						value = Long.parseLong(numberString.substring(0, numberString.length() - 1));
					} catch (NumberFormatException e) {
						//TODO Hier eine Exception werfen
					}
					break;
				case 'F':
					try {
						value = Float.parseFloat(numberString.substring(0, numberString.length() - 1));
					} catch (NumberFormatException e) {
						//TODO Hier eine Exception werfen
					}
					break;
				default:
					if (numberString.contains(".") || numberString.contains("e") || numberString.contains("E")) {
						try {
							value = Double.parseDouble(numberString);
						} catch (NumberFormatException e) {
							//TODO Hier eine Exception werfen
						}
					} else {
						try {
							value = Integer.parseInt(numberString);
						} catch (NumberFormatException e) {
							//TODO Hier eine Exception werfen
						}
					}
					break;
			}

		}
		return new JSONResult(value, iteratorStart + iteratorSkip);
	}

}
