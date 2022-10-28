package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.utils.logs.Logs;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VirtualPack {
	private final Set<String> paths;
	private final List<VirtualFile> files;

	public VirtualPack() {
		this.paths = new HashSet<>();
		this.files = new ArrayList<>();
	}

	public VirtualPack(Collection<VirtualFile> files) {
		this.paths = new HashSet<>(files.size());
		this.files = new ArrayList<>(files.size());
		addAll(files);
	}

	public boolean add(VirtualFile file) {
		String path = file.getPath();
		if (!paths.add(path)) {
			Logs.logWarning("Duplicated virtual file in pack found, Ignored! Path = " + path);
			file.close();
			return false;
		}
		files.add(file);
		return true;
	}

	public boolean addAll(Collection<VirtualFile> files) {
		boolean flag = true;
		for (VirtualFile file : files) {
			if (!add(file)) flag = false;
		}
		return flag;
	}

	public void sort() {
		Collections.sort(files);
	}

	public List<VirtualFile> getFiles() {
		return files;
	}
}
