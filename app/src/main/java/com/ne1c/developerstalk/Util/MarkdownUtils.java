package com.ne1c.developerstalk.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownUtils {
    private static final Pattern SINGLELINE_CODE_PATTERN = Pattern.compile("`(.*?)`"); // `code`
    private static final Pattern MULTILINE_CODE_PATTERN = Pattern.compile("'''(.*?)'''");  // '''code'''
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*{2}(.*?)\\*{2}"); // **bold**
    private static final Pattern ITALICS_PATTERN = Pattern.compile("[*](.*?)[*]"); // *italics*
    private static final Pattern STRIKETHROUGH_PATTERN = Pattern.compile("~{2}.*?~{2}"); // ~~strikethrough~~
    private static final Pattern QUOTE_PATTERN = Pattern.compile(">\\s.*?\\s*"); // > blockquote
    private static final Pattern ISSUE_PATTERN = Pattern.compile("#.*?\\S"); // #123
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[\\.*?\\S]\\(\\bhttp\\b://.*?\\)"); // [title](http://)
    private static final Pattern IMAGE_LINK_PATTERN = Pattern.compile("!\\[\\balt\\b]\\(\\bhttp\\b://.*?\\)"); // ![alt](http://)

    private final String mMessage;

    private List<String> mMultilineCode;
    private List<String> mSinglelineCode;
    private List<String> mBold;
    private List<String> mItalics;
    private List<String> mStrikethrough;
    private List<String> mQuote;
    private List<String> mLinks;
    private List<String> mImageLinks;
    private List<String> mIssues;

    public MarkdownUtils(String message) {
        if (message == null) {
            throw new NullPointerException("Message is null");
        } else {
            mMessage = message;
        }
    }

    private List<String> readMultilineCode(String message) {
        Matcher matcher = MULTILINE_CODE_PATTERN.matcher(message);

        if (matcher.find()) {
            if (mMultilineCode == null) {
                mMultilineCode = new ArrayList<>();
            } else {
                return mMultilineCode;
            }

            for (int i = 0; i < matcher.groupCount(); i++) {
                mMultilineCode.add(matcher.group(i).replace("\'\'\'", ""));
            }
            return mMultilineCode;
        }

        return Collections.EMPTY_LIST;
    }

    private List<String> readSinglelineCode(String message) {
        Matcher matcher = SINGLELINE_CODE_PATTERN.matcher(message);

        if (matcher.find()) {
            if (mSinglelineCode == null) {
                mSinglelineCode = new ArrayList<>();
            } else  {
                return mSinglelineCode;
            }

            for (int i = 0; i < matcher.groupCount(); i++) {
                mSinglelineCode.add(matcher.group(i).replace("`", ""));
            }

            return mSinglelineCode;
        }

        return Collections.EMPTY_LIST;
    }

    private List<String> readBold(String message) {
        Matcher matcher = BOLD_PATTERN.matcher(message);

        if (matcher.find()) {
            if (mBold == null) {
                mBold = new ArrayList<>();
            } else  {
                return mBold;
            }

            for (int i = 0; i < matcher.groupCount(); i++) {
                mBold.add(matcher.group(i).replace("**", ""));
            }

            return mBold;
        }

        return Collections.EMPTY_LIST;
    }

    private List<String> readStrikethrough(String message) {
        Matcher matcher = STRIKETHROUGH_PATTERN.matcher(message);

        if (matcher.find()) {
            if (mStrikethrough == null) {
                mStrikethrough = new ArrayList<>();
            } else  {
                return mStrikethrough;
            }

            for (int i = 0; i < matcher.groupCount(); i++) {
                mStrikethrough.add(matcher.group(i).replace("~~", ""));
            }

            return mStrikethrough;
        }

        return Collections.EMPTY_LIST;
    }

    private List<String> readQuote(String message) {
        Matcher matcher = QUOTE_PATTERN.matcher(message);

        if (matcher.find()) {
            if (mQuote == null) {
                mQuote = new ArrayList<>();
            } else  {
                return mQuote;
            }

            for (int i = 0; i < matcher.groupCount(); i++) {
                mQuote.add(matcher.group(i).replace(">", ""));
            }

            return mQuote;
        }

        return Collections.EMPTY_LIST;
    }

    private List<String> readItalics(String message) {
        Matcher matcher = ITALICS_PATTERN.matcher(message);

        if (matcher.find()) {
            if (mItalics == null) {
                mItalics = new ArrayList<>();
            } else  {
                return mItalics;
            }

            for (int i = 0; i < matcher.groupCount(); i++) {
                mItalics.add(matcher.group(i).replace("*", ""));
            }

            return mBold;
        }

        return Collections.EMPTY_LIST;
    }

    private List<String> readLinks(String message) {
        Matcher matcher = LINK_PATTERN.matcher(message);

        if (matcher.find()) {
            if (mLinks == null) {
                mLinks = new ArrayList<>();
            } else  {
                return mLinks;
            }

            for (int i = 0; i < matcher.groupCount(); i++) {
                mLinks.add(matcher.group(i).replaceFirst("\\[]\\(\\bhttp\\b://\\)", ""));
            }

            return mLinks;
        }

        return Collections.EMPTY_LIST;
    }

    private List<String> readImageLinks(String message) {
        Matcher matcher = IMAGE_LINK_PATTERN.matcher(message);

        if (matcher.find()) {
            if (mImageLinks == null) {
                mImageLinks = new ArrayList<>();
            } else  {
                return mImageLinks;
            }

            for (int i = 0; i < matcher.groupCount(); i++) {
                mImageLinks.add(matcher.group(i).replaceFirst("!\\[\\balt\\b]\\(\\bhttp\\b://.*?\\)", ""));
            }

            return mLinks;
        }

        return Collections.EMPTY_LIST;
    }

    private List<String> readIssues(String message) {
        Matcher matcher = ISSUE_PATTERN.matcher(message);

        if (matcher.find()) {
            if (mIssues == null) {
                mIssues = new ArrayList<>();
            } else  {
                return mIssues;
            }

            for (int i = 0; i < matcher.groupCount(); i++) {
                mIssues.add(matcher.group(i).replace("#", ""));
            }

            return mIssues;
        }

        return Collections.EMPTY_LIST;
    }

    public List<String> getSinglelineCode() {
        return readSinglelineCode(mMessage);
    }

    public List<String> getMultilineCode() {
        return readMultilineCode(mMessage);
    }

    public List<String> getBold() {
        return readBold(mMessage);
    }

    public List<String> getStrikethrough() {
        return readStrikethrough(mMessage);
    }

    public List<String> getQuote() {
        return readQuote(mMessage);
    }

    public List<String> getItalics() {
        return readItalics(mMessage);
    }

    public List<String> getLinks() {
        return readLinks(mMessage);
    }

    public List<String> getImageLinks() {
        return readImageLinks(mMessage);
    }

    public List<String> getIssues() {
        return readIssues(mMessage);
    }
}