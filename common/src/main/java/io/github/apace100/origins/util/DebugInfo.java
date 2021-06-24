package io.github.apace100.origins.util;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.api.OriginsAPI;

public class DebugInfo {

	public static void printRegistrySizes(String at) {
		printInfo(new String[]{
				"Registry Size at " + at,
				"Origins: " + OriginsAPI.getOrigins().getEntries().size(),
				"Layers:  " + OriginsAPI.getLayers().getEntries().size(),
				"Powers:  " + OriginsAPI.getOrigins().getEntries().size()
		});
	}

	private static void printInfo(String[] lines) {
		int longest = 0;
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].length() > longest)
				longest = lines[i].length();
			lines[i] = "| " + lines[i];
		}
		StringBuilder border = new StringBuilder("+");
		for (int i = 0; i < longest + 2; i++) {
			border.append("-");
		}
		border.append("+");
		Origins.LOGGER.info(border.toString());
		for (int i = 0; i < lines.length; i++) {
			while (lines[i].length() < longest + 3)
				lines[i] += " ";
			lines[i] += "|";
			Origins.LOGGER.info(lines[i]);
		}
		Origins.LOGGER.info(border.toString());
	}
}
