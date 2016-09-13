package com.ne1c.gitteroid.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.*
import android.text.util.Linkify
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.ne1c.gitteroid.R
import com.ne1c.gitteroid.ui.SinglelineSpan
import com.ne1c.gitteroid.utils.MarkdownUtils

class MarkdownViews(private val context: Context) {
    fun getSinglelineCodeSpan(fromStartPos: Int, text: String): Spannable {
        val span = SpannableString(text)
        span.setSpan(SinglelineSpan(fromStartPos, fromStartPos + text.length), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        return span
    }

    val multilineCodeView: FrameLayout
        get() = LayoutInflater.from(context).inflate(R.layout.view_multiline_code, null) as FrameLayout

    fun getBoldSpannableText(text: String): Spannable {
        val span = SpannableString(text)
        span.setSpan(StyleSpan(Typeface.BOLD), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        return span
    }

    fun getItalicSpannableText(text: String): Spannable {
        val span = SpannableString(text)
        span.setSpan(StyleSpan(Typeface.ITALIC), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        return span
    }

    fun getStrikethroughSpannableText(text: String): Spannable {
        val span = SpannableString(text)
        span.setSpan(StrikethroughSpan(), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        return span
    }

    fun getQuoteText(text: String): LinearLayout {
        val parent = LayoutInflater.from(context).inflate(R.layout.view_quote, null) as LinearLayout

        (parent.findViewById(R.id.text_quote) as TextView).text = text

        return parent
    }

    fun getLinksSpannableText(text: String, link: String): Spannable {
        val span = SpannableString(text)
        span.setSpan(URLSpan(link), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        span.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
               // loadUrlWithChromeTabs(context, link)
            }
        }, 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        return span
    }

    val linkImage: ImageView
        get() = LayoutInflater.from(context).inflate(R.layout.view_image_link, null) as ImageView

    fun getIssueSpannableText(text: String): Spannable {
        val span = SpannableString(text)
        span.setSpan(ForegroundColorSpan(Color.GREEN), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        return span
    }

    val textView: TextView
        @SuppressLint("PrivateResource")
        get() {
            val view = TextView(context)
            view.setTextColor(context.resources.getColor(R.color.primary_text_default_material_light))
            view.autoLinkMask = Linkify.ALL
            view.linksClickable = true
            return view
        }

    fun getMentionSpannableText(text: String): Spannable {
        val span = SpannableString(text)
        span.setSpan(StyleSpan(Typeface.ITALIC), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        span.setSpan(UnderlineSpan(), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        return span
    }

    fun bindMarkdownText(holder: MessagesAdapter.DynamicViewHolder, text: String) {
        val markdown = MarkdownUtils(text)
        var formatText = text
        
        var counterSingleline = -1
        var counterMultiline = -1
        var counterBold = -1
        var counterItalics = -1
        var counterQuote = -1
        var counterStrikethrough = -1
        var counterIssue = -1
        var counterGitterLinks = -1
        val counterLinks = -1
        var counterImageLinks = -1
        var counterMentions = -1
        
        holder.messageLayout.removeAllViews()

        if (markdown.existMarkdown()) {
            for (i in 0..markdown.parsedString.size - 1) {
                when (markdown.parsedString[i]) {
                    "{0}" // Singleline code
                    -> if (holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) is TextView) {
                        val textView = holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) as TextView
                        textView.append(getSinglelineCodeSpan(textView.text.length,
                                markdown.singlelineCode[++counterSingleline]))
                    } else {
                        val textView = textView
                        textView.text = getSinglelineCodeSpan(textView.text.length,
                                markdown.singlelineCode[++counterSingleline])
                        holder.messageLayout.addView(textView)
                    }
                    "{1}" // Multiline code
                    -> {
                        val multiline = multilineCodeView
                        (multiline.findViewById(R.id.multiline_textView) as TextView).text = markdown.multilineCode[++counterMultiline]
                        holder.messageLayout.addView(multiline)
                    }
                    "{2}" // Bold
                    -> if (holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) is TextView) {
                        (holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) as TextView).append(getBoldSpannableText(markdown.bold[++counterBold]))
                    } else {
                        val textView = textView
                        textView.text = getBoldSpannableText(markdown.bold[++counterBold])
                        holder.messageLayout.addView(textView)
                    }
                    "{3}" // Italics
                    -> if (holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) is TextView) {
                        (holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) as TextView).append(getItalicSpannableText(markdown.italics[++counterItalics]))
                    } else {
                        val textView = textView
                        textView.text = getItalicSpannableText(markdown.italics[++counterItalics])
                        holder.messageLayout.addView(textView)
                    }
                    "{4}" // Strikethrough
                    -> if (holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) is TextView) {
                        (holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) as TextView).append(getStrikethroughSpannableText(markdown.strikethrough[++counterStrikethrough]))
                    } else {
                        val strikeView = textView
                        strikeView.text = getStrikethroughSpannableText(markdown.strikethrough[++counterStrikethrough])
                        holder.messageLayout.addView(strikeView)
                    }
                    "{5}" // Quote
                    -> {
                        val quote = getQuoteText(markdown.quote[++counterQuote])
                        holder.messageLayout.addView(quote)
                    }
                    "{6}" // GitterLinks
                    -> if (holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) is TextView) {
                        var link = markdown.gitterLinks[++counterGitterLinks]

                        var url = "http://gitter.im"

                        val urlMatcher = Patterns.WEB_URL.matcher(link)
                        if (urlMatcher.find()) {
                            url = urlMatcher.group()

                            if (url.substring(url.length - 1, url.length) == ")") {
                                url = url.substring(0, url.length - 1)
                            }
                        }

                        link = link.substring(1, link.indexOf("]"))

                        val textView = holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) as TextView
                        textView.append(getLinksSpannableText(link, url))
                        textView.movementMethod = LinkMovementMethod.getInstance()
                    } else {
                        val textView = textView
                        var link = markdown.gitterLinks[++counterGitterLinks]

                        var url = "http://gitter.im"

                        val urlMatcher = Patterns.WEB_URL.matcher(link)
                        if (urlMatcher.find()) {
                            url = urlMatcher.group()

                            if (url.substring(url.length - 1, url.length) == ")") {
                                url = url.substring(0, url.length - 1)
                            }
                        }

                        link = link.substring(1, link.indexOf("]"))

                        textView.text = getLinksSpannableText(link, url)
                        textView.movementMethod = LinkMovementMethod.getInstance()

                        holder.messageLayout.addView(textView)
                    }
                    "{7}" // Image
                    -> {
                        val link = markdown.imageLinks[++counterImageLinks]
                        val previewUrl: String
                        val fullUrl: String

                        if (link is MarkdownUtils.PreviewImageModel) {
                            previewUrl = link.previewUrl
                            fullUrl = link.fullUrl
                        } else {
                            previewUrl = link.toString()
                            fullUrl = previewUrl
                        }

                        val image = linkImage
                        image.setOnClickListener { v -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))) }

                        holder.messageLayout.addView(image, 256, 192)

                        //linkImage = linkImage.substring(linkImage.indexOf("http"), linkImage.length() - 2);
                        Glide.with(context).load(previewUrl).crossFade(500).into(image)
                    }
                    "{8}" // Issue
                    -> {
                        val issue = textView
                        issue.text = getIssueSpannableText(markdown.issues[++counterIssue])
                        holder.messageLayout.addView(issue)
                    }
                    "{9}" // Mention
                    -> if (holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) is TextView) {
                        (holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) as TextView).append(getMentionSpannableText(markdown.mentions[++counterMentions]))
                    } else {
                        val mentionView = textView
                        mentionView.text = getMentionSpannableText(markdown.mentions[++counterMentions])
                        holder.messageLayout.addView(mentionView)
                    }
                //                    case "{10}": // Links
                //                        if (holder.messageLayout.getChildAt(holder.messageLayout.getChildCount() - 1) instanceof TextView) {
                //                            String url = markdown.getLinks().get(++counterLinks);
                //
                //                            TextView textView = (TextView) holder.messageLayout.getChildAt(holder.messageLayout.getChildCount() - 1);
                //                            textView.append(getLinksSpannableText(url, url));
                //                            textView.setMovementMethod(LinkMovementMethod.getInstance());
                //                        } else {
                //                            TextView textView = getTextView();
                //                            String url = markdown.getLinks().get(++counterLinks);
                //
                //                            textView.setText(getLinksSpannableText(url, url));
                //                            textView.setMovementMethod(LinkMovementMethod.getInstance());
                //
                //                            holder.messageLayout.addView(textView);
                //                        }
                //
                //                        break;
                    else // Text
                    -> if (holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) is TextView) {
                        (holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) as TextView).append(markdown.parsedString[i])
                    } else {
                        val textView = textView
                        textView.text = markdown.parsedString[i]
                        holder.messageLayout.addView(textView)
                    }
                }//                        ViewGroup.LayoutParams params = image.getLayoutParams();
                //                        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                //                        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                //                        image.setLayoutParams(params);
            }
        } else {
            val textView = textView

            if (text.substring(text.length - 1) == "\n") {
                formatText = text.substring(0, text.length - 1)
            }
            textView.text = formatText

            holder.messageLayout.addView(textView)
        }

        holder.messageLayout.requestLayout()

    }
}