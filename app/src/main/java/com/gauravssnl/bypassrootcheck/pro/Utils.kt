package com.gauravssnl.bypassrootcheck.pro

object Utils {
    @JvmStatic
    fun filePathEqualsOrEndsWith(filePath: String, pattern: String): Boolean {
        return filePath.contentEquals(pattern) || filePath.endsWith(pattern);
    }
}