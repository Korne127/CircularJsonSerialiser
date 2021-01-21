package de.korne127.circularJsonSerialiser.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.korne127.circularJsonSerialiser.exceptions.JsonParseException;

/**
 * JSONReader-Klasse:<br>
 * Diese Klasse verfügt über statische Methoden um Inhalte eines JSON-Strings einzulesen
 * und in JSON-Elemente umzuwandeln.
 * @author Korne127
 */
class JSONReader {

	/**
	 * Enum, das den Status des Einlesens eines JSON-Elementes im JSON-String repräsentiert
	 */
	private enum ElementReadingState {
		BEFORE_KEY,
		KEY,
		AFTER_KEY,
		VALUE,
		AFTER_VALUE
	}

	/**
	 * Transformiert einen bestimmten String mit der angegebenen Position, an der die
	 * Kodierung eines JSON-Elementes startet in das JSON-Element
	 * @param content Der String, in dem sich das kodierte JSON-Element befindet
	 * @param iteratorStart Die Position im String, an der das kodierte JSON-Element beginnt
	 * @return Ein JSONResult, welches aus dem JSON-Element, welches aus der Kodierung hergestellt
	 * wurde sowie der Position im String, an der die Kodierung des JSON-Elementes endet, besteht
	 * @throws JsonParseException Wird geworfen, falls das JSON-Element aus dem JSON-String nicht
	 * geparst werden konnte.
	 */
	static JSONResult readElement(String content, boolean isObject, int iteratorStart) throws JsonParseException {
		char startChar = isObject ? '{' : '[';
		char endChar = isObject ? '}' : ']';
		String element = isObject ? "Object" : "Array";

		if (content.charAt(iteratorStart) != startChar) {
			throw new JsonParseException(element + " in JSON-String could not be parsed.");
		}

		ElementReadingState state = isObject ? ElementReadingState.BEFORE_KEY : ElementReadingState.VALUE;
		Object value = null;
		StringBuilder key = new StringBuilder();
		Map<String, Object> map = new LinkedHashMap<>();
		List<Object> list = new ArrayList<>();

		for (int characterIterator = iteratorStart + 1; characterIterator < content.length(); characterIterator++) {
			char character = content.charAt(characterIterator);
			switch (state) {
				case BEFORE_KEY:
					switch (character) {
						case '\"': state = ElementReadingState.KEY; break;
						case ' ': break;
						default: throw new JsonParseException("Object in JSON-String could not be parsed.");
					}
					break;
				case KEY:
					if (character == '\"') {
						state = ElementReadingState.AFTER_KEY;
					} else {
						key.append(character);
					}
					break;
				case AFTER_KEY:
					if (character == ':') {
						state = ElementReadingState.VALUE;
					} else if (character != ' ') {
						throw new JsonParseException("Object in JSON-String could not be parsed.");
					}
					break;
				case VALUE:
					JSONResult result;
					switch (character) {
						case ' ': continue;
						case '{': result = readElement(content, true, characterIterator); break;
						case '[': result = readElement(content, false, characterIterator); break;
						case '"': result = readString(content, characterIterator); break;
						default: result = readBasicType(content, characterIterator); break;
					}
					value = result.getValue();
					characterIterator = result.getEndPosition() - 1;
					state = ElementReadingState.AFTER_VALUE;
					break;
				case AFTER_VALUE:
					switch (character) {
						case ' ': break;
						case ',':
							if (isObject) {
								map.put(key.toString(), value);
								key = new StringBuilder();
								state = ElementReadingState.BEFORE_KEY;
							} else {
								list.add(value);
								state = ElementReadingState.VALUE;
							}
							value = null;
							break;
						default:
							if (character == endChar) {
								if (isObject) {
									map.put(key.toString(), value);
									return new JSONResult(new JSONObject(map), characterIterator + 1);
								} else {
									list.add(value);
									return new JSONResult(new JSONArray(list), characterIterator + 1);
								}
							}
							throw new JsonParseException(element + " in JSON-String could not be parsed.");
					}
					break;
			}
		}
		throw new JsonParseException(element + " in JSON-String could not be parsed.");
	}

	/**
	 * Transformiert einen bestimmten String mit der angegebenen Position, an der die
	 * Kodierung eines Strings startet in diesen String
	 * @param content Der String, in dem sich der kodierte String befindet
	 * @param iteratorStart Die Position im String, an der der kodierte String beginnt
	 * @return Ein JSONResult, welches aus dem String, welcher aus der Kodierung hergestellt
	 * wurde sowie der Position im String, an der die Kodierung des Strings endet, besteht
	 * @throws JsonParseException Wird geworfen, falls der String aus dem JSON-String nicht
	 * geparst werden konnte.
	 */
	private static JSONResult readString(String content, int iteratorStart) throws JsonParseException {
		if (content.charAt(iteratorStart) != '\"') {
			throw new JsonParseException("String in JSON-String could not be parsed.");
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
					default: throw new JsonParseException("String in JSON-String could not be parsed.");
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
		throw new JsonParseException("String in JSON-String could not be parsed.");
	}

	/**
	 * Transformiert einen bestimmten String mit der angegebenen Position, an der die
	 * Kodierung eines primitiven Typen startet in den primitiven Datentypen.
	 * @param content Der String, in dem sich der primitive Datentyp befindet
	 * @param iteratorStart Die Position im String, an der der primitive Datentyp beginnt
	 * @return Ein JSONResult, welches aus dem primitiven Datentypen, welches aus der Kodierung
	 * hergestellt wurde sowie der Position im String, an der die Kodierung des Typen endet, besteht
	 * @throws JsonParseException Wird geworfen, falls die Nummer aus dem JSON-String nicht geparst
	 * werden konnte.
	 */
	private static JSONResult readBasicType(String content, int iteratorStart) throws JsonParseException {
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
			if (numberString.equals("")) {
				throw new JsonParseException("Number in JSON-String could not be parsed.");
			}

			char lastLetter = numberString.charAt(numberString.length() - 1);
			switch (lastLetter) {
				case 'B':
					try {
						value = Byte.parseByte(numberString.substring(0, numberString.length() - 1));
					} catch (NumberFormatException e) {
						throw new JsonParseException("Number in JSON-String could not be parsed.", e);
					}
					break;
				case 'S':
					try {
						value = Short.parseShort(numberString.substring(0, numberString.length() - 1));
					} catch (NumberFormatException e) {
						throw new JsonParseException("Number in JSON-String could not be parsed.", e);
					}
					break;
				case 'L':
					try {
						value = Long.parseLong(numberString.substring(0, numberString.length() - 1));
					} catch (NumberFormatException e) {
						throw new JsonParseException("Number in JSON-String could not be parsed.", e);
					}
					break;
				case 'F':
					try {
						value = Float.parseFloat(numberString.substring(0, numberString.length() - 1));
					} catch (NumberFormatException e) {
						throw new JsonParseException("Number in JSON-String could not be parsed.", e);
					}
					break;
				default:
					if (numberString.contains(".") || numberString.contains("e") || numberString.contains("E")) {
						try {
							value = Double.parseDouble(numberString);
						} catch (NumberFormatException e) {
							throw new JsonParseException("Number in JSON-String could not be parsed.", e);
						}
					} else {
						try {
							value = Integer.parseInt(numberString);
						} catch (NumberFormatException e) {
							throw new JsonParseException("Number in JSON-String could not be parsed.", e);
						}
					}
					break;
			}
		}
		return new JSONResult(value, iteratorStart + iteratorSkip);
	}

}
