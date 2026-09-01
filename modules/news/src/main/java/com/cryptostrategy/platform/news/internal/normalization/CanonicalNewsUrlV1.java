package com.cryptostrategy.platform.news.internal.normalization;

import com.cryptostrategy.platform.news.api.model.CanonicalNewsUrl;
import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

public final class CanonicalNewsUrlV1 {
    public CanonicalNewsUrl canonicalize(String raw) {
        try {
            URI input = new URI(raw.trim()).normalize();
            String scheme = input.getScheme().toLowerCase(Locale.ROOT);
            String host = IDN.toASCII(input.getHost()).toLowerCase(Locale.ROOT);
            int port = input.getPort();
            if ((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443)) port = -1;
            String path = input.getRawPath();
            if (path == null || path.isEmpty()) path = "/";
            path = normalizePercentEncoding(path);
            var query = new ArrayList<String>();
            if (input.getRawQuery() != null) {
                for (String part : input.getRawQuery().split("&", -1)) {
                    String[] pair=part.split("=",2);String normalizedKey=normalizePercentEncoding(pair[0]);
                    String key = normalizedKey.toLowerCase(Locale.ROOT);
                    if (!(key.startsWith("utm_") || key.equals("fbclid") || key.equals("gclid")))
                        query.add(normalizedKey+(pair.length==2?"="+normalizePercentEncoding(pair[1]):""));
                }
                query.sort(Comparator.naturalOrder());
            }
            URI result = new URI(scheme+"://"+host+(port<0?"":":"+port)+path+(query.isEmpty()?"":"?"+String.join("&",query)));
            return new CanonicalNewsUrl(result);
        } catch (NullPointerException | URISyntaxException | IllegalArgumentException error) {
            throw new IllegalArgumentException("Invalid News URL", error);
        }
    }
    private static String normalizePercentEncoding(String value){
        StringBuilder result=new StringBuilder();
        for(int i=0;i<value.length();i++){
            char current=value.charAt(i);
            if(current=='%'&&i+2<value.length()){
                int high=Character.digit(value.charAt(i+1),16),low=Character.digit(value.charAt(i+2),16);
                if(high<0||low<0)throw new IllegalArgumentException("Invalid percent encoding");
                char decoded=(char)((high<<4)+low);
                if(Character.isLetterOrDigit(decoded)||"-._~".indexOf(decoded)>=0)result.append(decoded);
                else result.append('%').append(Character.toUpperCase(value.charAt(i+1))).append(Character.toUpperCase(value.charAt(i+2)));
                i+=2;
            }else result.append(current);
        }
        return result.toString();
    }
}
