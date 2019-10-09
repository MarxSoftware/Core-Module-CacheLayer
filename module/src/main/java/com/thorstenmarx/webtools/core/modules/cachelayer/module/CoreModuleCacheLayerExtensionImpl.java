package com.thorstenmarx.webtools.core.modules.cachelayer.module;

import com.thorstenmarx.modules.api.annotation.Extension;
import com.thorstenmarx.webtools.api.cache.CacheLayer;
import com.thorstenmarx.webtools.api.extensions.core.CoreCacheLayerExtension;

/**
 *
 * @author marx
 */
@Extension(CoreCacheLayerExtension.class)
public class CoreModuleCacheLayerExtensionImpl extends CoreCacheLayerExtension {

	@Override
	public String getName() {
		return "CoreModule Configuration";
	}

	@Override
	public CacheLayer getCacheLayer() {
		return CoreModuleCacheLayerModuleLifeCycle.cachelayer;
	}

	@Override
	public void init() {
	}
	
}
