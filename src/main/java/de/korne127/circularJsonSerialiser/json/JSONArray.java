package de.korne127.circularJsonSerialiser.json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JSONArray implements Iterable<Object>, JSONElement {

	private final List<Object> list;

	public JSONArray() {
		list = new ArrayList<>();
	}

	public JSONArray(List<Object> list) {
		this.list = list;
	}

	public void put(Object object) {
		list.add(object);
	}

	public Object get(int index) {
		return list.get(index); //TODO eventuell eigene Exception wenn nicht existieren kann
	}

	public String getString(int index) {
		Object object = get(index);
		if (object instanceof String) {
			return (String) object;
		}
		//TODO hier eine Exception
		return null;
	}

	public int length() {
		return list.size();
	}

	@Override
	public Iterator<Object> iterator() {
		return list.iterator();
	}

	public String toString(int indentFactor) {
		if (list.size() == 0) {
			return "[]";
		}
		StringBuilder json = new StringBuilder("[");
		for (Object object : list) {
			json.append("\n").append(getTabs(indentFactor))
					.append(elementToString(object, indentFactor)).append(",");
		}
		return json.substring(0, json.length() - 1) + "\n" + getTabs(indentFactor - 1) + "]";
	}

	@Override
	public String toString() {
		return toString(1);
	}
}
