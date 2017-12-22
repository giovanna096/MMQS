package io.openems.backend.core;

/**
 * Provider for ConnectionManager singleton
 *
 * @author stefan.feilmeier
 *
 */
public class ConnectionManager {

	private static ConnectionManagerSingleton instance = null;

	/**
	 * Returns the singleton instance
	 *
	 * @return
	 */
	public static ConnectionManagerSingleton instance() {
		synchronized (ConnectionManager.class) {
			if (ConnectionManager.instance == null) {
				ConnectionManager.instance = new ConnectionManagerSingleton();
			}
			return ConnectionManager.instance;
		}
	}
}