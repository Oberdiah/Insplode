package com.oberdiah.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.oberdiah.d
import com.oberdiah.f

private var prefs: Preferences = Gdx.app.getPreferences("Preferences")
private var isDirty = false
private var lastSavedOn = 0.0

fun saveState() {
    if (isDirty && GameTime.APP_TIME - lastSavedOn > 5.0) {
        prefs.flush()
        isDirty = false
        lastSavedOn = GameTime.APP_TIME
    }
}

class StatefulDouble(private val key: String, defaultValue: Double) {
    var value: Double = defaultValue
        set(value) {
            if (field == value) return
            field = value
            prefs.putFloat(key, value.f)
            isDirty = true
        }

    init {
        value = prefs.getFloat(key, defaultValue.f).d
    }
}

class StatefulInt(private val key: String, defaultValue: Int) {
    var value: Int = defaultValue
        set(value) {
            if (field == value) return
            field = value
            prefs.putInteger(key, value)
            isDirty = true
        }

    init {
        value = prefs.getInteger(key, defaultValue)
    }
}

class StatefulBoolean(private val key: String, defaultValue: Boolean) {
    var value: Boolean = defaultValue
        set(value) {
            if (field == value) return
            field = value
            prefs.putBoolean(key, value)
            isDirty = true
        }

    init {
        value = prefs.getBoolean(key, defaultValue)
    }
}

class StatefulString(private val key: String, defaultValue: String) {
    var value: String = defaultValue
        set(value) {
            if (field == value) return
            field = value
            prefs.putString(key, value)
            isDirty = true
        }

    init {
        value = prefs.getString(key, defaultValue)
    }
}

/**
 * A wrapper around StatefulInt for enums.
 * Coz' reasons, you've got to pass in the values.
 */
class StatefulEnum<T : Enum<T>>(
    private val key: String,
    defaultValue: T,
    values: Array<T>
) {
    var value: T = defaultValue
        set(value) {
            if (field == value) return
            field = value
            prefs.putInteger(key, value.ordinal)
            isDirty = true
        }

    init {
        value = values[prefs.getInteger(key, defaultValue.ordinal)]
    }
}