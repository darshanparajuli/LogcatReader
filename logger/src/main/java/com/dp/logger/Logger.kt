package com.dp.logger

import android.util.Log
import kotlin.reflect.KClass

object Logger {
    private const val DEBUG = 1
    private const val ERROR = 2
    private const val INFO = 3
    private const val VERBOSE = 4
    private const val WARNING = 5
    private const val WTF = 6

    private var sTag = "MyDeviceLogs"

    fun init(tag: String) {
        sTag = tag
    }

    private fun KClass<*>.name(): String = simpleName ?: "N/A"

    fun logDebug(type: KClass<*>, msg: String) {
        logDebug(type.name(), msg)
    }

    fun logError(type: KClass<*>, msg: String) {
        logError(type.name(), msg)
    }

    fun logInfo(type: KClass<*>, msg: String) {
        logInfo(type.name(), msg)
    }

    fun logVerbose(type: KClass<*>, msg: String) {
        logVerbose(type.name(), msg)
    }

    fun logWarning(type: KClass<*>, msg: String) {
        logWarning(type.name(), msg)
    }

    fun logWtf(type: KClass<*>, msg: String) {
        logWtf(type.name(), msg)
    }

    fun logDebug(tag: String, msg: String) {
        log(DEBUG, "[$tag] $msg")
    }

    fun logError(tag: String, msg: String) {
        log(ERROR, "[$tag] $msg")
    }

    fun logInfo(tag: String, msg: String) {
        log(INFO, "[$tag] $msg")
    }

    fun logVerbose(tag: String, msg: String) {
        log(VERBOSE, "[$tag] $msg")
    }

    fun logWarning(tag: String, msg: String) {
        log(WARNING, "[$tag] $msg")
    }

    fun logWtf(tag: String, msg: String) {
        log(WTF, "[$tag] $msg")
    }

    private fun log(type: Int, msg: String) {
        when (type) {
            DEBUG -> Log.d(sTag, msg)
            ERROR -> Log.e(sTag, msg)
            INFO -> Log.i(sTag, msg)
            VERBOSE -> Log.v(sTag, msg)
            WARNING -> Log.w(sTag, msg)
            WTF -> Log.wtf(sTag, msg)
        }
    }
}