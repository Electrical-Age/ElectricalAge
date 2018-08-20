package mods.eln.misc

import java.awt.Color

val RED = HSLColor(Color.RED)
val GREEN = HSLColor(Color.GREEN)
val BLUE = HSLColor(Color.BLUE)
val BLACK = HSLColor(Color.BLACK)
val WHITE = HSLColor(Color.WHITE)
val YELLOW = HSLColor(Color.YELLOW)


data class BlackBodyColor(val red: Float, val green: Float, val blue: Float) {
    operator fun times(b: Float) = BlackBodyColor(red * b, green * b, blue * b)
    operator fun plus(b: BlackBodyColor) = BlackBodyColor(red + b.red, green + b.green, blue + b.blue)

    fun normalize(): BlackBodyColor {
        val max = Math.max(red, Math.max(green, blue))
        return BlackBodyColor(red / max, green / max, blue / max)
    }
}

/**
 * Returns the RGB color for a given black-body temperature.
 * Not very accurate. May return negative values.
 *
 * @Param temp: Temperature, in kelvins.
 */
fun BlackBodyTemperature(temp: Float): BlackBodyColor {
    val x = temp / 1000f
    val x2 = x * x
    val x3 = x2 * x
    val x4 = x3 * x
    val x5 = x4 * x

    val R: Float
    val G: Float
    val B: Float

    // red
    if (temp <= 6600)
        R = 1.0f
    else
        R = 0.0002889f * x5 - 0.01258f * x4 + 0.2148f * x3 - 1.776f * x2 + 6.907f * x - 8.723f

    // green
    if (temp <= 6600)
        G = -4.593e-05f * x5 + 0.001424f * x4 - 0.01489f * x3 + 0.0498f * x2 + 0.1669f * x - 0.1653f
    else
        G = -1.308e-07f * x5 + 1.745e-05f * x4 - 0.0009116f * x3 + 0.02348f * x2 - 0.3048f * x + 2.159f

    // blue
    if (temp <= 2000f)
        B = 0f;
    else if (temp < 6600f)
        B = 1.764e-05f * x5 + 0.0003575f * x4 - 0.01554f * x3 + 0.1549f * x2 - 0.3682f * x + 0.2386f
    else
        B = 1f;

    return BlackBodyColor(R, G, B)
}

/**
 * Returns the luminosity of an ideal radiator at a given black-body temperature, in some sense.
 * This does not clamp to the visible range, nor account for size or.. anything, really.
 * (Stefen-Boltzmann law)
 *
 * It'll need a full-screen shader to look really good, because what we're looking for here is glare. Perhaps later.
 *
 * @Param temp: Temperature, in kelvins.
 */
fun BlackBodyPower(temp: Float): Float {
    val sigma = 5.6703e-8f
    return sigma * temp * temp * temp * temp
}

/**
 * The HSLColor class provides methods to manipulate HSL (Hue, Saturation
 * Luminance) values to create a corresponding Color object using the RGB
 * ColorSpace.

 * The HUE is the color, the Saturation is the purity of the color (with
 * respect to grey) and Luminance is the brightness of the color (with respect
 * to black and white)

 * The Hue is specified as an angel between 0 - 360 degrees where red is 0,
 * green is 120 and blue is 240. In between you have the colors of the rainbow.
 * Saturation is specified as a percentage between 0 - 100 where 100 is fully
 * saturated and 0 approaches gray. Luminance is specified as a percentage
 * between 0 - 100 where 0 is black and 100 is white.

 * In particular the HSL color space makes it easier change the Tone or Shade
 * of a color by adjusting the luminance value.
 */
class HSLColor {
    /**
     * Get the RGB Color object represented by this HDLColor.

     * @return the RGB Color object.
     */
    var rgb: Color? = null
        private set
    /**
     * Get the HSL values.

     * @return the HSL values.
     */
    var hsl: FloatArray? = null
        private set
    /**
     * Get the Alpha value.

     * @return the Alpha value.
     */
    var alpha: Float = 0.toFloat()
        private set

    /**
     * Create a HSLColor object using an RGB Color object.

     * @param rgb the RGB Color object
     */
    constructor(rgb: Color) {
        this.rgb = rgb
        hsl = fromRGB(rgb)
        alpha = rgb.alpha / 255.0f
    }

    /**
     * Create a HSLColor object using individual HSL values.

     * @param h     the Hue value in degrees between 0 - 360
     * *
     * @param s     the Saturation percentage between 0 - 100
     * *
     * @param l     the Lumanance percentage between 0 - 100
     * *
     * @param alpha the alpha value between 0 - 1
     */
    @JvmOverloads constructor(h: Float, s: Float, l: Float, alpha: Float = 1.0f) {
        rgb = toRGB(floatArrayOf(h, s, l), alpha)
        this.alpha = alpha
    }

    /**
     * Create a HSLColor object using an an array containing the
     * individual HSL values.

     * @param hsl  array containing HSL values
     * *
     * @param alpha the alpha value between 0 - 1
     */
    @JvmOverloads constructor(hsl: FloatArray, alpha: Float = 1.0f) {
        this.hsl = hsl
        this.alpha = alpha
        rgb = toRGB(hsl, alpha)
    }

    /**
     * Create a RGB Color object based on this HSLColor with a different
     * Hue value. The degrees specified is an absolute value.

     * @param degrees - the Hue value between 0 - 360
     * *
     * @return the RGB Color object
     */
    fun adjustHue(degrees: Float): Color {
        return toRGB(degrees, hsl!![1], hsl!![2], alpha)
    }

    /**
     * Create a RGB Color object based on this HSLColor with a different
     * Luminance value. The percent specified is an absolute value.

     * @param percent - the Luminance value between 0 - 100
     * *
     * @return the RGB Color object
     */
    fun adjustLuminance(percent: Float): Color {
        return toRGB(hsl!![0], hsl!![1], percent, alpha)
    }

    /**
     * Create a RGB color object based on this HSLColor with a different
     * Luminance value. The percent specified is an absolute value.
     *
     * @param percent - the Luminance value. Will be clamped to min-max.
     * @param min
     * @param max
     *
     * @return the RGB Color object
     */
    fun adjustLuminanceClamped(percent: Float, min: Float, max: Float): Color {
        return adjustLuminance(Math.min(Math.max(percent, min), max))
    }

    /**
     * Create a RGB Color object based on this HSLColor with a different
     * Saturation value. The percent specified is an absolute value.

     * @param percent - the Saturation value between 0 - 100
     * *
     * @return the RGB Color object
     */
    fun adjustSaturation(percent: Float): Color {
        return toRGB(hsl!![0], percent, hsl!![2], alpha)
    }

    /**
     * Create a RGB Color object based on this HSLColor with a different
     * Shade. Changing the shade will return a darker color. The percent
     * specified is a relative value.

     * @param percent - the value between 0 - 100
     * *
     * @return the RGB Color object
     */
    fun adjustShade(percent: Float): Color {
        val multiplier = (100.0f - percent) / 100.0f
        val l = Math.max(0.0f, hsl!![2] * multiplier)

        return toRGB(hsl!![0], hsl!![1], l, alpha)
    }

    /**
     * Create a RGB Color object based on this HSLColor with a different
     * Tone. Changing the tone will return a lighter color. The percent
     * specified is a relative value.

     * @param percent - the value between 0 - 100
     * *
     * @return the RGB Color object
     */
    fun adjustTone(percent: Float): Color {
        val multiplier = (100.0f + percent) / 100.0f
        val l = Math.min(100.0f, hsl!![2] * multiplier)

        return toRGB(hsl!![0], hsl!![1], l, alpha)
    }

    /**
     * Create a RGB Color object that is the complementary color of this
     * HSLColor. This is a convenience method. The complementary color is
     * determined by adding 180 degrees to the Hue value.
     * @return the RGB Color object
     */
    val complementary: Color
        get() {
            val hue = (hsl!![0] + 180.0f) % 360.0f
            return toRGB(hue, hsl!![1], hsl!![2])
        }

    /**
     * Get the Hue value.

     * @return the Hue value.
     */
    val hue: Float
        get() = hsl!![0]

    /**
     * Get the Luminance value.

     * @return the Luminance value.
     */
    val luminance: Float
        get() = hsl!![2]

    /**
     * Get the Saturation value.

     * @return the Saturation value.
     */
    val saturation: Float
        get() = hsl!![1]

    override fun toString(): String {
        val toString = "HSLColor[h=" + hsl!![0] + ",s=" + hsl!![1] + ",l=" + hsl!![2] + ",alpha=" + alpha + "]"

        return toString
    }

    companion object {

        /**
         * Convert a RGB Color to it corresponding HSL values.

         * @return an array containing the 3 HSL values.
         */
        fun fromRGB(color: Color): FloatArray {
            //  Get RGB values in the range 0 - 1

            val rgb = color.getRGBColorComponents(null)
            val r = rgb[0]
            val g = rgb[1]
            val b = rgb[2]

            //	Minimum and Maximum RGB values are used in the HSL calculations

            val min = Math.min(r, Math.min(g, b))
            val max = Math.max(r, Math.max(g, b))

            //  Calculate the Hue

            var h = 0f

            if (max == min)
                h = 0f
            else if (max == r)
                h = ((60 * (g - b) / (max - min)) + 360) % 360
            else if (max == g)
                h = (60 * (b - r) / (max - min)) + 120
            else if (max == b)
                h = (60 * (r - g) / (max - min)) + 240

            //  Calculate the Luminance

            val l = (max + min) / 2

            //  Calculate the Saturation

            var s: Float

            if (max == min)
                s = 0f
            else if (l <= .5f)
                s = (max - min) / (max + min)
            else
                s = (max - min) / (2f - max - min)

            return floatArrayOf(h, s * 100, l * 100)
        }

        /**
         * Convert HSL values to a RGB Color.
         * H (Hue) is specified as degrees in the range 0 - 360.
         * S (Saturation) is specified as a percentage in the range 1 - 100.
         * L (Lumanance) is specified as a percentage in the range 1 - 100.

         * @param hsl    an array containing the 3 HSL values
         * *
         * @param alpha  the alpha value between 0 - 1
         * *
         * *
         * @returns the RGB Color object
         */
        @JvmOverloads fun toRGB(hsl: FloatArray, alpha: Float = 1.0f): Color {
            return toRGB(hsl[0], hsl[1], hsl[2], alpha)
        }

        /**
         * Convert HSL values to a RGB Color.

         * @param h Hue is specified as degrees in the range 0 - 360.
         * *
         * @param s Saturation is specified as a percentage in the range 1 - 100.
         * *
         * @param l Lumanance is specified as a percentage in the range 1 - 100.
         * *
         * @param alpha  the alpha value between 0 - 1
         * *
         * *
         * @returns the RGB Color object
         */
        @JvmOverloads fun toRGB(h: Float, s: Float, l: Float, alpha: Float = 1.0f): Color {
            var hvar = h
            var svar = s
            var lvar = l
            if (svar < 0.0f || svar > 100.0f) {
                val message = "Color parameter outside of expected range - Saturation"
                throw IllegalArgumentException(message)
            }

            if (lvar < 0.0f || lvar > 100.0f) {
                val message = "Color parameter outside of expected range - Luminance"
                throw IllegalArgumentException(message)
            }

            if (alpha < 0.0f || alpha > 1.0f) {
                val message = "Color parameter outside of expected range - Alpha"
                throw IllegalArgumentException(message)
            }

            //  Formula needs all values between 0 - 1.

            hvar = hvar % 360.0f
            hvar /= 360f
            svar /= 100f
            lvar /= 100f

            var q: Float

            if (lvar < 0.5)
                q = lvar * (1 + svar)
            else
                q = (lvar + svar) - (svar * lvar)

            val p = 2 * lvar - q

            var r = Math.max(0f, HueToRGB(p, q, hvar + (1.0f / 3.0f)))
            var g = Math.max(0f, HueToRGB(p, q, hvar))
            var b = Math.max(0f, HueToRGB(p, q, hvar - (1.0f / 3.0f)))

            r = Math.min(r, 1.0f)
            g = Math.min(g, 1.0f)
            b = Math.min(b, 1.0f)

            return Color(r, g, b, alpha)
        }

        private fun HueToRGB(p: Float, q: Float, h: Float): Float {
            var hvar = h
            if (hvar < 0) hvar += 1f

            if (hvar > 1) hvar -= 1f

            if (6 * hvar < 1) {
                return p + ((q - p) * 6f * hvar)
            }

            if (2 * hvar < 1) {
                return q
            }

            if (3 * hvar < 2) {
                return p + ((q - p) * 6f * ((2.0f / 3.0f) - hvar))
            }

            return p
        }
    }
}
/**
 * Create a HSLColor object using individual HSL values and a default
 * alpha value of 1.0.

 * @param h is the Hue value in degrees between 0 - 360
 * *
 * @param s is the Saturation percentage between 0 - 100
 * *
 * @param l is the Lumanance percentage between 0 - 100
 */
/**
 * Create a HSLColor object using an an array containing the
 * individual HSL values and with a default alpha value of 1.

 * @param hsl  array containing HSL values
 */
/**
 * Convert HSL values to a RGB Color with a default alpha value of 1.
 * H (Hue) is specified as degrees in the range 0 - 360.
 * S (Saturation) is specified as a percentage in the range 1 - 100.
 * L (Lumanance) is specified as a percentage in the range 1 - 100.

 * @param hsl an array containing the 3 HSL values
 * *
 * *
 * @returns the RGB Color object
 */
/**
 * Convert HSL values to a RGB Color with a default alpha value of 1.

 * @param h Hue is specified as degrees in the range 0 - 360.
 * *
 * @param s Saturation is specified as a percentage in the range 1 - 100.
 * *
 * @param l Lumanance is specified as a percentage in the range 1 - 100.
 * *
 * *
 * @returns the RGB Color object
 */
