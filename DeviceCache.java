package io.openems.backend.timedata.influx;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
/**
 *
 * @author FENECON GmbH
 *
 */
public class DeviceCache {
	private long timestamp = 0l;
	private final Map<String, Object> channelValueCache = new HashMap<>();

	public final Optional<Object> getChannelValueOpt(String address) {
		synchronized (this) {
			return Optional.ofNullable(this.channelValueCache.get(address));
		}
	}

	public final Set<Entry<String, Object>> getChannelCacheEntries() {
		synchronized (this) {
			return this.channelValueCache.entrySet();
		}
	}

	/**
	 * Adds the channel value to the cache
	 *
	 * @param channel
	 * @param timestamp
	 * @param value
	 */
	public void putToChannelCache(String address, Object value) {
		synchronized (this) {
			this.channelValueCache.put(address, value);
		}
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public void clear() {
		this.channelValueCache.clear();
	}
}
