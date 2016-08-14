package com.ne1c.gitteroid.utils

import android.util.Patterns
import java.util.*
import java.util.regex.Pattern

class MarkdownUtils(message: String?) {

    private val mMessage: String

    private var mParsedString: MutableList<String>? = null
    private var mMultilineCode: MutableList<String>? = null
    private var mSinglelineCode: MutableList<String>? = null
    private var mBold: MutableList<String>? = null
    private var mItalics: MutableList<String>? = null
    private var mStrikethrough: MutableList<String>? = null
    private var mQuote: MutableList<String>? = null
    private var mGitterLinks: MutableList<String>? = null
    private var mLinks: MutableList<String>? = null
    private var mImageLinks: MutableList<Any>? = null
    private var mIssues: MutableList<String>? = null
    private var mMentions: MutableList<String>? = null

    init {
        if (message == null) {
            throw NullPointerException("Message is null")
        } else {
            mMessage = message
        }
    }

    private fun convertWithPatterns(message: String): MutableList<String> {
        var message = message
        if (mParsedString == null) {
            mParsedString = ArrayList<String>()
        } else {
            return mParsedString as MutableList<String>
        }

        message = message.replace(SINGLELINE_CODE_PATTERN.pattern().toRegex(), "{" + SINGLELINE_CODE.toString() + "}")
        message = message.replace(MULTILINE_CODE_PATTERN.pattern().toRegex(), "{" + MULTILINE_CODE.toString() + "}")
        message = message.replace(BOLD_PATTERN.pattern().toRegex(), "{" + BOLD.toString() + "}")
        message = message.replace(ITALICS_PATTERN.pattern().toRegex(), "{" + ITALICS.toString() + "}")
        message = message.replace(STRIKETHROUGH_PATTERN.pattern().toRegex(), "{" + STRIKETHROUGH.toString() + "}")
        message = message.replace(QUOTE_PATTERN.pattern().toRegex(), "{" + QUOTE.toString() + "}")
        message = message.replace(PREVIEW_IMAGE_LINK_PATTERN.pattern().toRegex(), "{" + IMAGE_LINK.toString() + "}")
        message = message.replace(IMAGE_LINK_PATTERN.pattern().toRegex(), "{" + IMAGE_LINK.toString() + "}")
        message = message.replace(GITTER_LINK_PATTERN.pattern().toRegex(), "{" + GITTER_LINK.toString() + "}")
        message = message.replace(MENTION_PATTERN.pattern().toRegex(), "{" + MENTIONS.toString() + "}")

        val matcher = Pattern.compile("\\{\\d+\\}").matcher(message)
        val splitted = message.split("\\{\\d+\\}".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        var find = matcher.find()

        if (find) {
            var i = 0
            do {
                if (i < splitted.size && splitted[i] != "\n" && !splitted[i].isEmpty()) {
                    var text = splitted[i]

                    if (!text.isEmpty()) {
                        if (text.substring(0, 1) == "\n") {
                            text = text.substring(1)
                        }

                        if (text.substring(text.length - 1) == "\n") {
                            text = text.substring(0, text.length - 1)
                        }
                    }

                    mParsedString!!.add(text)
                }

                if (find) {
                    mParsedString!!.add(matcher.group())
                }

                i++
                find = matcher.find()
            } while (i < splitted.size || find)
        } else {
            mParsedString!!.addAll(Arrays.asList(*splitted))
        }

        return mParsedString as ArrayList<String>
    }

    private fun readMultilineCode(message: String): List<String> {
        val matcher = MULTILINE_CODE_PATTERN.matcher(message)

        if (mMultilineCode != null) {
            return mMultilineCode as MutableList<String>
        }

        if (matcher.find()) {
            mMultilineCode = ArrayList<String>()

            do {
                var code = matcher.group().replace("```", "")

                if (code != "\n") {
                    code = code.substring(1, code.length - 1) // remove \n
                }

                mMultilineCode!!.add(code)
            } while (matcher.find())
            return mMultilineCode as ArrayList<String>
        }

        return emptyList()
    }

    private fun readSinglelineCode(message: String): List<String> {
        val matcher = SINGLELINE_CODE_PATTERN.matcher(message)

        if (mSinglelineCode != null) {
            return mSinglelineCode as MutableList<String>
        }

        if (matcher.find()) {
            mSinglelineCode = ArrayList<String>()

            do {
                mSinglelineCode!!.add(matcher.group().replace("`", ""))
            } while (matcher.find())

            return mSinglelineCode as ArrayList<String>
        }

        return emptyList()
    }

    private fun readBold(message: String): List<String> {
        val matcher = BOLD_PATTERN.matcher(message)

        if (mBold != null) {
            return mBold as MutableList<String>
        }

        if (matcher.find()) {
            mBold = ArrayList<String>()

            do {
                mBold!!.add(matcher.group().replace("**", ""))
            } while (matcher.find())

            return mBold as ArrayList<String>
        }

        return emptyList()
    }

    private fun readStrikethrough(message: String): MutableList<String> {
        val matcher = STRIKETHROUGH_PATTERN.matcher(message)

        if (mStrikethrough != null) {
            return mStrikethrough as MutableList<String>
        }

        if (matcher.find()) {
            mStrikethrough = ArrayList<String>()

            do {
                mStrikethrough!!.add(matcher.group().replace("~~", ""))
            } while (matcher.find())

            return mStrikethrough as ArrayList<String>
        }

        return mutableListOf()
    }

    private fun readQuote(message: String): List<String> {
        val matcher = QUOTE_PATTERN.matcher(message)

        if (mQuote != null) {
            return mQuote as MutableList<String>
        }

        if (matcher.find()) {
            mQuote = ArrayList<String>()

            do {
                var text = matcher.group().replaceFirst(">\\s?".toRegex(), "")

                if (text.length > 0) {
                    if (text.substring(0, 1) == "\n") {
                        text = text.substring(1)
                    }

                    if (text.substring(text.length - 1) == "\n") {
                        text = text.substring(0, text.length - 1)
                    }
                }

                mQuote!!.add(text)
            } while (matcher.find())

            return mQuote as ArrayList<String>
        }

        return emptyList()
    }

    private fun readItalics(message: String): List<String> {
        val matcher = ITALICS_PATTERN.matcher(message)

        if (mItalics != null) {
            return mItalics as MutableList<String>
        }

        if (matcher.find()) {
            mItalics = ArrayList<String>()

            do {
                val text = matcher.group() // remove *
                mItalics!!.add(text.substring(1, text.length - 1))
            } while (matcher.find())

            return mItalics as ArrayList<String>
        }

        return emptyList()
    }

    private fun readGitterLinks(message: String): List<String> {
        val matcher = GITTER_LINK_PATTERN.matcher(message)

        if (mGitterLinks != null) {
            return mGitterLinks as MutableList<String>
        }

        if (matcher.find()) {
            mGitterLinks = ArrayList<String>()

            do {
                mGitterLinks!!.add(matcher.group().replaceFirst("\\[]\\((\\bhttp\\b|\\bhttps\\b)://\\)".toRegex(), ""))
            } while (matcher.find())

            return mGitterLinks as ArrayList<String>
        }

        return emptyList()
    }

    private fun readLinks(message: String): List<String> {
        val matcher = LINK_PATTERN.matcher(message)

        if (mLinks != null) {
            return mLinks as MutableList<String>
        }

        if (matcher.find()) {
            mLinks = ArrayList<String>()

            do {
                mLinks!!.add(matcher.group())
            } while (matcher.find())

            return mLinks as ArrayList<String>
        }

        return emptyList()
    }

    private fun readImageLinks(message: String): List<Any> {
        val preview_matcher = PREVIEW_IMAGE_LINK_PATTERN.matcher(message)
        val full_matcher = IMAGE_LINK_PATTERN.matcher(message)

        if (mImageLinks != null) {
            return mImageLinks as MutableList<Any>
        }

        if (preview_matcher.find()) {
            mImageLinks = ArrayList<Any>()

            do {
                val previewUrl = preview_matcher.group().replaceFirst("\\[!\\[\\b.*\\b]\\(".toRegex(), "").replaceFirst("(\\)\\].*)+".toRegex(), "")
                val fullUrl = preview_matcher.group().replaceFirst("\\[!\\[\\b.*\\b]\\((.*?)\\)\\]\\(".toRegex(), "").replaceFirst("\\)".toRegex(), "")

                val link = PreviewImageModel(previewUrl, fullUrl)

                mImageLinks!!.add(link)
            } while (preview_matcher.find())

            return mImageLinks as MutableList<Any>
        } else if (full_matcher.find()) {
            mImageLinks = ArrayList<Any>()

            do {
                mImageLinks!!.add(full_matcher.group().replaceFirst("!\\[.*?]\\(".toRegex(), "").replace(")", ""))
            } while (full_matcher.find())

            return mImageLinks as MutableList<Any>
        }

        return emptyList()
    }

    private fun readIssues(message: String): List<String> {
        val matcher = ISSUE_PATTERN.matcher(message)

        if (mIssues != null) {
            return mIssues as MutableList<String>
        }

        if (matcher.find()) {
            mIssues = ArrayList<String>()

            do {
                mIssues!!.add(matcher.group().replace("#", ""))
            } while (matcher.find())

            return mIssues as ArrayList<String>
        }

        return emptyList()
    }

    private fun readMentions(message: String): List<String> {
        val matcher = MENTION_PATTERN.matcher(message)

        if (mMentions != null) {
            return mMentions as MutableList<String>
        }

        if (matcher.find()) {
            mMentions = ArrayList<String>()

            do {
                mMentions!!.add(matcher.group())
            } while (matcher.find())

            return mMentions as ArrayList<String>
        }

        return emptyList()
    }

    val singlelineCode: List<String>
        get() = readSinglelineCode(mMessage)

    val multilineCode: List<String>
        get() = readMultilineCode(mMessage)

    val bold: List<String>
        get() = readBold(mMessage)

    val strikethrough: List<String>
        get() = readStrikethrough(mMessage)

    val quote: List<String>
        get() = readQuote(mMessage)

    val italics: List<String>
        get() = readItalics(mMessage)

    val gitterLinks: List<String>
        get() = readGitterLinks(mMessage)

    val links: List<String>
        get() = readLinks(mMessage)

    val imageLinks: List<Any>
        get() = readImageLinks(mMessage)

    val issues: List<String>
        get() = readIssues(mMessage)

    val mentions: List<String>
        get() = readMentions(mMessage)

    val parsedString: List<String>
        get() = convertWithPatterns(mMessage)

    fun existMarkdown(): Boolean {
        return singlelineCode.size > 0 ||
                multilineCode.size > 0 ||
                bold.size > 0 ||
                strikethrough.size > 0 ||
                quote.size > 0 ||
                italics.size > 0 ||
                imageLinks.size > 0 ||
                gitterLinks.size > 0 ||
                links.size > 0
    }

    class PreviewImageModel(private val mPreviewUrl: String?, private val mFullUrl: String?) {

        val previewUrl: String
            get() = mPreviewUrl ?: ""

        val fullUrl: String
            get() = mFullUrl ?: ""
    }

    companion object {
        val SINGLELINE_CODE = 0
        val MULTILINE_CODE = 1
        val BOLD = 2
        val ITALICS = 3
        val STRIKETHROUGH = 4
        val QUOTE = 5
        val GITTER_LINK = 6
        val IMAGE_LINK = 7
        val ISSUE = 8
        val MENTIONS = 9
        val LINK = 10

        private val SINGLELINE_CODE_PATTERN = Pattern.compile("((?!``)`(.+?)`(?!```))|(```(.+?)```)") // `code` or ```code```
        private val MULTILINE_CODE_PATTERN = Pattern.compile("```(.|\\n?)+?```")  // ```code``` multiline
        private val BOLD_PATTERN = Pattern.compile("[*]{2}(.+?)[*]{2}") // **bold**
        private val ITALICS_PATTERN = Pattern.compile("\\*(.+?)\\*") // *italics*
        private val STRIKETHROUGH_PATTERN = Pattern.compile("~{2}(.+?)~{2}") // ~~strikethrough~~
        private val QUOTE_PATTERN = Pattern.compile("(\\n|^)>(.+?[\\n]?)+", Pattern.MULTILINE) // >blockquote
        private val ISSUE_PATTERN = Pattern.compile("#(.+?)\\S+") // #123
        private val GITTER_LINK_PATTERN = Pattern.compile("\\[.*?]\\((\\bhttp\\b|\\bhttps\\b):\\/\\/.*?\\)") // [title](http://)
        private val LINK_PATTERN = Patterns.WEB_URL
        private val IMAGE_LINK_PATTERN = Pattern.compile("!\\[.*?]\\((\\bhttp\\b|\\bhttps\\b):\\/\\/.*?\\)") // ![alt](http://)
        private val PREVIEW_IMAGE_LINK_PATTERN = Pattern.compile("\\[!\\[.*?]\\((\\bhttp\\b|\\bhttps\\b):\\/\\/.*?\\)]\\((\\bhttp\\b|\\bhttps\\b):\\/\\/.*?\\)") // [![alt](preview_url)(full_url)]
        private val MENTION_PATTERN = Pattern.compile("@(\\w*-?\\w)*")
    }
}