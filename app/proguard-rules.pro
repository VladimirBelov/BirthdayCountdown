# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Сохраняем имена файлов и номера строк для красивых стектрейсов в краш-репортах
-keepattributes SourceFile,LineNumberTable

# Критично для AndroidX и Material Components (точка входа в приложение)
-keep class androidx.core.app.CoreComponentFactory { *; }

# ==========================================
# ML Kit, Firebase Components и R8
# ==========================================

# 1. ВАЖНО: ComponentRegistrar - это ИНТЕРФЕЙС, поэтому используем 'implements'!
# Сохраняем конструкторы и метод getComponents, чтобы DI-контейнер смог их найти.
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    public <init>(...);
    public java.util.List getComponents();
}

# 2. Сохраняем внутренние классы ML Kit и bundled модели.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_segmentation_bundled.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_segmentation.** { *; }

# 3. Сохраняем метаданные и аннотации, которые ML Kit ищет через Reflection при старте
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# 4. Инициализаторы (ContentProvider, через который ML Kit стартует)
-keep class com.google.mlkit.common.internal.MlKitInitProvider { *; }

# 5. Защита методов, помеченных для Google Play SDK
-keepclassmembers class * {
    @com.google.android.gms.common.annotation.KeepForSdk *;
}

-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.internal.mlkit_vision_segmentation.**
-dontwarn com.google.android.gms.internal.mlkit_vision_segmentation_bundled.**

#end