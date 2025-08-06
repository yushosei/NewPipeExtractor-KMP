/*
 * Created by Christian Schabesberger on 11.02.17.
 *
 * Copyright (C) 2017 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * InfoItem.java is part of NewPipe Extractor.
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
 * along with NewPipe Extractor.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.yushosei.newpipe.extractor

abstract class InfoItem(
    val infoType: InfoType,
    val serviceId: Int,
    val url: String,
    val name: String
) {
    var thumbnails: List<Image> = listOf()

    override fun toString(): String {
        return "InfoItem{" +
                "infoType=$infoType" +
                ", serviceId=$serviceId" +
                ", url='$url'" +
                ", name='$name'" +
                ", thumbnails=$thumbnails" +
                '}'
    }

    enum class InfoType {
        STREAM,
        PLAYLIST,
        CHANNEL,
        COMMENT
    }
}
