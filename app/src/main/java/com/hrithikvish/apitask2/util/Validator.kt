package com.hrithikvish.apitask2.util

class Validator {

    companion object {

        fun isValidYouTubeUrl(url: String): Boolean {
            val youTubeRegex =
                Regex(
                    """^((?:https?:)?\/\/)?((?:www|m)\.)?((?:youtube\.com|youtu.be))(\/(?:[\w\-]+\?v=|embed\/|v\/)?)([\w\-]+)(\S+)?${'$'}""".trimIndent()
                )
            return youTubeRegex.matches(url)
        }
    }
}