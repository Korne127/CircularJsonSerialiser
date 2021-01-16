package de.korne127.circularJsonSerialiser.json;

import java.util.LinkedHashMap;
import java.util.Map;

public class JSONObject implements JSONElement {

	private final Map<String, Object> map;

	public JSONObject() {
		map = new LinkedHashMap<>();
	}

	public JSONObject(Map<String, Object> map) {
		this.map = map;
	}

	public JSONObject(String content) {
		content = content.replaceAll("[\\n\\t]", "");
		map = ((JSONObject) JSONReader.readObject(content, 0).getValue()).map;
	}

	public void put(String key, Object value) {
		map.put(key, value);
	}

	public Object get(String key) {
		if (map.containsKey(key)) {
			return map.get(key);
		}
		//TODO hier eine Exception
		return null;
	}

	public String getString(String key) {
		Object object = get(key);
		if (object instanceof String) {
			return (String) object;
		}
		//TODO hier eine Exception
		return null;
	}

	public Iterable<String> keySet() {
		return map.keySet();
	}

	public String toString(int indentFactor) {
		if (map.size() == 0) {
			return "{}";
		}
		StringBuilder json = new StringBuilder("{");
		for (String key : keySet()) {
			Object value = map.get(key);
			json.append("\n").append(getTabs(indentFactor)).append("\"").append(key).append("\": ")
					.append(elementToString(value, indentFactor)).append(",");
		}
		return json.substring(0, json.length() - 1) + "\n" + getTabs(indentFactor - 1) + "}";
	}

	@Override
	public String toString() {
		return toString(1);
	}

}
