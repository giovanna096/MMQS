/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016 FENECON GmbH and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *   FENECON GmbH - initial API and implementation and initial documentation
 *******************************************************************************/
package io.openems.impl.controller.api.rest;

import org.restlet.Component;
import org.restlet.data.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.exception.OpenemsException;
import io.openems.core.utilities.api.ApiWorker;
/**
 *
 * @author FENECON GmbH
 *
 */
public class ComponentSingleton {

	private static Component instance = null;
	private static Integer port = null;

	private final static Logger log = LoggerFactory.getLogger(ComponentSingleton.class);

	protected static Component getComponent(ConfigChannel<Integer> portt, ApiWorker apiWorkerr) throws OpenemsException {
		synchronized (ComponentSingleton.class){
		if (portt.valueOptional().isPresent()) {
			return getComponent(portt.valueOptional().get(), apiWorkerr);
		}
		throw new OpenemsException("Unable to start REST-Api: port is not set");
		}
	}

	protected static Component getComponent(int portt, ApiWorker apiWorkerr) throws OpenemsException {
		synchronized (ComponentSingleton.class){
		if (ComponentSingleton.instance != null
				&& (ComponentSingleton.port == null || ComponentSingleton.port != portt)) {
			// port changed -> restart
			ComponentSingleton.restartComponent(portt, apiWorkerr);
		}
		
		if (ComponentSingleton.instance == null) {
			// instance not available -> start
			startComponent(portt, apiWorkerr);
		}
		
		return ComponentSingleton.instance;
	}}

	protected static void restartComponent(int portt, ApiWorker apiWorkerr) throws OpenemsException {
		synchronized (ComponentSingleton.class){
			stopComponent();
		
		startComponent(portt, apiWorkerr);}
	}

	private static void startComponent(int portt, ApiWorker apiWorkerr) throws OpenemsException {
		synchronized (ComponentSingleton.class){
		ComponentSingleton.instance = new Component();
		ComponentSingleton.instance.getServers().add(Protocol.HTTP, portt);
		ComponentSingleton.instance.getDefaultHost().attach("/rest", new RestApiApplication(apiWorkerr));
		try {
			ComponentSingleton.instance.start();
			ComponentSingleton.port = portt;
			log.info("REST-Api started on port [" + portt + "].");
		} catch (Exception e) {
			throw new OpenemsException("REST-Api failed on port [" + portt + "].", e);
		}
	}}

	protected static void stopComponent() {
		if (ComponentSingleton.instance != null) {
			try {
				ComponentSingleton.instance.stop();
				log.error("REST-Api stopped.");
			} catch (Exception e) {
				log.error("REST-Api failed to stop.", e);
			}
			ComponentSingleton.instance = null;
			ComponentSingleton.port = null;
		}
	}
}
