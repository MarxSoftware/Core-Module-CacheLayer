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

import com.thorstenmarx.webtools.api.cache.CacheLayer;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 * @author marx
 */
public class LocalCacheLayerNGTest {
	
	CacheLayer cache;
	
	@BeforeClass
	public void setUpClass() throws Exception {
		cache = new LocalCacheLayer();
	}

	@Test
	public void test_add_exist() {
		cache.add("test_add", "That's my dog", 5, TimeUnit.SECONDS);		
		Assertions.assertThat(cache.exists("test_add")).isTrue();
	}
	
	@Test
	public void test_expire () throws InterruptedException {
		cache.add("test_expire", "That's my dog", 5, TimeUnit.SECONDS);		
		Thread.sleep(6000l);
		Assertions.assertThat(cache.exists("test_expire")).isFalse();	
	}
	@Test
	public void test_get () throws InterruptedException {
		cache.add("test_get", "That's my dog", 5, TimeUnit.SECONDS);		
		Thread.sleep(3000l);
		Assertions.assertThat(cache.get("test_get", String.class).get()).isEqualTo("That's my dog");
	}
	
}
