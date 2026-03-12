# JGit
-keep class org.eclipse.jgit.** { *; }
-dontwarn org.eclipse.jgit.**
-dontwarn org.slf4j.**

# Keep data model classes for Gson
-keep class com.novelwriter.app.data.model.** { *; }

# Kotlin
-keep class kotlin.** { *; }
-dontwarn kotlin.**
