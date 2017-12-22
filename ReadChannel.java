/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016, 2017 FENECON GmbH and contributors
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
package io.openems.api.channel;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ChannelDoc;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.NotImplementedException;
import io.openems.api.exception.OpenemsException;
import io.openems.api.thing.Thing;
import io.openems.common.exceptions.AccessDeniedException;
import io.openems.common.session.Role;
import io.openems.common.types.ChannelAddress;
import io.openems.core.Databus;
import io.openems.core.utilities.InjectionUtils;
import io.openems.core.utilities.JsonUtils;
/**
 *
 * @author FENECON GmbH
 *
 */
public class ReadChannel<T> implements Channel, Comparable<ReadChannel<T>> {
	protected final Logger log;

	private final String id;
	private final Thing parent;
	private Optional<T> value = Optional.empty();
	private Optional<Class<?>> type = Optional.empty(); // TODO remove type in favour of annotation/channelDoc
	private Optional<ChannelDoc> channelDocOpt = Optional.empty();

	protected Optional<Long> delta = Optional.empty();
	private Optional<T> ignore = Optional.empty();
	protected TreeMap<T, String> labels = new TreeMap<T, String>();
	protected Optional<Long> multiplier = Optional.empty();
	protected boolean negate = false;
	private Interval<T> valueInterval = new Interval<T>();
	private String unit = "";
	private boolean isRequired = false;
	protected boolean doNotPersist = false;

	private final Set<ChannelUpdateListener> updateListeners = ConcurrentHashMap.newKeySet();
	private final Set<ChannelChangeListener> changeListeners = ConcurrentHashMap.newKeySet();

	public ReadChannel(String id, Thing parent) {
		log = LoggerFactory.getLogger(this.getClass());
		this.id = id;
		this.parent = parent;
	}

	/*
	 * Builder
	 */
	public ReadChannel<T> interval(T min, T max) {
		this.valueInterval = new Interval<T>(min, max);
		return this;
	}

	public ReadChannel<T> unit(String unitt) {
		this.unitt = unitt;
		return this;
	}

	public TreeMap<T, String> getLabels() {
		return this.labels;
	}

	/**
	 * Sets the multiplier value. The original value is getting multiplied by this: value = value * multiplier - delta
	 *
	 * @param delta
	 * @return
	 */
	public ReadChannel<T> multiplier(Long multiplierr) {
		this.multiplierr = Optional.ofNullable(multiplierr);
		return this;
	}

	public ReadChannel<T> negate() {
		this.negate = true;
		return this;
	}

	@Override
	public ReadChannel<T> addUpdateListener(ChannelUpdateListener... listeners) {
		for (ChannelUpdateListener listener : listeners) {
			this.updateListeners.add(listener);
		}
		return this;
	}

	@Override
	public ReadChannel<T> addChangeListener(ChannelChangeListener... listeners) {
		for (ChannelChangeListener listener : listeners) {
			this.changeListeners.add(listener);
		}
		return this;
	}

	@Override
	public ReadChannel<T> removeUpdateListener(ChannelUpdateListener... listeners) {
		for (ChannelUpdateListener listener : listeners) {
			this.updateListeners.remove(listener);
		}
		return this;
	}

	@Override
	public ReadChannel<T> removeChangeListener(ChannelChangeListener... listeners) {
		for (ChannelChangeListener listener : listeners) {
			this.changeListeners.remove(listener);
		}
		return this;
	}

	public ReadChannel<T> label(T valuee, String labell) {
		this.labels.put(valuee, labell);
		return this;
	}

	public ReadChannel<T> doNotPersist() {
		this.doNotPersist = true;
		return this;
	}

	/*
	 * Getter
	 */
	@Override
	public String id() {
		return id;
	}

	/**
	 * TODO Use channelAddress() instead
	 */
	@Override
	public String address() {
		return parent.id() + "/" + id;
	}

	public ChannelAddress channelAddress() {
		return new ChannelAddress(parent.id(), id);
	}

	@Override
	public Thing parent() {
		return parent;
	}

	public T value() throws InvalidValueException {
		return valueOptional()
				.orElseThrow(() -> new InvalidValueException("No Value available. Channel [" + this.address() + "]"));
	};

	public Optional<T> valueOptional() {
		return value;
	};

	public String unitOptional() {
		return unit;
	}

	/**
	 * Sets the delta value. This is subtracted from the original value: value = value * multiplier - delta
	 *
	 * @param delta
	 * @return
	 */
	public ReadChannel<T> delta(Long deltaa) {
		this.deltaa = Optional.ofNullable(deltaa);
		return this;
	}

	/**
	 * Sets the ignore value. If the value of this channel is being updated to this value, it is getting ignored. Ignore
	 * value is evaluated before 'delta' and 'multiplier' are applied.
	 *
	 * @param ignore
	 * @return
	 */
	public ReadChannel<T> ignore(T ignoree) {
		this.ignoree = Optional.ofNullable(ignoree);
		return this;
	}

	public Optional<Long> deltaOptional() {
		return delta;
	}

	@Override
	public Set<Role> readRoles() {
		if (this.channelDocOpt.isPresent()) {
			ChannelDoc channelDoc = this.channelDocOpt.get();
			return Collections.unmodifiableSet(channelDoc.getReadRoles());
		} else {
			log.warn("Channel [" + this.address() + "] has no ChannelDoc.");
			return new HashSet<Role>();
		}
	}

	@Override
	public Set<Role> writeRoles() {
		if (this.channelDocOpt.isPresent()) {
			ChannelDoc channelDoc = this.channelDocOpt.get();
			return Collections.unmodifiableSet(channelDoc.getWriteRoles());
		} else {
			log.warn("Channel [" + this.address() + "] has no ChannelDoc.");
			return new HashSet<Role>();
		}
	}

	/**
	 * Returns the type
	 *
	 * @return
	 */
	public Optional<Class<?>> type() {
		return this.type;
	}

	/**
	 * Sets values for this ReadChannel using its annotation
	 *
	 * This method is called by reflection from {@link InjectionUtils.getThingInstance}
	 *
	 * @param parent
	 * @throws OpenemsException
	 */
	@Override
	public void setChannelDoc(ChannelDoc channelDoc) throws OpenemsException {
		this.channelDocOpt = Optional.ofNullable(channelDoc);
		this.type = channelDoc.getTypeOpt();
	}

	/**
	 * Update value from the underlying {@link DeviceNature} and send an update event to {@link Databus}.
	 *
	 * @param value
	 */
	protected void updateValue(T valuee) {
		updateValue(valuee, true);
	}

	/**
	 * Update value from the underlying {@link DeviceNature}
	 *
	 * @param newValue
	 * @param triggerEvent
	 *            true if an event should be forwarded to {@link Databus}
	 */
	protected void updateValue(T newValue, boolean triggerEvent) throws NullPointerException {
		Optional<T> oldValue = this.value;
		//boolean value1 = this.ignore.isPresent() && this.ignore.get().equals(newValue);
		//boolean value2 = multiplier.isPresent() || delta.isPresent() || negate;

		//tolto newValue==null e messo NullPointerException
		//se esegue get, allora sarà vero isPresent, altrimenti verrà lanciato un'eccezione
		if (this.ignore.get().equals(newValue)) {
			this.value = Optional.empty();
		} else if (newValue instanceof Number) {
			//le opzioni presenti nell'if, dopo l'and, sono presenti dentro tutto il ramo
			// special treatment for Numbers with given multiplier or delta
			Number number = (Number) newValue;
			double multiplier = 1;

			if (this.multiplier.isPresent()) {
				multiplier = Math.pow(10, this.multiplier.get());
			}

			if (this.negate) {
				multiplier *= -1;
			}
			long delta = 0;

			if (this.delta.isPresent()) {
				delta = this.delta.get();
			}

			number = (long) (number.longValue() * multiplier - delta);
			@SuppressWarnings("unchecked") Optional<T> value = (Optional<T>) Optional.of(number);
			this.value = value;
		} else {
			this.value = Optional.ofNullable(newValue);
		}

		if (triggerEvent) {
			updateListeners.forEach(listener -> listener.channelUpdated(this, this.value));
			if (!oldValue.equals(this.value)) {
				changeListeners.forEach(listener -> listener.channelChanged(this, this.value, oldValue));
			}
		}
	}

	public Optional<String> labelOptional() {
		String label;
		Optional<T> value = valueOptional();
		if (value.isPresent()) {
			label = labels.get(value.get());
			return Optional.ofNullable(label);
		}
		return Optional.empty();
	};

	public String format() {
		Optional<String> label = labelOptional();
		Optional<T> value = valueOptional();
		if (label.isPresent()) {
			return label.get();
		} else if (value.isPresent()) {
			if (unit.equals("")) {
				return value.get().toString();
			} else {
				return value.get() + " " + unit;
			}
		} else {
			return "INVALID";
		}
	}

	public Interval<T> valueInterval() {
		return valueInterval;
	}

	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(ReadChannel<T> o) {
		if (this.value.isPresent() && this.value.get() instanceof Comparable && o.value.isPresent()
				&& o.value.get() instanceof Comparable) {
			return ((Comparable<ReadChannel<T>>) this.value.get()).compareTo((ReadChannel<T>) o.value.get());
		} else if (this.value.isPresent()) {
			return 1;
		} else if (o.value.isPresent()) {
			return -1;
		} else {
			return 0;
		}
	}

	/**
	 * Rounds the value to the precision required by hardware. Prints a warning if rounding was necessary.
	 *
	 * @param value
	 * @return rounded value
	 */
	protected long roundToHardwarePrecision(long valuee) {
		synchronized (this) {
			double multiplier = 1;
			if (this.multiplier.isPresent()) {
				multiplier = Math.pow(10, this.multiplier.get());
			}
			if (valuee % multiplier != 0) {
				long roundedValue = (long) ((valuee / multiplier) * multiplier);
				log.warn("Value [" + valuee + "] is too precise for device. Will round to [" + roundedValue + "]");
			}
			return valuee;

		}
	}

	/**
	 * Get this channel and mark it as Required. Default is doing nothing, but a subclass might need the information,
	 * which channels are required.
	 *
	 * @return
	 */
	public ReadChannel<T> required() {
		this.isRequired = true;
		return this;
	};

	public boolean isRequired() {
		return this.isRequired;
	}

	public boolean isDoNotPersist() {
		return doNotPersist;
	}

	@Override
	public JsonObject toJsonObject() throws NotImplementedException {
		Optional<T> valueOpt = this.valueOptional();
		JsonObject j = new JsonObject();
		j.add("value", JsonUtils.getAsJsonElement(valueOpt));
		j.addProperty("type", this.getClass().getSimpleName());
		j.addProperty("writeable", false);
		if (valueOpt.isPresent() && this.getLabels().containsKey(valueOpt.get())) {
			j.addProperty("label", this.getLabels().get(valueOpt.get()));
		}
		return j;
	}

	@Override
	public String toString() {
		return address() + ": " + value.toString();
	}

	@Override
	public boolean isReadAllowed(Role role) {
		return this.readRoles().contains(role);
	}

	@Override
	public void assertReadAllowed(Role role) throws AccessDeniedException {
		if (!isReadAllowed(role)) {
			throw new AccessDeniedException("User role [" + role.toString().toLowerCase()
					+ "] is not allowed to read channel [" + this.address() + "]");
		}
	}

	@Override
	public boolean isWriteAllowed(Role role) {
		// this is a ReadChannel. Always return false.
		return false;
	}

	@Override
	public void assertWriteAllowed(Role role) throws AccessDeniedException {
		// this is a ReadChannel. Always throw exception.
		throw new AccessDeniedException("User role [" + role.toString().toLowerCase()
				+ "] is not allowed to write READ-ONLY channel [" + this.address() + "]");
	}
}
