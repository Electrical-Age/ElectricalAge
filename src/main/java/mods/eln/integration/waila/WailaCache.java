package mods.eln.integration.waila;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.nodepackets.NodePacket;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class WailaCache {
	public static LoadingCache<Coordonate, Map<String, String>> wailaCache = CacheBuilder.newBuilder()
			.maximumSize(1000)
			.expireAfterWrite(1, TimeUnit.MINUTES)
			.build(
					new CacheLoader<Coordonate, Map<String,String>>() {
						public Map<String, String> load(Coordonate key) throws Exception {
							Eln.achNetwork.sendToServer(new NodePacket(key));
							return null;
						}
					});
}
