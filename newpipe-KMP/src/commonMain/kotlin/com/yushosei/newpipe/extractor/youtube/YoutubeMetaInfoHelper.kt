package com.yushosei.newpipe.extractor.youtube

import io.ktor.http.Url
import com.yushosei.newpipe.extractor.MetaInfo
import com.yushosei.newpipe.extractor.exceptions.ParsingException
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.extractCachedUrlIfNeeded
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.getTextFromObject
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.getTextFromObjectOrThrow
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.getUrlFromNavigationEndpoint
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.isGoogleURL
import com.yushosei.newpipe.extractor.stream.Description
import com.yushosei.newpipe.extractor.utils.Utils
import com.yushosei.newpipe.nanojson.JsonArray
import com.yushosei.newpipe.nanojson.JsonObject

internal object YoutubeMetaInfoHelper {
    
    fun getMetaInfo(contents: JsonArray): List<MetaInfo> {
        val metaInfo: MutableList<MetaInfo> = ArrayList()
        for (content in contents) {
            val resultObject = content as JsonObject
            if (resultObject.has("itemSectionRenderer")) {
                for (sectionContentObject
                in resultObject.getObject("itemSectionRenderer")!!.getArray("contents")) {
                    val sectionContent = sectionContentObject as JsonObject
                    if (sectionContent.has("infoPanelContentRenderer")) {
                        metaInfo.add(
                            getInfoPanelContent(
                                sectionContent.getObject("infoPanelContentRenderer")!!
                            )
                        )
                    }
                    if (sectionContent.has("clarificationRenderer")) {
                        metaInfo.add(
                            getClarificationRenderer(
                                sectionContent.getObject("clarificationRenderer")!!
                            )
                        )
                    }
                    if (sectionContent.has("emergencyOneboxRenderer")) {
                        getEmergencyOneboxRenderer(
                            sectionContent.getObject("emergencyOneboxRenderer")
                        ) {
                            metaInfo.add(it)
                        }
                    }
                }
            }
        }
        return metaInfo
    }


    
    private fun getInfoPanelContent(infoPanelContentRenderer: JsonObject): MetaInfo {
        val metaInfo = MetaInfo()
        val sb = StringBuilder()
        for (paragraph in infoPanelContentRenderer.getArray("paragraphs")) {
            if (sb.length != 0) {
                sb.append("<br>")
            }
            sb.append(getTextFromObject((paragraph as JsonObject)))
        }
        metaInfo.content = Description(
            sb.toString(),
            Description.HTML
        )
        if (infoPanelContentRenderer.has("sourceEndpoint")) {
            val metaInfoLinkUrl = getUrlFromNavigationEndpoint(
                infoPanelContentRenderer.getObject("sourceEndpoint")
            )
            try {
                metaInfo.addUrl(
                    Url(
                        extractCachedUrlIfNeeded(metaInfoLinkUrl)!!
                    )
                )
            } catch (e: NullPointerException) {
                throw ParsingException("Could not get metadata info URL", e)
            } catch (e: IllegalArgumentException) {  // Url() 생성자가 이 예외 던짐
                throw ParsingException("Invalid URL format: $metaInfoLinkUrl", e)
            }

            val metaInfoLinkText = getTextFromObject(
                infoPanelContentRenderer.getObject("inlineSource")!!
            )
            requireNotNull(metaInfoLinkText) {
                ParsingException("Could not get metadata info link text.")
            }
            metaInfo.addUrlText(metaInfoLinkText)
        }

        return metaInfo
    }


    
    private fun getClarificationRenderer(
        clarificationRenderer: JsonObject
    ): MetaInfo {
        val metaInfo = MetaInfo()

        val title = getTextFromObject(
            clarificationRenderer
                .getObject("contentTitle")!!
        )
        val text = getTextFromObject(
            clarificationRenderer
                .getObject("text")!!
        )
        if (title == null || text == null) {
            throw ParsingException("Could not extract clarification renderer content")
        }
        metaInfo.title = title
        metaInfo.content =
            Description(
                text,
                Description.PLAIN_TEXT
            )

        if (clarificationRenderer.has("actionButton")) {
            val actionButton = clarificationRenderer.getObject("actionButton")!!
                .getObject("buttonRenderer")
            try {
                val url = getUrlFromNavigationEndpoint(
                    actionButton
                    !!.getObject("command")!!
                )
                metaInfo.addUrl(Url(extractCachedUrlIfNeeded(url)!!))
            } catch (e: NullPointerException) {
                throw ParsingException("Could not get metadata info URL", e)
            } catch (e: IllegalArgumentException) {
                throw ParsingException("Could not get metadata info URL", e)
            }

            val metaInfoLinkText = getTextFromObject(
                actionButton!!.getObject("text")!!
            )
            requireNotNull(metaInfoLinkText) {
                ParsingException("Could not get metadata info link text.")
            }
            metaInfo.addUrlText(metaInfoLinkText)
        }

        if (clarificationRenderer.has("secondaryEndpoint") && clarificationRenderer
                .has("secondarySource")
        ) {
            val url = getUrlFromNavigationEndpoint(
                clarificationRenderer
                    .getObject("secondaryEndpoint")!!
            )
            // Ignore Google URLs, because those point to a Google search about "Covid-19"
            if (url != null && !isGoogleURL(url)) {
                try {
                    metaInfo.addUrl(Url(url))
                    val description = getTextFromObject(
                        clarificationRenderer
                            .getObject("secondarySource")!!
                    )
                    metaInfo.addUrlText(description ?: url)
                } catch (e: IllegalArgumentException) {
                    throw ParsingException("Could not get metadata info secondary URL", e)
                }
            }
        }

        return metaInfo
    }

    
    private fun getEmergencyOneboxRenderer(
        emergencyOneboxRenderer: JsonObject,
        addMetaInfo: (MetaInfo) -> Unit
    ) {
        val supportRenderers = emergencyOneboxRenderer.values
            .filterIsInstance<JsonObject>()
            .filter { it.has("singleActionEmergencySupportRenderer") }
            .map { it.getObject("singleActionEmergencySupportRenderer") }

        if (supportRenderers.isEmpty()) {
            throw ParsingException("Could not extract any meta info from emergency renderer")
        }

        for (r in supportRenderers) {
            val metaInfo = MetaInfo()

            // usually an encouragement like "We are with you"
            val title = getTextFromObjectOrThrow(r!!.getObject("title")!!, "title")

            // usually a phone number
            val action: String // this variable is expected to start with "\n"
            if (r.has("actionText")) {
                action = """
                    
                    ${getTextFromObjectOrThrow(r!!.getObject("actionText")!!, "action")}
                    """.trimIndent()
            } else if (r.has("contacts")) {
                val contacts = r.getArray("contacts")
                val stringBuilder = StringBuilder()
                // Loop over contacts item from the first contact to the last one
                for (i in contacts.indices) {
                    stringBuilder.append("\n")
                    stringBuilder.append(
                        getTextFromObjectOrThrow(
                            contacts.getObject(i)
                                .getObject("actionText")!!, "contacts.actionText"
                        )
                    )
                }
                action = stringBuilder.toString()
            } else {
                action = ""
            }

            // usually details about the phone number
            val details = getTextFromObjectOrThrow(r.getObject("detailsText")!!, "details")

            // usually the name of an association
            val urlText = getTextFromObjectOrThrow(
                r.getObject("navigationText")!!,
                "urlText"
            )

            metaInfo.title = title
            metaInfo.content = Description(details + action, Description.PLAIN_TEXT)
            metaInfo.addUrlText(urlText)

            // usually the webpage of the association
            val url = getUrlFromNavigationEndpoint(r.getObject("navigationEndpoint"))
                ?: throw ParsingException("Could not extract emergency renderer url")

            try {
                metaInfo.addUrl(Url(Utils.replaceHttpWithHttps(url)))
            } catch (e: IllegalArgumentException) {
                throw ParsingException("Could not parse emergency renderer url", e)
            }

            addMetaInfo(metaInfo)
        }
    }
}
