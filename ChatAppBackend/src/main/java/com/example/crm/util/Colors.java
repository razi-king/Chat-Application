package com.example.crm.util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

// Neon Palette Used For Avatars, Server Icons And Group Icons
public final class Colors {
	public static final List<String> PALETTE = List.of(
			"#22d3ee", "#a78bfa", "#f472b6", "#34d399", "#fbbf24", "#60a5fa", "#fb7185", "#2dd4bf", "#c084fc", "#f97316");

	private Colors() {
	}

	public static String random() {
		return PALETTE.get(ThreadLocalRandom.current().nextInt(PALETTE.size()));
	}

	public static String orRandom(String color) {
		return color == null || color.isBlank() ? random() : color;
	}
}
