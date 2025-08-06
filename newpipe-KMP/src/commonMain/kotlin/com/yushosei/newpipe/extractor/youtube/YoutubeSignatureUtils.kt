package com.yushosei.newpipe.extractor.youtube

import com.yushosei.newpipe.extractor.exceptions.ParsingException
import com.yushosei.newpipe.extractor.utils.Parser
import com.yushosei.newpipe.extractor.utils.Parser.RegexException

/**
 * Utility class to get the signature timestamp of YouTube's base JavaScript player and deobfuscate
 * signature of streaming URLs from HTML5 clients.
 */
internal object YoutubeSignatureUtils {
    /**
     * The name of the deobfuscation function which needs to be called inside the deobfuscation
     * code.
     */
    const val DEOBFUSCATION_FUNCTION_NAME: String = "deobfuscate"

    private val FUNCTION_REGEXES = listOf( // CHECKSTYLE:OFF
        Regex("\\bm=([a-zA-Z0-9$]{2,})\\(decodeURIComponent\\(h\\.s\\)\\)"),
        Regex("\\bc&&\\(c=([a-zA-Z0-9$]{2,})\\(decodeURIComponent\\(c\\)\\)"),
        Regex("(?:\\b|[^a-zA-Z0-9$])([a-zA-Z0-9$]{2,})\\s*=\\s*function\\(\\s*a\\s*\\)\\s*\\{\\s*a\\s*=\\s*a\\.split\\(\\s*\"\"\\s*\\)"),
        Regex("([\\w$]+)\\s*=\\s*function\\((\\w+)\\)\\{\\s*\\2=\\s*\\2\\.split\\(\"\"\\)\\s*;") // CHECKSTYLE:ON
    )

    private const val STS_REGEX = "signatureTimestamp[=:](\\d+)"

    private const val DEOBF_FUNC_REGEX_START = "("
    private const val DEOBF_FUNC_REGEX_END = "=function\\([a-zA-Z0-9_]+\\)\\{.+?\\})"

    private const val SIG_DEOBF_HELPER_OBJ_NAME_REGEX = ";([A-Za-z0-9_\\$]{2,})\\...\\("
    private const val SIG_DEOBF_HELPER_OBJ_REGEX_START = "(var "
    private const val SIG_DEOBF_HELPER_OBJ_REGEX_END = "=\\{(?>.|\\n)+?\\}\\};)"

    /**
     * Get the signature timestamp property of YouTube's base JavaScript file.
     *
     * @param javaScriptPlayerCode the complete JavaScript base player code
     * @return the signature timestamp
     * @throws ParsingException if the signature timestamp couldn't be extracted
     */
    
    fun getSignatureTimestamp(javaScriptPlayerCode: String): String {
        try {
            return Parser.matchGroup1(STS_REGEX, javaScriptPlayerCode)
        } catch (e: ParsingException) {
            throw ParsingException(
                "Could not extract signature timestamp from JavaScript code", e
            )
        }
    }

    /**
     * Get the signature deobfuscation code of YouTube's base JavaScript file.
     *
     * @param javaScriptPlayerCode the complete JavaScript base player code
     * @return the signature deobfuscation code
     * @throws ParsingException if the signature deobfuscation code couldn't be extracted
     */
    
    fun getDeobfuscationCode(javaScriptPlayerCode: String): String {
        try {
            val deobfuscationFunctionName = getDeobfuscationFunctionName(
                javaScriptPlayerCode
            )
            var deobfuscationFunction = try {
                getDeobfuscateFunctionWithLexer(
                    javaScriptPlayerCode, deobfuscationFunctionName
                )
            } catch (e: Exception) {
                getDeobfuscateFunctionWithRegex(
                    javaScriptPlayerCode, deobfuscationFunctionName
                )
            }

            /*// Assert the extracted deobfuscation function is valid
            JavaScript.compileOrThrow(deobfuscationFunction)*/

            val helperObjectName =
                Parser.matchGroup1(SIG_DEOBF_HELPER_OBJ_NAME_REGEX, deobfuscationFunction)

            val helperObject = getHelperObject(javaScriptPlayerCode, helperObjectName)

            val callerFunction = ("function " + DEOBFUSCATION_FUNCTION_NAME
                    + "(a){return "
                    + deobfuscationFunctionName
                    + "(a);}")

            return "$helperObject$deobfuscationFunction;$callerFunction"
        } catch (e: Exception) {
            throw ParsingException("Could not parse deobfuscation function", e)
        }
    }


    
    private fun getDeobfuscationFunctionName(javaScriptPlayerCode: String): String {
        try {
            return Parser.matchGroup1MultiplePatterns(FUNCTION_REGEXES, javaScriptPlayerCode)
        } catch (e: RegexException) {
            throw ParsingException(
                "Could not find deobfuscation function with any of the known patterns", e
            )
        }
    }


    
    private fun getDeobfuscateFunctionWithLexer(
        javaScriptPlayerCode: String,
        deobfuscationFunctionName: String
    ): String {
        /*val functionBase = "$deobfuscationFunctionName=function"
        return functionBase + JavaScriptExtractor.matchToClosingBrace(
            javaScriptPlayerCode, functionBase
        )*/
        return ""
    }


    
    private fun getDeobfuscateFunctionWithRegex(
        javaScriptPlayerCode: String,
        deobfuscationFunctionName: String
    ): String {
        val functionPattern = DEOBF_FUNC_REGEX_START +
                Regex.escape(deobfuscationFunctionName) +
                DEOBF_FUNC_REGEX_END

        return "var " + Parser.matchGroup1(functionPattern, javaScriptPlayerCode)
    }

    
    private fun getHelperObject(
        javaScriptPlayerCode: String,
        helperObjectName: String
    ): String {
        val helperPattern = SIG_DEOBF_HELPER_OBJ_REGEX_START +
                Regex.escape(helperObjectName) +
                SIG_DEOBF_HELPER_OBJ_REGEX_END

        return Parser.matchGroup1(helperPattern, javaScriptPlayerCode).replace("\n", "")
    }
}
