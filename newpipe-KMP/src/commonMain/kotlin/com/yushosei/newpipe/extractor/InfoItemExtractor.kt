package com.yushosei.newpipe.extractor

import com.yushosei.newpipe.extractor.exceptions.ParsingException

interface InfoItemExtractor {

    
    val name: String


    
    val url: String

    
    val thumbnails: List<Image>
}
