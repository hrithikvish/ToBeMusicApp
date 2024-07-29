package com.hrithikvish.apitask2.util

import android.util.Log

class Utils {

    companion object {

        // https://stackoverflow.com/a/25734136/14400320
        fun largeLog(tag: String = "tesstt", content: String) {
            if (content.length > 4000) {
                Log.d(tag, content.substring(0, 4000))
                largeLog(tag, content.substring(4000))
            } else {
                Log.d(tag, content)
            }
        }
    }
}