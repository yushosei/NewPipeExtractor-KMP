package com.yushosei.newpipe.extractor


/**
 * Class representing images in the extractor.
 *
 *
 *
 * An image has four properties: its URL, its height, its width and its estimated quality level.
 *
 *
 *
 *
 * Depending of the services, the height, the width or both properties may be not known.
 * Implementations **must use** the relevant unknown constants in this case
 * ([.HEIGHT_UNKNOWN] and [.WIDTH_UNKNOWN]), to ensure properly the lack of knowledge
 * of one or both of these properties to extractor clients.
 *
 *
 *
 *
 * They should also respect the ranges defined in the estimated image resolution levels as much as
 * possible, to ensure consistency to extractor clients.
 *
 */
class Image(
    /**
     * Get the URL of this [Image].
     *
     * @return the [Image]'s URL.
     */
    val url: String,
    /**
     * Get the height of this [Image].
     *
     *
     *
     * If it is unknown, [.HEIGHT_UNKNOWN] is returned instead.
     *
     *
     * @return the [Image]'s height or [.HEIGHT_UNKNOWN]
     */
    val height: Int,
    /**
     * Get the width of this [Image].
     *
     *
     *
     * If it is unknown, [.WIDTH_UNKNOWN] is returned instead.
     *
     *
     * @return the [Image]'s width or [.WIDTH_UNKNOWN]
     */
    val width: Int,
    private val estimatedResolutionLevel: ResolutionLevel
) {
    /**
     * Get the estimated resolution level of this image.
     *
     *
     *
     * If it is unknown, [ResolutionLevel.UNKNOWN] is returned instead.
     *
     *
     * @return the estimated resolution level, which is never `null`
     * @see ResolutionLevel
     */


    /**
     * Get a string representation of this [Image] instance.
     *
     *
     *
     * The representation will be in the following format, where `url`, `height`,
     * `width` and `estimatedResolutionLevel` represent the corresponding properties:
     * <br></br>
     * <br></br>
     * `Image {url=url, height='height, width=width,
     * estimatedResolutionLevel=estimatedResolutionLevel}'`
     *
     *
     * @return a string representation of this [Image] instance
     */
    override fun toString(): String {
        return ("Image {" + "url=" + url + ", height=" + height + ", width=" + width
                + ", estimatedResolutionLevel=" + estimatedResolutionLevel + "}")
    }

    /**
     * The estimated resolution level of an [Image].
     *
     *
     *
     * Some services don't return the size of their images, but we may know for a specific image
     * type that a service returns, according to real data, an approximation of the resolution
     * level.
     *
     */
    enum class ResolutionLevel {
        /**
         * The high resolution level.
         *
         *
         *
         * This level applies to images with a height greater than or equal to 720px.
         *
         */
        HIGH,

        /**
         * The medium resolution level.
         *
         *
         *
         * This level applies to images with a height between 175px inclusive and 720px exclusive.
         *
         */
        MEDIUM,

        /**
         * The low resolution level.
         *
         *
         *
         * This level applies to images with a height between 1px inclusive and 175px exclusive.
         *
         */
        LOW,

        /**
         * The unknown resolution level.
         *
         *
         *
         * This value is returned when the extractor doesn't know what resolution level an image
         * could have, for example if the extractor loops in an array of images with different
         * resolution levels without knowing the height.
         *
         */
        UNKNOWN;

        companion object {
            /**
             * Get a [ResolutionLevel] based from the given height.
             *
             * @param heightPx the height from which returning the good [ResolutionLevel]
             * @return the [ResolutionLevel] corresponding to the height provided. See the
             * [ResolutionLevel] values for details about what value is returned.
             */

            fun fromHeight(heightPx: Int): ResolutionLevel {
                if (heightPx <= 0) {
                    return UNKNOWN
                }

                if (heightPx < 175) {
                    return LOW
                }

                if (heightPx < 720) {
                    return MEDIUM
                }

                return HIGH
            }
        }
    }

    companion object {
        /**
         * Constant representing that the height of an [Image] is unknown.
         */
        const val HEIGHT_UNKNOWN: Int = -1

        /**
         * Constant representing that the width of an [Image] is unknown.
         */
        const val WIDTH_UNKNOWN: Int = -1
    }
}
