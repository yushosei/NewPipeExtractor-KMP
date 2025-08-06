package com.yushosei.newpipe.extractor.youtube

import com.yushosei.newpipe.extractor.youtube.ClientsConstants

// TODO: add docs
internal class InnertubeClientRequestInfo private constructor(
     var clientInfo: ClientInfo,
    var deviceInfo: DeviceInfo
) {
    class ClientInfo(
        var clientName: String,
         var clientVersion: String,
        var clientScreen: String,
        var clientId: String,
         var visitorData: String?
    )

    class DeviceInfo(
        var platform: String,
        var deviceMake: String?,
        var deviceModel: String?,
        var osName: String?,
        var osVersion: String?,
        var androidSdkVersion: Int
    )


    companion object {
        
        fun ofWebClient(): InnertubeClientRequestInfo {
            return InnertubeClientRequestInfo(
                ClientInfo(
                    ClientsConstants.WEB_CLIENT_NAME,
                    ClientsConstants.WEB_HARDCODED_CLIENT_VERSION,
                    ClientsConstants.WATCH_CLIENT_SCREEN,
                    ClientsConstants.WEB_CLIENT_ID,
                    null
                ),
                DeviceInfo(
                    ClientsConstants.DESKTOP_CLIENT_PLATFORM, null, null,
                    null, null, -1
                )
            )
        }


        
        fun ofWebEmbeddedPlayerClient(): InnertubeClientRequestInfo {
            return InnertubeClientRequestInfo(
                ClientInfo(
                    ClientsConstants.WEB_EMBEDDED_CLIENT_NAME,
                    ClientsConstants.WEB_REMIX_HARDCODED_CLIENT_VERSION,
                    ClientsConstants.EMBED_CLIENT_SCREEN,
                    ClientsConstants.WEB_EMBEDDED_CLIENT_ID,
                    null
                ),
                DeviceInfo(
                    ClientsConstants.DESKTOP_CLIENT_PLATFORM, null, null,
                    null, null, -1
                )
            )
        }


        
        fun ofTvHtml5Client(): InnertubeClientRequestInfo {
            return InnertubeClientRequestInfo(
                ClientInfo(
                    ClientsConstants.TVHTML5_CLIENT_NAME,
                    ClientsConstants.TVHTML5_CLIENT_VERSION,
                    ClientsConstants.WATCH_CLIENT_SCREEN,
                    ClientsConstants.TVHTML5_CLIENT_ID,
                    null
                ),
                DeviceInfo(
                    ClientsConstants.TVHTML5_CLIENT_PLATFORM,
                    ClientsConstants.TVHTML5_DEVICE_MAKE,
                    ClientsConstants.TVHTML5_DEVICE_MODEL_AND_OS_NAME,
                    ClientsConstants.TVHTML5_DEVICE_MODEL_AND_OS_NAME,
                    "",
                    -1
                )
            )
        }


        
        fun ofAndroidClient(): InnertubeClientRequestInfo {
            return InnertubeClientRequestInfo(
                ClientInfo(
                    ClientsConstants.ANDROID_CLIENT_NAME,
                    ClientsConstants.ANDROID_CLIENT_VERSION,
                    ClientsConstants.WATCH_CLIENT_SCREEN,
                    ClientsConstants.ANDROID_CLIENT_ID,
                    null
                ),
                DeviceInfo(
                    ClientsConstants.MOBILE_CLIENT_PLATFORM, null, null,
                    "Android", "15", 35
                )
            )
        }


        
        fun ofIosClient(): InnertubeClientRequestInfo {
            return InnertubeClientRequestInfo(
                ClientInfo(
                    ClientsConstants.IOS_CLIENT_NAME, ClientsConstants.IOS_CLIENT_VERSION,
                    ClientsConstants.WATCH_CLIENT_SCREEN, ClientsConstants.IOS_CLIENT_ID, null
                ),
                DeviceInfo(
                    ClientsConstants.MOBILE_CLIENT_PLATFORM, "Apple",
                    ClientsConstants.IOS_DEVICE_MODEL, "iOS", ClientsConstants.IOS_OS_VERSION, -1
                )
            )
        }
    }
}
