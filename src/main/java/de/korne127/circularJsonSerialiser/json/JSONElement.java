package de.korne127.circularJsonSerialiser.json;

/**
 * JSONElement-Interface:<br>
 * Dies Interface umschließt JSONObject und JSONArray.<br>
 * Es stellt mehrere Methoden bereit, die von beiden Unterklassen dieses Interfaces benutzt werden.
 * @author Korne127
 */
public interface JSONElement {

	/**
	 * Gibt einen formatierten String zurück, der alle Inhalte des JSON-Elementes beinhaltet.
	 * @param indentFactor Die Anzahl an Tabs, die vor den Unterelementen gesetzt werden soll
	 * @return Ein formatierter String, der alle Inhalte des JSON-Elementes beinhaltet
	 */
	String toString(int indentFactor);

	/**
	 * Gibt einen String mit einer bestimmten Anzahl an Tabs zurück.
	 * @param numberOfTabs Die Anzahl an Tabs, die hintereinander zurückgegeben werden sollen
	 * @return Ein String mit einer bestimmten Anzahl an Tabs
	 */
	default String getTabs(int numberOfTabs) {
		StringBuilder tabs = new StringBuilder();
		for (int i = 0; i < numberOfTabs; i++) {
			tabs.append("\t");
		}
		return tabs.toString();
	}

}
