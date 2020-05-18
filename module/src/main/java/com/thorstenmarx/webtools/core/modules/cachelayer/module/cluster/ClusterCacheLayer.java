package com.thorstenmarx.webtools.core.modules.cachelayer;

/*-
 * #%L
 * webtools-manager
 * %%
 * Copyright (C) 2016 - 2019 Thorsten Marx
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.google.gson.Gson;
import com.thorstenmarx.webtools.api.cache.CacheLayer;
import com.thorstenmarx.webtools.api.cache.Expirable;
import com.thorstenmarx.webtools.api.cluster.ClusterMessageAdapter;
import com.thorstenmarx.webtools.api.cluster.ClusterService;
import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author marx
 */
public class ClusterCacheLayer implements CacheLayer, ClusterMessageAdapter<String> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCacheLayer.class);

	private Cache<String, Expirable> cache;

	private static final String CACHE_TYPE = "cache_type";
	private static final String CACHE_ADD = "cache_add";
	private static final String CACHE_INVALIDATE = "cache_invalidate";

	Gson gson = new Gson();

	final ClusterService cluster;

	public ClusterCacheLayer(final ClusterService cluster) {
		this.cluster = cluster;
		cache = Caffeine.newBuilder().expireAfter(new Expiry<String, Expirable>() {
			@Override
			public long expireAfterCreate(String key, Expirable value, long currentTime) {
				return value.getCacheTime();
			}

			@Override
			public long expireAfterUpdate(String key, Expirable value, long currentTime, long currentDuration) {
				return currentDuration;
			}

			@Override
			public long expireAfterRead(String key, Expirable value, long currentTime, long currentDuration) {
				return currentDuration;
			}
		}).build();

		cluster.registerAdpater(this);
	}

	public void close() {
	}

	@Override
	public <T extends Serializable> void add(final String key, final T value, final long duration, final TimeUnit unit) {
		Expirable cache_value = new Expirable(value, unit.toNanos(duration));
		cache.put(key, cache_value);

		Payload payload = new Payload();
		payload.type = CACHE_ADD;
		payload.key = key;
		payload.value = cache_value;
		cluster.append(CACHE_TYPE, gson.toJson(payload));
	}

	@Override
	public <T extends Serializable> Optional<T> get(final String key, final Class<T> clazz) {

		Expirable ifPresent = cache.getIfPresent(key);
		if (ifPresent != null && clazz.isInstance(ifPresent.getValue())) {
			return Optional.ofNullable(clazz.cast(ifPresent.getValue()));
		}

		return Optional.empty();
	}

	@Override
	public boolean exists(final String key) {
		return cache.getIfPresent(key) != null;
	}

	@Override
	public void invalidate(final String key) {
		Payload payload = new Payload();
		payload.type = CACHE_INVALIDATE;
		payload.key = key;
		cluster.append(CACHE_TYPE, gson.toJson(payload));
	}

	@Override
	public Class<String> getValueClass() {
		return String.class;
	}

	@Override
	public String getType() {
		return CACHE_TYPE;
	}

	@Override
	public void reset() {
		cache.cleanUp();
	}

	@Override
	public void apply(final String value) {
		Payload payload = gson.fromJson(value, Payload.class);
		if (CACHE_ADD.equals(payload.type)) {
			cache.put(payload.key, payload.value);
		} else if (CACHE_INVALIDATE.equals(payload.type)) {
			this.invalidate(payload.key);
		}
	}

	public static class Payload {

		public String key;
		public String type;
		public Expirable value;
	}
}
