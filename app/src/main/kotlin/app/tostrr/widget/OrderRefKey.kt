package app.tostrr.widget

import androidx.datastore.preferences.core.stringPreferencesKey

/** Which order a widget instance shows; stored in its Glance state, shared by both widgets and their config screen. */
val OrderRefKey = stringPreferencesKey("order_reference")
