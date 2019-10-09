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
import com.thorstenmarx.webtools.api.cache.CacheLayer;
import com.thorstenmarx.webtools.api.cache.Expirable;
import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author marx
 */
public class LocalCacheLayer implements CacheLayer {

	private Cache<String, Expirable> cache;
	
	public LocalCacheLayer () {
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
	}
	
	@Override
	public <T extends Serializable> void add (final String key, final T value, final long duration, final TimeUnit unit) {
		Expirable cache_value = new Expirable(value, unit.toNanos(duration));
		cache.put(key, cache_value);
	}

	@Override
	public <T extends Serializable> Optional<T> get(final String key, final Class<T> clazz) {
		Expirable ifPresent = cache.getIfPresent(key);
		if (ifPresent != null && clazz.isInstance(ifPresent.getValue())){
			return Optional.of(clazz.cast(ifPresent.getValue()));
		}
		
		return Optional.empty();
	}

	@Override
	public boolean exists(final String key) {
		return cache.getIfPresent(key) != null;
	}
	
}
