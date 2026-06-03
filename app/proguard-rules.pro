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

#end