# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/ajinkyabadve/Library/Android/sdk/tools/proguard/proguard-android-optimize.txt

# One of this app's JVM-shared dependencies references slf4j's optional binder lookup (a
# soft/reflective dependency by design - slf4j falls back to a no-op logger when it's absent,
# which is exactly what happens on Android since no slf4j binder is ever on this classpath).
# Confirmed via R8's own generated composeApp/build/outputs/mapping/release/missing_rules.txt
# when first enabling minification (2026-09-12) - not a real crash risk, just R8 being unable to
# prove that at compile time.
-dontwarn org.slf4j.impl.StaticLoggerBinder

# kotlinx.serialization: the serializer for a @Serializable class is looked up by reflection at
# runtime (Class.forName on a generated "$serializer" companion) - R8 can't see that reference
# statically and will strip the generated serializer classes without this.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.ajinkyabadve.kmmmywatchlist.**$$serializer { *; }
-keepclassmembers class com.ajinkyabadve.kmmmywatchlist.** {
    *** Companion;
}
-keepclasseswithmembers class com.ajinkyabadve.kmmmywatchlist.** {
    kotlinx.serialization.KSerializer serializer(...);
}
