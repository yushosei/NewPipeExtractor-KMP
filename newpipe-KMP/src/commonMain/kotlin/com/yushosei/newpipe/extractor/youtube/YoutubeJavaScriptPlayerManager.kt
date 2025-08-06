package com.yushosei.newpipe.extractor.youtube

import com.yushosei.newpipe.extractor.exceptions.ParsingException

/**
 * Manage the extraction and the usage of YouTube's player JavaScript needed data in the YouTube
 * service.
 *
 *
 *
 * YouTube restrict streaming their media in multiple ways by requiring their HTML5 clients to use
 * a signature timestamp, and on streaming URLs a signature deobfuscation function for some
 * contents and a throttling parameter deobfuscation one for all contents.
 *
 *
 *
 *
 * This class provides access to methods which allows to get base JavaScript player's signature
 * timestamp and to deobfuscate streaming URLs' signature and/or throttling parameter of HTML5
 * clients.
 *
 */
internal object YoutubeJavaScriptPlayerManager {
    private val CACHED_THROTTLING_PARAMETERS: MutableMap<String, String> = HashMap()

    private var cachedJavaScriptPlayerCode: String? = null


    private var cachedSignatureTimestamp: Int? = null

    private var cachedSignatureDeobfuscationFunction: String? = null

    private var cachedThrottlingDeobfuscationFunctionName: String? = null

    private var cachedThrottlingDeobfuscationFunction: String? = null


    private var throttlingDeobfFuncExtractionEx: ParsingException? = null

    private var sigDeobFuncExtractionEx: ParsingException? = null

    private var sigTimestampExtractionEx: ParsingException? = null

    /**
     * Get the signature timestamp of the base JavaScript player file.
     *
     *
     *
     * A valid signature timestamp sent in the payload of player InnerTube requests is required to
     * get valid stream URLs on HTML5 clients for videos which have obfuscated signatures.
     *
     *
     *
     *
     * The base JavaScript player file will fetched if it is not already done.
     *
     *
     *
     *
     * The result of the extraction is cached until [.clearAllCaches] is called, making
     * subsequent calls faster.
     *
     *
     * @param videoId the video ID used to get the JavaScript base player file (an empty one can be
     * passed, even it is not recommend in order to spoof better official YouTube
     * clients)
     * @return the signature timestamp of the base JavaScript player file
     * @throws ParsingException if the extraction of the base JavaScript player file or the
     * signature timestamp failed
     */
    
    fun getSignatureTimestamp(videoId: String): Int? {
        // Return the cached result if it is present
        if (cachedSignatureTimestamp != null) {
            return cachedSignatureTimestamp
        }

        // If the signature timestamp has been not extracted on a previous call, this mean that we
        // will fail to extract it on next calls too if the player code has been not changed
        // Throw again the corresponding stored exception in this case to improve performance
        if (sigTimestampExtractionEx != null) {
            throw sigTimestampExtractionEx!!
        }

        extractJavaScriptCodeIfNeeded(videoId)

        try {
            cachedSignatureTimestamp = YoutubeSignatureUtils.getSignatureTimestamp(
                cachedJavaScriptPlayerCode!!
            ).toInt()
        } catch (e: ParsingException) {
            // Store the exception for future calls of this method, in order to improve performance
            sigTimestampExtractionEx = e
            throw e
        } catch (e: NumberFormatException) {
            sigTimestampExtractionEx =
                ParsingException("Could not convert signature timestamp to a number", e)
        } catch (e: Exception) {
            sigTimestampExtractionEx = ParsingException("Could not get signature timestamp", e)
            throw e
        }

        return cachedSignatureTimestamp
    }

    /**
     * Deobfuscate a signature of a streaming URL using its corresponding JavaScript base player's
     * function.
     *
     *
     *
     * Obfuscated signatures are only present on streaming URLs of some videos with HTML5 clients.
     *
     *
     * @param videoId             the video ID used to get the JavaScript base player file (an
     * empty one can be passed, even it is not recommend in order to
     * spoof better official YouTube clients)
     * @param obfuscatedSignature the obfuscated signature of a streaming URL
     * @return the deobfuscated signature
     * @throws ParsingException if the extraction of the base JavaScript player file or the
     * signature deobfuscation function failed
     */
    
    fun deobfuscateSignature(
        videoId: String,
        obfuscatedSignature: String?
    ): String {
        // If the signature deobfuscation function has been not extracted on a previous call, this
        // mean that we will fail to extract it on next calls too if the player code has been not
        // changed
        // Throw again the corresponding stored exception in this case to improve performance
        if (sigDeobFuncExtractionEx != null) {
            throw sigDeobFuncExtractionEx!!
        }

        extractJavaScriptCodeIfNeeded(videoId)

        if (cachedSignatureDeobfuscationFunction == null) {
            try {
                cachedSignatureDeobfuscationFunction = YoutubeSignatureUtils.getDeobfuscationCode(
                    cachedJavaScriptPlayerCode!!
                )
            } catch (e: ParsingException) {
                // Store the exception for future calls of this method, in order to improve
                // performance
                sigDeobFuncExtractionEx = e
                throw e
            } catch (e: Exception) {
                sigDeobFuncExtractionEx = ParsingException(
                    "Could not get signature parameter deobfuscation JavaScript function", e
                )
                throw e
            }
        }

        try {
            // Return an empty parameter in the case the function returns null
            /*return JavaScript.run(
                cachedSignatureDeobfuscationFunction,
                YoutubeSignatureUtils.DEOBFUSCATION_FUNCTION_NAME,
                obfuscatedSignature
            ) ?: ""*/
            return ""
        } catch (e: Exception) {
            // This shouldn't happen as the function validity is checked when it is extracted
            throw ParsingException(
                "Could not run signature parameter deobfuscation JavaScript function", e
            )
        }
    }

    /**
     * Return a streaming URL with the throttling parameter of a given one deobfuscated, if it is
     * present, using its corresponding JavaScript base player's function.
     *
     *
     *
     * The throttling parameter is present on all streaming URLs of HTML5 clients.
     *
     *
     *
     *
     * If it is not given or deobfuscated, speeds will be throttled to a very slow speed (around 50
     * KB/s) and some streaming URLs could even lead to invalid HTTP responses such a 403 one.
     *
     *
     *
     *
     * As throttling parameters can be common between multiple streaming URLs of the same player
     * response, deobfuscated parameters are cached with their obfuscated variant, in order to
     * improve performance with multiple calls of this method having the same obfuscated throttling
     * parameter.
     *
     *
     *
     *
     * The cache's size can be get using [.getThrottlingParametersCacheSize] and the cache
     * can be cleared using [.clearThrottlingParametersCache] or [.clearAllCaches].
     *
     *
     * @param videoId      the video ID used to get the JavaScript base player file (an empty one
     * can be passed, even it is not recommend in order to spoof better
     * official YouTube clients)
     * @param streamingUrl a streaming URL
     * @return the original streaming URL if it has no throttling parameter or a URL with a
     * deobfuscated throttling parameter
     * @throws ParsingException if the extraction of the base JavaScript player file or the
     * throttling parameter deobfuscation function failed
     */
    
    fun getUrlWithThrottlingParameterDeobfuscated(
        videoId: String,
        streamingUrl: String
    ): String {
        val obfuscatedThrottlingParameter =
            YoutubeThrottlingParameterUtils.getThrottlingParameterFromStreamingUrl(
                streamingUrl
            )
        // If the throttling parameter is not present, return the original streaming URL
        if (obfuscatedThrottlingParameter == null) {
            return streamingUrl
        }

        // Do not use the containsKey method of the Map interface in order to avoid a double
        // element search, and so to improve performance
        val cacheResult = CACHED_THROTTLING_PARAMETERS[obfuscatedThrottlingParameter]
        if (cacheResult != null) {
            // If the throttling parameter function has been already ran on the throttling parameter
            // of the current streaming URL, replace directly the obfuscated throttling parameter
            // with the cached result in the streaming URL
            return streamingUrl.replace(obfuscatedThrottlingParameter, cacheResult)
        }

        extractJavaScriptCodeIfNeeded(videoId)

        // If the throttling parameter deobfuscation function has been not extracted on a previous
        // call, this mean that we will fail to extract it on next calls too if the player code has
        // been not changed
        // Throw again the corresponding stored exception in this case to improve performance
        if (throttlingDeobfFuncExtractionEx != null) {
            throw throttlingDeobfFuncExtractionEx!!
        }

        if (cachedThrottlingDeobfuscationFunction == null) {
            try {
                cachedThrottlingDeobfuscationFunctionName =
                    YoutubeThrottlingParameterUtils.getDeobfuscationFunctionName(
                        cachedJavaScriptPlayerCode!!
                    )

                cachedThrottlingDeobfuscationFunction =
                    YoutubeThrottlingParameterUtils.getDeobfuscationFunction(
                        cachedJavaScriptPlayerCode!!,
                        cachedThrottlingDeobfuscationFunctionName!!
                    )
            } catch (e: ParsingException) {
                // Store the exception for future calls of this method, in order to improve
                // performance
                throttlingDeobfFuncExtractionEx = e
                throw e
            } catch (e: Exception) {
                throttlingDeobfFuncExtractionEx = ParsingException(
                    "Could not get throttling parameter deobfuscation JavaScript function", e
                )
                throw e
            }
        }

        try {
            /*val deobfuscatedThrottlingParameter = JavaScript.run(
                cachedThrottlingDeobfuscationFunction,
                cachedThrottlingDeobfuscationFunctionName,
                obfuscatedThrottlingParameter
            )

            CACHED_THROTTLING_PARAMETERS[obfuscatedThrottlingParameter] =
                deobfuscatedThrottlingParameter

            return streamingUrl.replace(
                obfuscatedThrottlingParameter, deobfuscatedThrottlingParameter
            )*/
            return streamingUrl.replace(
                obfuscatedThrottlingParameter, ""
            )
        } catch (e: Exception) {
            // This shouldn't happen as the function validity is checked when it is extracted
            throw ParsingException(
                "Could not run throttling parameter deobfuscation JavaScript function", e
            )
        }
    }

    val throttlingParametersCacheSize: Int
        /**
         * Get the current cache size of throttling parameters.
         *
         * @return the current cache size of throttling parameters
         */
        get() = CACHED_THROTTLING_PARAMETERS.size

    /**
     * Clear all caches.
     *
     *
     *
     * This method will clear all cached JavaScript code and throttling parameters.
     *
     *
     *
     *
     * The next time [.getSignatureTimestamp],
     * [.deobfuscateSignature] or
     * [.getUrlWithThrottlingParameterDeobfuscated] is called, the JavaScript
     * code will be fetched again and the corresponding extraction methods will be ran.
     *
     */
    fun clearAllCaches() {
        cachedJavaScriptPlayerCode = null
        cachedSignatureDeobfuscationFunction = null
        cachedThrottlingDeobfuscationFunctionName = null
        cachedThrottlingDeobfuscationFunction = null
        cachedSignatureTimestamp = null
        clearThrottlingParametersCache()

        // Clear cached extraction exceptions, if applicable
        throttlingDeobfFuncExtractionEx = null
        sigDeobFuncExtractionEx = null
        sigTimestampExtractionEx = null
    }

    /**
     * Clear all cached throttling parameters.
     *
     *
     *
     * The throttling parameter deobfuscation function will be ran again on these parameters if
     * streaming URLs containing them are passed in the future.
     *
     *
     *
     *
     * This method doesn't clear the cached throttling parameter deobfuscation function, this can
     * be done using [.clearAllCaches].
     *
     */
    fun clearThrottlingParametersCache() {
        CACHED_THROTTLING_PARAMETERS.clear()
    }

    /**
     * Extract the JavaScript code if it isn't already cached.
     *
     * @param videoId the video ID used to get the JavaScript base player file (an empty one can be
     * passed, even it is not recommend in order to spoof better official YouTube
     * clients)
     * @throws ParsingException if the extraction of the base JavaScript player file failed
     */
    
    private fun extractJavaScriptCodeIfNeeded(videoId: String) {
        if (cachedJavaScriptPlayerCode == null) {
            cachedJavaScriptPlayerCode = YoutubeJavaScriptExtractor.extractJavaScriptPlayerCode(
                videoId
            )
        }
    }
}
