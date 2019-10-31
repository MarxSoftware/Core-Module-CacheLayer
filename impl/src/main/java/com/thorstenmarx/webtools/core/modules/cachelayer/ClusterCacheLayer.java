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
import com.thorstenmarx.webtools.api.cluster.Cluster;
import com.thorstenmarx.webtools.api.cluster.Message;
import com.thorstenmarx.webtools.api.cluster.services.MessageService;
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
public class ClusterCacheLayer implements CacheLayer, MessageService.MessageListener {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCacheLayer.class);

	private Cache<String, Expirable> cache;

	private static final String CACHE_ADD = "cache_add";
	private static final String CACHE_INVALIDATE = "cache_INVALIDATE";
	
	Gson gson = new Gson();
	
	final Cluster cluster;
	
	public ClusterCacheLayer(final Cluster cluster) {
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

		cluster.getMessageService().registerMessageListener(this);
	}
	
	public void close () {
		cluster.getMessageService().unregisterMessageListener(this);
	}

	@Override
	public <T extends Serializable> void add(final String key, final T value, final long duration, final TimeUnit unit) {
		Expirable cache_value = new Expirable(value, unit.toNanos(duration));
//		cache.put(key, cache_value);
		
		AddPayload payload = new AddPayload();
		payload.value = cache_value;
		
		Message message = new Message();
		message.setType(CACHE_ADD);
		message.setPayload(gson.toJson(payload));
		try {	
			cluster.getRAFTMessageService().publish(message);
		} catch (IOException ex) {
			LOGGER.error("", ex);
		}
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
//		cache.invalidate(key);

		
		InvalidatePayload payload = new InvalidatePayload();
		payload.key = key;
		
		Message message = new Message();
		message.setType(CACHE_INVALIDATE);
		message.setPayload(gson.toJson(payload));
		try {	
			cluster.getRAFTMessageService().publish(message);
		} catch (IOException ex) {
			LOGGER.error("", ex);
		}
	}

	@Override
	public void handle(Message message) {
		if (CACHE_ADD.equals(message.getType())) {
			AddPayload payloadadd = gson.fromJson(message.getPayload(), AddPayload.class);
			cache.put(payloadadd.key, payloadadd.value);
		} else if (CACHE_INVALIDATE.equals(message.getType())) {
			InvalidatePayload payload = gson.fromJson(message.getPayload(), InvalidatePayload.class);
			cache.invalidate(payload.key);
		}
	}

	public static class AddPayload {
		public String key;
		public Expirable value;
	}

	public static class InvalidatePayload {
		public String key;
	}
}
