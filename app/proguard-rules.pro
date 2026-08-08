# Protobuf-lite: only keep field names, not entire classes.
# GeneratedMessageLite.dynamicMethod accesses fields by name,
# so renaming them (qn_ -> a) breaks deserialization.
# Let R8 still remove unused proto classes via normal reachability.
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# Keep only runtime entry points used by generated Lite messages and the app.
-keep class com.google.protobuf.GeneratedMessageLite { *; }
-keep class com.google.protobuf.ByteString { *; }
-keep class com.google.protobuf.Parser { *; }
-keep class com.google.protobuf.MessageLite { *; }
-keep class com.google.protobuf.Any { *; }
-keep class com.google.protobuf.Empty { *; }
-dontwarn com.google.protobuf.**

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**