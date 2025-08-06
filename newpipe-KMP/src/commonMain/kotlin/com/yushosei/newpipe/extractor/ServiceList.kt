package com.yushosei.newpipe.extractor

import com.yushosei.newpipe.extractor.youtube.YoutubeService

/*
* Copyright (C) 2018 Christian Schabesberger <chris.schabesberger@mailbox.org>
* ServiceList.java is part of NewPipe Extractor.
*
* NewPipe Extractor is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* NewPipe Extractor is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with NewPipe Extractor.  If not, see <http://www.gnu.org/licenses/>.
*/
/**
 * A list of supported services.
 */
// keep unusual names and inner assignments
object ServiceList {
    val YouTube: YoutubeService = YoutubeService(0)

    /**
     * When creating a new service, put this service in the end of this list,
     * and give it the next free id.
     */
    private val SERVICES: List<StreamingService> = listOf(
        YouTube
    )

    /**
     * Get all the supported services.
     *
     * @return a unmodifiable list of all the supported services
     */
    
    fun all(): List<StreamingService> {
        return SERVICES
    }
}
