package com.cryptostrategy.platform.news.internal.normalization;

import com.cryptostrategy.platform.news.api.NewsNormalizationPolicy;
import com.cryptostrategy.platform.news.api.model.*;
import java.text.Normalizer;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;

public final class CanonicalNewsNormalizer implements NewsNormalizationPolicy {
    private static final Pattern CONTROLS = Pattern.compile("[\\p{Cc}&&[^\\n\\t]]");
    private static final Pattern HORIZONTAL = Pattern.compile("[\\p{Zs}\\t]+");
    private static final Pattern EXTRA_BLANK = Pattern.compile("\\n{3,}");
    private final CanonicalNewsUrlV1 urls = new CanonicalNewsUrlV1();
    private final NewsContentHashV1 hashes = new NewsContentHashV1();

    @Override
    public NormalizedNews normalize(String url, String titleHtml, String contentHtml, String language) {
        var lang = new LanguageCode(language);
        String title = normalizeText(titleHtml).replace('\n', ' ').trim();
        String content = normalizeText(contentHtml);
        if (title.isEmpty() || title.codePointCount(0, title.length()) > 1000) throw new IllegalArgumentException("Invalid title size");
        if (content.isEmpty() || content.codePointCount(0, content.length()) > 100000) throw new IllegalArgumentException("Invalid content size");
        return new NormalizedNews(urls.canonicalize(url), title, content, lang, hashes.hash(lang, title, content));
    }

    private static String normalizeText(String html) {
        var document = Jsoup.parse(html == null ? "" : html);
        document.select("script,style,template,svg,object,embed,iframe").remove();
        document.select("br").forEach(element -> element.after("\n"));
        document.select("p,div,section,article,header,footer,aside,main,blockquote,pre,li,dt,dd,h1,h2,h3,h4,h5,h6,tr")
                .forEach(element -> { element.prependText("\n"); element.appendText("\n"); });
        String text = document.body().wholeText().replace("\r\n", "\n").replace('\r', '\n').replace('\u00a0', ' ');
        text = CONTROLS.matcher(text).replaceAll("");
        text = Normalizer.normalize(text, Normalizer.Form.NFC);
        StringBuilder result = new StringBuilder();
        for (String line : text.split("\\n", -1)) result.append(HORIZONTAL.matcher(line).replaceAll(" ").trim()).append('\n');
        return EXTRA_BLANK.matcher(result.toString().strip()).replaceAll("\n\n");
    }
}
