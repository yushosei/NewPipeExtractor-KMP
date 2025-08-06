package com.yushosei.newpipe.extractor.youtube

internal object ClientsConstants {
    // Common client fields
    const val DESKTOP_CLIENT_PLATFORM: String = "DESKTOP"
    const val MOBILE_CLIENT_PLATFORM: String = "MOBILE"
    const val WATCH_CLIENT_SCREEN: String = "WATCH"
    const val EMBED_CLIENT_SCREEN: String = "EMBED"

    // WEB (YouTube desktop) client fields
    const val WEB_CLIENT_ID: String = "1"
    const val WEB_CLIENT_NAME: String = "WEB"

    /**
     * The client version for InnerTube requests with the `WEB` client, used as the last
     * fallback if the extraction of the real one failed.
     */
    const val WEB_HARDCODED_CLIENT_VERSION: String = "2.20250122.04.00"

    // WEB_REMIX (YouTube Music) client fields
    const val WEB_REMIX_CLIENT_ID: String = "67"
    const val WEB_REMIX_CLIENT_NAME: String = "WEB_REMIX"
    const val WEB_REMIX_HARDCODED_CLIENT_VERSION: String = "1.20250122.01.00"

    // TVHTML5 (YouTube on TVs and consoles using HTML5) client fields
    const val TVHTML5_CLIENT_ID: String = "7"
    const val TVHTML5_CLIENT_NAME: String = "TVHTML5"
    const val TVHTML5_CLIENT_VERSION: String = "7.20250122.15.00"
    const val TVHTML5_CLIENT_PLATFORM: String = "GAME_CONSOLE"
    const val TVHTML5_DEVICE_MAKE: String = "Sony"
    const val TVHTML5_DEVICE_MODEL_AND_OS_NAME: String = "PlayStation 4"

    // CHECKSTYLE:OFF
    const val TVHTML5_USER_AGENT: String =
        "Mozilla/5.0 (PlayStation; PlayStation 4/12.00) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Safari/605.1.15"

    // CHECKSTYLE:ON
    // WEB_EMBEDDED_PLAYER (YouTube embeds)
    const val WEB_EMBEDDED_CLIENT_ID: String = "56"
    const val WEB_EMBEDDED_CLIENT_NAME: String = "WEB_EMBEDDED_PLAYER"
    const val WEB_EMBEDDED_CLIENT_VERSION: String = "1.20250121.00.00"

    // IOS (iOS YouTube app) client fields
    const val IOS_CLIENT_ID: String = "5"
    const val IOS_CLIENT_NAME: String = "IOS"

    /**
     * The hardcoded client version of the iOS app used for InnerTube requests with this client.
     *
     *
     *
     * It can be extracted by getting the latest release version of the app on
     * [the App
 * Store page of the YouTube app](https://apps.apple.com/us/app/youtube-watch-listen-stream/id544007664/), in the `What’s New` section.
     *
     */
    const val IOS_CLIENT_VERSION: String = "20.03.02"

    /**
     * The device machine id for the iPhone 15 Pro Max, used to get 60fps with the `iOS`
     * client.
     *
     *
     *
     * See [this GitHub Gist](https://gist.github.com/adamawolf/3048717) for more
     * information.
     *
     */
    const val IOS_DEVICE_MODEL: String = "iPhone16,2"

    /**
     * The iOS version to be used in JSON POST requests, the one of an iPhone 15 Pro Max running
     * iOS 18.2.1 with the hardcoded version of the iOS app (for the `"osVersion"` field).
     *
     *
     *
     * The value of this field seems to use the following structure:
     * "iOS major version.minor version.patch version.build version", where
     * "patch version" is equal to 0 if it isn't set
     * The build version corresponding to the iOS version used can be found on
     * [
 * https://theapplewiki.com/wiki/Firmware/iPhone/18.x#iPhone_15_Pro_Max](https://theapplewiki.com/wiki/Firmware/iPhone/18.x#iPhone_15_Pro_Max)
     *
     *
     * @see .IOS_USER_AGENT_VERSION
     */
    const val IOS_OS_VERSION: String = "18.2.1.22C161"

    /**
     * The iOS version to be used in the HTTP user agent for requests.
     *
     *
     *
     * This should be the same of as [.IOS_OS_VERSION].
     *
     *
     * @see .IOS_OS_VERSION
     */
    const val IOS_USER_AGENT_VERSION: String = "18_2_1"

    // ANDROID (Android YouTube app) client fields
    const val ANDROID_CLIENT_ID: String = "3"
    const val ANDROID_CLIENT_NAME: String = "ANDROID"

    /**
     * The hardcoded client version of the Android app used for InnerTube requests with this
     * client.
     *
     *
     *
     * It can be extracted by getting the latest release version of the app in an APK repository
     * such as [APKMirror](https://www.apkmirror.com/apk/google-inc/youtube/).
     *
     */
    const val ANDROID_CLIENT_VERSION: String = "19.28.35"
}
