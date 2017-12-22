package io.openems.backend.browserwebsocket;

import io.openems.common.exceptions.OpenemsException;

/**
 * Provider for OpenemsWebsocketServer singleton
 *
 * @author stefan.feilmeier
 *
 */
public class BrowserWebsocket {

	private static BrowserWebsocketSingleton instance;

	/**
	 * Initialize and start the Websocketserver
	 *
	 * @param port
	 * @throws Exception
	 */
	public static void initialize(int port) throws OpenemsException {
		synchronized (BrowserWebsocket.class) {
			BrowserWebsocket.instance = new BrowserWebsocketSingleton(port);
			BrowserWebsocket.instance.start();
		}
	}

	/**
	 * Returns the singleton instance
	 *
	 * @return
	 */
	public static BrowserWebsocketSingleton instance() {
		synchronized (BrowserWebsocket.class) {
			return BrowserWebsocket.instance;
		} 
	}
}