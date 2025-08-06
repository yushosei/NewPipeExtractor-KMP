package com.yushosei.newpipe.extractor.youtube

import com.yushosei.newpipe.extractor.exceptions.ParsingException
import com.yushosei.newpipe.extractor.utils.Parser
import com.yushosei.newpipe.extractor.utils.Parser.RegexException

/**
 * Utility class to get the throttling parameter decryption code and check if a streaming has the
 * throttling parameter.
 */
internal object YoutubeThrottlingParameterUtils {
    private val THROTTLING_PARAM_PATTERN: Regex = Regex("[&?]n=([^&]+)")

    private val SINGLE_CHAR_VARIABLE_REGEX = "[a-zA-Z0-9\$_]"

    private val MULTIPLE_CHARS_REGEX = SINGLE_CHAR_VARIABLE_REGEX + "+"

    private const val ARRAY_ACCESS_REGEX = "\\[(\\d+)]"

    // CHECKSTYLE:OFF
    private val DEOBFUSCATION_FUNCTION_NAME_REGEXES = listOf( /*
             * Matches the following text, where we want SDa and the array index accessed:
             *
             * a.D&&(b="nn"[+a.D],WL(a),c=a.j[b]||null)&&(c=SDa[0](c),a.set(b,c),SDa.length||Wma("")
             */Regex(
        (SINGLE_CHAR_VARIABLE_REGEX + "=\"nn\"\\[\\+" + MULTIPLE_CHARS_REGEX
                + "\\." + MULTIPLE_CHARS_REGEX + "]," + MULTIPLE_CHARS_REGEX + "\\("
                + MULTIPLE_CHARS_REGEX + "\\)," + MULTIPLE_CHARS_REGEX + "="
                + MULTIPLE_CHARS_REGEX + "\\." + MULTIPLE_CHARS_REGEX + "\\["
                + MULTIPLE_CHARS_REGEX + "]\\|\\|null\\)&&\\(" + MULTIPLE_CHARS_REGEX + "=("
                + MULTIPLE_CHARS_REGEX + ")" + ARRAY_ACCESS_REGEX)
    ),  /*
             * Matches the following text, where we want Wma:
             *
             * a.D&&(b="nn"[+a.D],WL(a),c=a.j[b]||null)&&(c=SDa[0](c),a.set(b,c),SDa.length||Wma("")
             */

        Regex(
            (SINGLE_CHAR_VARIABLE_REGEX + "=\"nn\"\\[\\+" + MULTIPLE_CHARS_REGEX
                    + "\\." + MULTIPLE_CHARS_REGEX + "]," + MULTIPLE_CHARS_REGEX + "\\("
                    + MULTIPLE_CHARS_REGEX + "\\)," + MULTIPLE_CHARS_REGEX + "="
                    + MULTIPLE_CHARS_REGEX + "\\." + MULTIPLE_CHARS_REGEX + "\\["
                    + MULTIPLE_CHARS_REGEX + "]\\|\\|null\\).+\\|\\|(" + MULTIPLE_CHARS_REGEX
                    + ")\\(\"\"\\)")
        ),  /*
             * Matches the following text, where we want cvb and the array index accessed:
             *
             * ,Vb(m),W=m.j[c]||null)&&(W=cvb[0](W),m.set(c,W)
             */

        Regex(
            ("," + MULTIPLE_CHARS_REGEX + "\\("
                    + MULTIPLE_CHARS_REGEX + "\\)," + MULTIPLE_CHARS_REGEX + "="
                    + MULTIPLE_CHARS_REGEX + "\\." + MULTIPLE_CHARS_REGEX + "\\["
                    + MULTIPLE_CHARS_REGEX + "]\\|\\|null\\)&&\\(\\b" + MULTIPLE_CHARS_REGEX + "=("
                    + MULTIPLE_CHARS_REGEX + ")" + ARRAY_ACCESS_REGEX + "\\("
                    + SINGLE_CHAR_VARIABLE_REGEX + "\\)," + MULTIPLE_CHARS_REGEX
                    + "\\.set\\((?:\"n+\"|" + MULTIPLE_CHARS_REGEX + ")," + MULTIPLE_CHARS_REGEX
                    + "\\)")
        ),  /*
             * Matches the following text, where we want rma:
             *
             * a.D&&(b="nn"[+a.D],c=a.get(b))&&(c=rDa[0](c),a.set(b,c),rDa.length||rma("")
             */

        Regex(
            (SINGLE_CHAR_VARIABLE_REGEX + "=\"nn\"\\[\\+" + MULTIPLE_CHARS_REGEX
                    + "\\." + MULTIPLE_CHARS_REGEX + "]," + MULTIPLE_CHARS_REGEX + "="
                    + MULTIPLE_CHARS_REGEX + "\\.get\\(" + MULTIPLE_CHARS_REGEX + "\\)\\).+\\|\\|("
                    + MULTIPLE_CHARS_REGEX + ")\\(\"\"\\)")
        ),  /*
             * Matches the following text, where we want rDa and the array index accessed:
             *
             * a.D&&(b="nn"[+a.D],c=a.get(b))&&(c=rDa[0](c),a.set(b,c),rDa.length||rma("")
             */

        Regex(
            (SINGLE_CHAR_VARIABLE_REGEX + "=\"nn\"\\[\\+" + MULTIPLE_CHARS_REGEX
                    + "\\." + MULTIPLE_CHARS_REGEX + "]," + MULTIPLE_CHARS_REGEX + "="
                    + MULTIPLE_CHARS_REGEX + "\\.get\\(" + MULTIPLE_CHARS_REGEX + "\\)\\)&&\\("
                    + MULTIPLE_CHARS_REGEX + "=(" + MULTIPLE_CHARS_REGEX + ")\\[(\\d+)]")
        ),  /*
             * Matches the following text, where we want BDa and the array index accessed:
             *
             * (b=String.fromCharCode(110),c=a.get(b))&&(c=BDa[0](c)
             */

        Regex(
            ("\\(" + SINGLE_CHAR_VARIABLE_REGEX + "=String\\.fromCharCode\\(110\\),"
                    + SINGLE_CHAR_VARIABLE_REGEX + "=" + SINGLE_CHAR_VARIABLE_REGEX + "\\.get\\("
                    + SINGLE_CHAR_VARIABLE_REGEX + "\\)\\)" + "&&\\(" + SINGLE_CHAR_VARIABLE_REGEX
                    + "=(" + MULTIPLE_CHARS_REGEX + ")" + "(?:" + ARRAY_ACCESS_REGEX + ")?\\("
                    + SINGLE_CHAR_VARIABLE_REGEX + "\\)")
        ),  /*
             * Matches the following text, where we want Yva and the array index accessed:
             *
             * .get("n"))&&(b=Yva[0](b)
             */

        Regex(
            ("\\.get\\(\"n\"\\)\\)&&\\(" + SINGLE_CHAR_VARIABLE_REGEX
                    + "=(" + MULTIPLE_CHARS_REGEX + ")(?:" + ARRAY_ACCESS_REGEX + ")?\\("
                    + SINGLE_CHAR_VARIABLE_REGEX + "\\)")
        )
    )


    // CHECKSTYLE:ON
    // Escape the curly end brace to allow compatibility with Android's regex engine
    // See https://stackoverflow.com/q/45074813
    private const val DEOBFUSCATION_FUNCTION_BODY_REGEX =
        "=\\s*function([\\S\\s]*?\\}\\s*return [\\w$]+?\\.join\\(\"\"\\)\\s*\\};)"

    private const val DEOBFUSCATION_FUNCTION_ARRAY_OBJECT_TYPE_DECLARATION_REGEX = "var "

    private const val FUNCTION_NAMES_IN_DEOBFUSCATION_ARRAY_REGEX = "\\s*=\\s*\\[(.+?)][;,]"

    private const val FUNCTION_ARGUMENTS_REGEX = "=\\s*function\\s*\\(\\s*([^)]*)\\s*\\)"

    private val EARLY_RETURN_REGEX = (";\\s*if\\s*\\(\\s*typeof\\s+" + MULTIPLE_CHARS_REGEX
            + "+\\s*===?\\s*([\"'])undefined\\1\\s*\\)\\s*return\\s+")

    /**
     * Get the throttling parameter deobfuscation function name of YouTube's base JavaScript file.
     *
     * @param javaScriptPlayerCode the complete JavaScript base player code
     * @return the name of the throttling parameter deobfuscation function
     * @throws ParsingException if the name of the throttling parameter deobfuscation function
     * could not be extracted
     */
    
    fun getDeobfuscationFunctionName(javaScriptPlayerCode: String): String? {
        val matchResult: MatchResult? = try {
            Parser.matchMultiplePatterns(
                DEOBFUSCATION_FUNCTION_NAME_REGEXES,
                javaScriptPlayerCode
            )
        } catch (e: RegexException) {
            throw ParsingException(
                "Could not find deobfuscation function with any of the " +
                        "known patterns in the base JavaScript player code", e
            )
        }

        val functionName = matchResult?.groupValues?.getOrNull(1)
            ?: throw ParsingException("Function name group not found")

        if (matchResult.groupValues.size == 2) {
            return functionName
        }

        val arrayNum = matchResult.groupValues.getOrNull(2)?.toIntOrNull()
            ?: throw ParsingException("Invalid or missing array index")

        val arrayRegex = Regex(
            DEOBFUSCATION_FUNCTION_ARRAY_OBJECT_TYPE_DECLARATION_REGEX +
                    Regex.escape(functionName) +
                    FUNCTION_NAMES_IN_DEOBFUSCATION_ARRAY_REGEX
        )

        val arrayStr = Parser.matchGroup1(arrayRegex, javaScriptPlayerCode)
        val names = arrayStr.split(",").map { it.trim() }

        return names.getOrNull(arrayNum)
            ?: throw ParsingException("Array index $arrayNum out of bounds in names list")
    }


    /**
     * Get the throttling parameter deobfuscation code of YouTube's base JavaScript file.
     *
     * @param javaScriptPlayerCode the complete JavaScript base player code
     * @return the throttling parameter deobfuscation function code
     * @throws ParsingException if the throttling parameter deobfuscation code couldn't be
     * extracted
     */
    
    fun getDeobfuscationFunction(
        javaScriptPlayerCode: String,
        functionName: String
    ): String {
        var function = try {
            parseFunctionWithLexer(
                javaScriptPlayerCode,
                functionName
            )
        } catch (e: Exception) {
            parseFunctionWithRegex(
                javaScriptPlayerCode,
                functionName
            )
        }
        return fixupFunction(function)
    }

    /**
     * Get the throttling parameter of a streaming URL if it exists.
     *
     * @param streamingUrl a streaming URL
     * @return the throttling parameter of the streaming URL or `null` if no parameter has
     * been found
     */
    fun getThrottlingParameterFromStreamingUrl(streamingUrl: String): String? {
        return try {
            Parser.matchGroup1(
                THROTTLING_PARAM_PATTERN,
                streamingUrl
            )
        } catch (e: RegexException) {
            // If the throttling parameter could not be parsed from the URL, it means that there is
            // no throttling parameter
            // Return null in this case
            null
        }
    }


    
    private fun parseFunctionWithLexer(
        javaScriptPlayerCode: String,
        functionName: String
    ): String {
        /*val functionBase = "$functionName=function"
        return functionBase + JavaScriptExtractor.matchToClosingBrace(
            javaScriptPlayerCode, functionBase
        ) + ";"*/
        return ""
    }

    /*
    private fun parseFunctionWithRegex(
        javaScriptPlayerCode: String,
        functionName: String
    ): String {
        // Kotlin Regex.escape replaces Pattern.quote
        val functionRegex = Regex(
            Regex.escape(functionName) + DEOBFUSCATION_FUNCTION_BODY_REGEX,
            RegexOption.DOT_MATCHES_ALL
        )
        return validateFunction(
            "function $functionName" +
                    Parser.matchGroup1(functionRegex, javaScriptPlayerCode)
        )
    }*/
    
    private fun parseFunctionWithRegex(
        javaScriptPlayerCode: String,
        functionName: String
    ): String {
        val pattern = "(?s)" + Regex.escape(functionName) + DEOBFUSCATION_FUNCTION_BODY_REGEX
        val functionRegex = Regex(pattern)

        val matchedBody = Parser.matchGroup1(functionRegex, javaScriptPlayerCode)
        return validateFunction("function $functionName$matchedBody")
    }

    private fun validateFunction(function: String): String {
        /*JavaScript.compileOrThrow(function)*/
        return function
    }

    /**
     * Removes an early return statement from the code of the throttling parameter deobfuscation
     * function.
     *
     *
     * In newer version of the player code the function contains a check for something defined
     * outside of the function. If that was not found it will return early.
     *
     *
     * The check can look like this (JS):<br></br>
     * if(typeof RUQ==="undefined")return p;
     *
     *
     * In this example RUQ will always be undefined when running the function as standalone.
     * If the check is kept it would just return p which is the input parameter and would be wrong.
     * For that reason this check and return statement needs to be removed.
     *
     * @param function the original throttling parameter deobfuscation function code
     * @return the throttling parameter deobfuscation function code with the early return statement
     * removed
     */
    /*
    private fun fixupFunction(function: String): String {
        val firstArgName = Parser
            .matchGroup1(FUNCTION_ARGUMENTS_REGEX, function)
            .split(",")
            .map { it.trim() }
            .firstOrNull()
            ?: throw RegexException("Could not extract first argument name")

        val earlyReturnRegex = Regex(
            EARLY_RETURN_REGEX + Regex.escape(firstArgName) + ";",
            RegexOption.DOT_MATCHES_ALL
        )

        return earlyReturnRegex.replaceFirst(function, ";")
    }*/
    
    private fun fixupFunction(function: String): String {
        val firstArgName = Parser
            .matchGroup1(FUNCTION_ARGUMENTS_REGEX, function)
            .split(",")
            .map(String::trim)
            .firstOrNull()
            ?: throw RegexException("Could not extract first argument name")

        val pattern = "(?s)$EARLY_RETURN_REGEX${Regex.escape(firstArgName)};"
        val earlyReturnRegex = Regex(pattern)

        return earlyReturnRegex.replaceFirst(function, ";")
    }
}
