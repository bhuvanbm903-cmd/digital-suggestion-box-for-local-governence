# Keep domain models and Firebase/Room entities if using reflection
-keepclassmembers class * {
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <methods>;
}
-keepattributes Signature,InnerClasses,EnclosingMethod
-keep class com.suggestbox.domain.model.** { *; }
-keep class com.suggestbox.data.** { *; }
