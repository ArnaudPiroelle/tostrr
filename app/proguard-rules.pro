# R8 rules of the release build, on top of the ones the libraries ship (kotlinx.serialization, Room, WorkManager,
# Koin, Glance, OkHttp) and those AAPT derives from the manifest.

# The back stack is saved with each key's class name and restored by looking that class up again.
-keep class app.tostrr.ui.navigation.Screen { *; }
-keep class app.tostrr.ui.navigation.Screen$* { *; }
