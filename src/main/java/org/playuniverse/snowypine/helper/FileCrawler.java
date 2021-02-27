package org.playuniverse.snowypine.helper;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;

public final class FileCrawler {

	private FileCrawler() {}

	public static UUID[] crawlAsArray(File folder) {
		return crawl(folder).toArray(new UUID[0]);
	}

	public static ArrayList<UUID> crawl(File folder) {
		if (!folder.exists()) {
			return new ArrayList<>();
		}
		File[] files = folder.listFiles();
		ArrayList<UUID> uniqueIds = new ArrayList<>();
		for (File file : files) {
			UUID uniqueId;
			try {
				uniqueId = UUID.fromString(file.getName().substring(0, file.getName().length() - 4));
			} catch (IllegalArgumentException ignore) {
				continue;
			}
			uniqueIds.add(uniqueId);
		}
		return uniqueIds;
	}

}
