package com.yushosei.newpipe.extractor.youtube


/**
 * Interface to provide `poToken`s to YouTube player requests.
 *
 *
 *
 * On some major clients, YouTube requires that the integrity of the device passes some checks to
 * allow playback.
 *
 *
 *
 *
 * These checks involve running codes to verify the integrity and using their result to generate
 * one or multiple `poToken`(s) (which stands for proof of origin token(s)).
 *
 *
 *
 *
 * These tokens may have a role in triggering the sign in requirement.
 *
 *
 *
 *
 * If an implementation does not want to return a `poToken` for a specific client, it **must
 * return `null`**.
 *
 *
 *
 *
 * **Implementations of this interface are expected to be thread-safe, as they may be accessed by
 * multiple threads.**
 *
 */
internal interface PoTokenProvider {
    /**
     * Get a [PoTokenResult] specific to the desktop website, a.k.a. the WEB InnerTube client.
     *
     *
     *
     * To be generated and valid, `poToken`s from this client must be generated using Google's
     * BotGuard machine, which requires a JavaScript engine with a good DOM implementation. They
     * must be added to adaptive/DASH streaming URLs with the `pot` parameter.
     *
     *
     *
     *
     * Note that YouTube desktop website generates two `poToken`s:
     * - one for the player requests `poToken`s, using the videoId as the minter value;
     * - one for the streaming URLs, using a visitor data for logged-out users as the minter value.
     *
     *
     * @return a [PoTokenResult] specific to the WEB InnerTube client
     */
    fun getWebClientPoToken(videoId: String?): PoTokenResult?

    /**
     * Get a [PoTokenResult] specific to the web embeds, a.k.a. the WEB_EMBEDDED_PLAYER
     * InnerTube client.
     *
     *
     *
     * To be generated and valid, `poToken`s from this client must be generated using Google's
     * BotGuard machine, which requires a JavaScript engine with a good DOM implementation. They
     * should be added to adaptive/DASH streaming URLs with the `pot` parameter.
     *
     *
     *
     *
     * As of writing, like the YouTube desktop website previously did, it generates only one
     * `poToken`, sent in player requests and streaming URLs, using a visitor data for
     * logged-out users. `poToken`s do not seem to be mandatory for now on this client.
     *
     *
     * @return a [PoTokenResult] specific to the WEB_EMBEDDED_PLAYER InnerTube client
     */
    fun getWebEmbedClientPoToken(videoId: String?): PoTokenResult?

    /**
     * Get a [PoTokenResult] specific to the Android app, a.k.a. the ANDROID InnerTube client.
     *
     *
     *
     * Implementation details are not known, the app uses DroidGuard, a downloaded native virtual
     * machine ran by Google Play Services for which its code is updated pretty frequently.
     *
     *
     *
     *
     * As of writing, DroidGuard seem to check for the Android app signature and package ID, as
     * non-rooted YouTube patched with reVanced doesn't work without spoofing another InnerTube
     * client while the rooted version works without any client spoofing.
     *
     *
     *
     *
     * There should be only one `poToken` needed for the player requests, it shouldn't be
     * required for regular adaptive URLs (i.e. not server adaptive bitrate (SABR) URLs). HLS
     * formats returned (only for premieres and running and post-live livestreams) in the client's
     * HLS manifest URL should work without `poToken`s.
     *
     *
     * @return a [PoTokenResult] specific to the ANDROID InnerTube client
     */
    fun getAndroidClientPoToken(videoId: String?): PoTokenResult?

    /**
     * Get a [PoTokenResult] specific to the iOS app, a.k.a. the IOS InnerTube client.
     *
     *
     *
     * Implementation details are not known, the app seem to use something called iosGuard which
     * should be similar to Android's DroidGuard. It may rely on Apple's attestation APIs.
     *
     *
     *
     *
     * As of writing, there should be only one `poToken` needed for the player requests, it
     * shouldn't be required for regular adaptive URLs (i.e. not server adaptive bitrate (SABR)
     * URLs). HLS formats returned in the client's HLS manifest URL should also work without a
     * `poToken`.
     *
     *
     * @return a [PoTokenResult] specific to the IOS InnerTube client
     */
    fun getIosClientPoToken(videoId: String?): PoTokenResult?
}
