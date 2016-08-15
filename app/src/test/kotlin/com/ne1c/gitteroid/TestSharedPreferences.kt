package com.ne1c.gitteroid

import android.content.SharedPreferences
import java.util.*

class TestSharedPreferences: SharedPreferences {
    private val storage: MutableMap<String, Any> = HashMap()

    override fun contains(key: String?): Boolean = storage.containsKey(key)

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return if (storage.containsKey(key)) storage[key] as Boolean else defValue
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {

    }

    override fun getInt(key: String?, defValue: Int): Int {
        return if (storage.containsKey(key)) storage[key] as Int else defValue
    }

    override fun getAll(): MutableMap<String, *>? = storage

    override fun edit(): SharedPreferences.Editor {
        val temp: MutableMap<String, Any> = HashMap()

        return object : SharedPreferences.Editor {
            override fun clear(): SharedPreferences.Editor {
                temp.clear()
                return this
            }

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                temp.put(key!!, value)
                return this
            }

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                temp.put(key!!, value)
                return this
            }

            override fun remove(key: String?): SharedPreferences.Editor {
                temp.remove(key)
                return this
            }

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                temp.put(key!!, value)
                return this
            }

            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
                temp.put(key!!, values!!)
                return this
            }

            override fun commit(): Boolean {
                storage.putAll(temp)
                return true
            }

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                temp.put(key!!, value)
                return this
            }

            override fun apply() {
                storage.putAll(temp)
            }

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                temp.put(key!!, value!!)
                return this
            }
        }
    }

    override fun getLong(key: String?, defValue: Long): Long {
        return if (storage.containsKey(key)) storage[key] as Long else defValue
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        return if (storage.containsKey(key)) storage[key] as Float else defValue
    }

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        return if (storage.containsKey(key)) storage[key] as MutableSet<String> else defValues!!
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
    }

    override fun getString(key: String?, defValue: String?): String {
        return if (storage.containsKey(key)) storage[key] as String else defValue.toString()
    }
}