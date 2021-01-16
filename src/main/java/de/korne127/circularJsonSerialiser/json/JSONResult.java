package de.korne127.circularJsonSerialiser.json;

class JSONResult {
	private final Object value;
	private final int endPosition;

	JSONResult(Object value, int endPosition) {
		this.value = value;
		this.endPosition = endPosition;
	}

	Object getValue() {
		return value;
	}

	int getEndPosition() {
		return endPosition;
	}
}
