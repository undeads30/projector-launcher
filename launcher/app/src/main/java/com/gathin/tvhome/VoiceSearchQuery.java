package com.gathin.tvhome;

import java.util.Locale;

final class VoiceSearchQuery {
    static VoiceCommand command(String freeSpeech,VoiceCommand constrained) {
        return VoiceCommands.parse(freeSpeech)==constrained ? constrained : VoiceCommand.NONE;
    }
    static String extract(String speech) {
        if (speech == null) return "";
        String text = speech.toLowerCase(Locale.ROOT).replace('ё','е').trim()
                .replaceAll("\\s+", " ").replaceAll("[.!?,]+$", "");
        if (!text.startsWith("найди ")) return "";
        String query = text.substring(6).trim();
        if (query.equals("фильм") || query.equals("видео")) return "";
        if (query.startsWith("фильм ") || query.startsWith("видео ")) query = query.substring(6).trim();
        return query.isEmpty() || query.length() > 200 ? "" : query;
    }
    static String destination(String pkg) {
        return "mov.film".equals(pkg) || "com.google.android.youtube.tv".equals(pkg) || "com.vk.tv".equals(pkg) ? pkg : "";
    }
}
