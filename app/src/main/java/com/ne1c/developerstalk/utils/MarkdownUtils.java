package com.ne1c.developerstalk.utils;

import android.util.Patterns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownUtils {
    public static final int SINGLELINE_CODE = 0;
    public static final int MULTILINE_CODE = 1;
    public static final int BOLD = 2;
    public static final int ITALICS = 3;
    public static final int STRIKETHROUGH = 4;
    public static final int QUOTE = 5;
    public static final int GITTER_LINK = 6;
    public static final int IMAGE_LINK = 7;
    public static final int ISSUE = 8;
    public static final int MENTIONS = 9;
    public static final int LINK = 10;

    private static final Pattern SINGLELINE_CODE_PATTERN = Pattern.compile("((?!``)`(.+?)`(?!```))|(```(.+?)```)"); // `code` or ```code```
    private static final Pattern MULTILINE_CODE_PATTERN = Pattern.compile("```(.|\\n?)+?```");  // ```code``` multiline
    private static final Pattern BOLD_PATTERN = Pattern.compile("[*]{2}(.+?)[*]{2}"); // **bold**
    private static final Pattern ITALICS_PATTERN = Pattern.compile("\\*(.+?)\\*"); // *italics*
    private static final Pattern STRIKETHROUGH_PATTERN = Pattern.compile("~{2}(.+?)~{2}"); // ~~strikethrough~~
    private static final Pattern QUOTE_PATTERN = Pattern.compile("(\\n|^)>(.+?[\\n]?)+", Pattern.MULTILINE); // >blockquote
    private static final Pattern ISSUE_PATTERN = Pattern.compile("#(.+?)\\S+"); // #123
    private static final Pattern GITTER_LINK_PATTERN = Pattern.compile("\\[.*?]\\((\\bhttp\\b|\\bhttps\\b):\\/\\/.*?\\)"); // [title](http://)
    private static final Pattern LINK_PATTERN = Patterns.WEB_URL;
    private static final Pattern IMAGE_LINK_PATTERN = Pattern.compile("!\\[.*?]\\((\\bhttp\\b|\\bhttps\\b):\\/\\/.*?\\)"); // ![alt](http://)
    private static final Pattern PREVIEW_IMAGE_LINK_PATTERN =
            Pattern.compile("\\[!\\[.*?]\\((\\bhttp\\b|\\bhttps\\b):\\/\\/.*?\\)]\\((\\bhttp\\b|\\bhttps\\b):\\/\\/.*?\\)"); // [![alt](preview_url)(full_url)]
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w*-?\\w)*");

    private final String mMessage;

    private List<String> mParsedString;
    private List<String> mMultilineCode;
    private List<String> mSinglelineCode;
    private List<String> mBold;
    private List<String> mItalics;
    private List<String> mStrikethrough;
    private List<String> mQuote;
    private List<String> mGitterLinks;
    private List<String> mLinks;
    private List<Object> mImageLinks;
    private List<String> mIssues;
    private List<String> mMentions;

    public MarkdownUtils(String message) {
        if (message == null) {
            throw new NullPointerException("Message is null");
        } else {
            mMessage = message;
        }
    }

    private List<String> convertWithPatterns(String message) {
        if (mParsedString == null) {
            mParsedString = new ArrayList<>();
        } else {
            return mParsedString;
        }

        message = message.replaceAll(SINGLELINE_CODE_PATTERN.pattern(), "{" + String.valueOf(SINGLELINE_CODE) + "}");
        message = message.replaceAll(MULTILINE_CODE_PATTERN.pattern(), "{" + String.valueOf(MULTILINE_CODE) + "}");
        message = message.replaceAll(BOLD_PATTERN.pattern(), "{" + String.valueOf(BOLD) + "}");
        message = message.replaceAll(ITALICS_PATTERN.pattern(), "{" + String.valueOf(ITALICS) + "}");
        message = message.replaceAll(STRIKETHROUGH_PATTERN.pattern(), "{" + String.valueOf(STRIKETHROUGH) + "}");
        message = message.replaceAll(QUOTE_PATTERN.pattern(), "{" + String.valueOf(QUOTE) + "}");
        message = message.replaceAll(PREVIEW_IMAGE_LINK_PATTERN.pattern(), "{" + String.valueOf(IMAGE_LINK) + "}");
        message = message.replaceAll(IMAGE_LINK_PATTERN.pattern(), "{" + String.valueOf(IMAGE_LINK) + "}");
        message = message.replaceAll(GITTER_LINK_PATTERN.pattern(), "{" + String.valueOf(GITTER_LINK) + "}");
        message = message.replaceAll(MENTION_PATTERN.pattern(), "{" + String.valueOf(MENTIONS) + "}");

        Matcher matcher = Pattern.compile("\\{\\d+\\}").matcher(message);
        String[] splitted = message.split("\\{\\d+\\}");

        boolean find = matcher.find();

        if (find) {
            int i = 0;
            do {
                if (i < splitted.length && !splitted[i].equals("\n") && !splitted[i].isEmpty()) {
                    String text = splitted[i];

                    if (!text.isEmpty()) {
                        if (text.substring(0, 1).equals("\n")) {
                            text = text.substring(1);
                        }

                        if (text.substring(text.length() - 1).equals("\n")) {
                            text = text.substring(0, text.length() - 1);
                        }
                    }

                    mParsedString.add(text);
                }

                if (find) {
                    mParsedString.add(matcher.group());
                }

                i++;
                find = matcher.find();
            } while (i < splitted.length || find);
        } else {
            mParsedString.addAll(Arrays.asList(splitted));
        }

        return mParsedString;
    }

    private List<String> readMultilineCode(String message) {
        Matcher matcher = MULTILINE_CODE_PATTERN.matcher(message);

        if (mMultilineCode != null) {
            return mMultilineCode;
        }

        if (matcher.find()) {
            mMultilineCode = new ArrayList<>();

            do {
                String code = matcher.group().replace("```", "");

                if (!code.equals("\n")) {
                    code = code.substring(1, code.length() - 1); // remove \n
                }

                mMultilineCode.add(code);
            } while (matcher.find());
            return mMultilineCode;
        }

        return Collections.emptyList();
    }

    private List<String> readSinglelineCode(String message) {
        Matcher matcher = SINGLELINE_CODE_PATTERN.matcher(message);

        if (mSinglelineCode != null) {
            return mSinglelineCode;
        }

        if (matcher.find()) {
            mSinglelineCode = new ArrayList<>();

            do {
                mSinglelineCode.add(matcher.group().replace("`", ""));
            } while (matcher.find());

            return mSinglelineCode;
        }

        return Collections.emptyList();
    }

    private List<String> readBold(String message) {
        Matcher matcher = BOLD_PATTERN.matcher(message);

        if (mBold != null) {
            return mBold;
        }

        if (matcher.find()) {
            mBold = new ArrayList<>();

            do {
                mBold.add(matcher.group().replace("**", ""));
            } while (matcher.find());

            return mBold;
        }

        return Collections.emptyList();
    }

    private List<String> readStrikethrough(String message) {
        Matcher matcher = STRIKETHROUGH_PATTERN.matcher(message);

        if (mStrikethrough != null) {
            return mStrikethrough;
        }

        if (matcher.find()) {
            mStrikethrough = new ArrayList<>();

            do {
                mStrikethrough.add(matcher.group().replace("~~", ""));
            } while (matcher.find());

            return mStrikethrough;
        }

        return Collections.emptyList();
    }

    private List<String> readQuote(String message) {
        Matcher matcher = QUOTE_PATTERN.matcher(message);

        if (mQuote != null) {
            return mQuote;
        }

        if (matcher.find()) {
            mQuote = new ArrayList<>();

            do {
                String text = matcher.group().replaceFirst(">\\s?", "");

                if (text.length() > 0) {
                    if (text.substring(0, 1).equals("\n")) {
                        text = text.substring(1);
                    }

                    if (text.substring(text.length() - 1).equals("\n")) {
                        text = text.substring(0, text.length() - 1);
                    }
                }

                mQuote.add(text);
            } while (matcher.find());

            return mQuote;
        }

        return Collections.emptyList();
    }

    private List<String> readItalics(String message) {
        Matcher matcher = ITALICS_PATTERN.matcher(message);

        if (mItalics != null) {
            return mItalics;
        }

        if (matcher.find()) {
            mItalics = new ArrayList<>();

            do {
                String text = matcher.group(); // remove *
                mItalics.add(text.substring(1, text.length() - 1));
            } while (matcher.find());

            return mItalics;
        }

        return Collections.emptyList();
    }

    private List<String> readGitterLinks(String message) {
        Matcher matcher = GITTER_LINK_PATTERN.matcher(message);

        if (mGitterLinks != null) {
            return mGitterLinks;
        }

        if (matcher.find()) {
            mGitterLinks = new ArrayList<>();

            do {
                mGitterLinks.add(matcher.group().replaceFirst("\\[]\\((\\bhttp\\b|\\bhttps\\b)://\\)", ""));
            } while (matcher.find());

            return mGitterLinks;
        }

        return Collections.emptyList();
    }

    private List<String> readLinks(String message) {
        Matcher matcher = LINK_PATTERN.matcher(message);

        if (mLinks != null) {
            return mLinks;
        }

        if (matcher.find()) {
            mLinks = new ArrayList<>();

            do {
                mLinks.add(matcher.group());
            } while (matcher.find());

            return mLinks;
        }

        return Collections.emptyList();
    }

    private List<Object> readImageLinks(String message) {
        Matcher preview_matcher = PREVIEW_IMAGE_LINK_PATTERN.matcher(message);
        Matcher full_matcher = IMAGE_LINK_PATTERN.matcher(message);

        if (mImageLinks != null) {
            return mImageLinks;
        }

        if (preview_matcher.find()) {
            mImageLinks = new ArrayList<>();

            do {
                String previewUrl = preview_matcher.group().replaceFirst("\\[!\\[\\b.*\\b]\\(", "").replaceFirst("(\\)\\].*)+", "");
                String fullUrl = preview_matcher.group().replaceFirst("\\[!\\[\\b.*\\b]\\((.*?)\\)\\]\\(", "").replaceFirst("\\)", "");

                PreviewImageModel link = new PreviewImageModel(previewUrl, fullUrl);

                mImageLinks.add(link);
            } while (preview_matcher.find());

            return mImageLinks;
        } else if (full_matcher.find()) {
            mImageLinks = new ArrayList<>();

            do {
                mImageLinks.add(full_matcher.group().replaceFirst("!\\[.*?]\\(", "").replace(")", ""));
            } while (full_matcher.find());

            return mImageLinks;
        }

        return Collections.emptyList();
    }

    private List<String> readIssues(String message) {
        Matcher matcher = ISSUE_PATTERN.matcher(message);

        if (mIssues != null) {
            return mIssues;
        }

        if (matcher.find()) {
            mIssues = new ArrayList<>();

            do {
                mIssues.add(matcher.group().replace("#", ""));
            } while (matcher.find());

            return mIssues;
        }

        return Collections.emptyList();
    }

    private List<String> readMentions(String message) {
        Matcher matcher = MENTION_PATTERN.matcher(message);

        if (mMentions != null) {
            return mMentions;
        }

        if (matcher.find()) {
            mMentions = new ArrayList<>();

            do {
                mMentions.add(matcher.group());
            } while (matcher.find());

            return mMentions;
        }

        return Collections.emptyList();
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

    public List<String> getGitterLinks() {
        return readGitterLinks(mMessage);
    }

    public List<String> getLinks() {
        return readLinks(mMessage);
    }

    public List<Object> getImageLinks() {
        return readImageLinks(mMessage);
    }

    public List<String> getIssues() {
        return readIssues(mMessage);
    }

    public List<String> getMentions() {
        return readMentions(mMessage);
    }

    public List<String> getParsedString() {
        return convertWithPatterns(mMessage);
    }

    public boolean existMarkdown() {
        return getSinglelineCode().size() > 0 ||
                getMultilineCode().size() > 0 ||
                getBold().size() > 0 ||
                getStrikethrough().size() > 0 ||
                getQuote().size() > 0 ||
                getItalics().size() > 0 ||
                getImageLinks().size() > 0 ||
                getGitterLinks().size() > 0 ||
                getLinks().size() > 0;
    }

    public static class PreviewImageModel {
        private String mPreviewUrl;
        private String mFullUrl;

        public PreviewImageModel(String previewUrl, String fullUrl) {
            mPreviewUrl = previewUrl;
            mFullUrl = fullUrl;
        }

        public String getPreviewUrl() {
            return mPreviewUrl == null ? "" : mPreviewUrl;
        }

        public String getFullUrl() {
            return mFullUrl == null ? "" : mFullUrl;
        }
    }
}