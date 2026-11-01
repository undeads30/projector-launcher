# Shrink and optimise, but keep class and method names: the size win comes from dropping unused
# AndroidX code, not from renaming, and readable stack traces matter more in a sideloaded app
# where crash reports arrive as screenshots.
-dontobfuscate
-keep class com.sun.jna.** { *; }
-keep class org.vosk.** { *; }
# Optional desktop-only JNA AWT helpers are unreachable on Android.
-dontwarn java.awt.**
