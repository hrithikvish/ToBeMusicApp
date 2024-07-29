package com.hrithikvish.apitask2

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

class XmlPullParserHandler {

    fun parse(inputStream: InputStream): List<Lyrics> {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        val transcriptSegments = mutableListOf<Lyrics>()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "text") {
                        val start = parser.getAttributeValue(null, "start")?.toDoubleOrNull() ?: 0.0
                        val dur = parser.getAttributeValue(null, "dur")?.toDoubleOrNull() ?: 0.0
                        var text = ""
                        while (true) {
                            eventType = parser.next()
                            if (eventType == XmlPullParser.TEXT) {
                                text += parser.text
                            } else if (eventType == XmlPullParser.END_TAG && parser.name == "text") {
                                break
                            }
                        }
                        transcriptSegments.add(Lyrics(start, dur, text))
                    }
                }
            }
            eventType = parser.next()
        }

        return transcriptSegments
    }
}