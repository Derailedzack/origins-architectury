package io.github.apace100.origins;

import io.github.apace100.origins.power.factory.condition.EntityConditionsServer;
import net.fabricmc.api.DedicatedServerModInitializer;

public class OriginsServer {

	public static void register() {
		EntityConditionsServer.register();
	}
}
