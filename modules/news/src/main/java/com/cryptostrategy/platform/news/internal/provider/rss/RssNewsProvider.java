package com.cryptostrategy.platform.news.internal.provider.rss;

import com.cryptostrategy.platform.news.api.model.*;
import com.cryptostrategy.platform.news.api.port.out.NewsProvider;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.xml.stream.*;

public final class RssNewsProvider implements NewsProvider {
    private static final int MAX_BODY=1_048_576;
    private final NewsSource source; private final URI feed; private final String language; private final List<String> assets;
    private final HttpClient http; private final Duration timeout;
    public RssNewsProvider(NewsSource source,URI feed,String language,List<String> assets,HttpClient http,Duration timeout){this.source=source;this.feed=feed;this.language=language;this.assets=List.copyOf(assets);this.http=http;this.timeout=timeout;}
    @Override public NewsSource source(){return source;}
    @Override public List<ProviderNewsItem> fetchSince(Instant since){
        try{
            var request=HttpRequest.newBuilder(feed).timeout(timeout).header("Accept","application/rss+xml, application/atom+xml, application/xml, text/xml").GET().build();
            var response=http.send(request,HttpResponse.BodyHandlers.ofByteArray());
            if(response.statusCode()<200||response.statusCode()>=300)throw new IllegalStateException("Feed HTTP "+response.statusCode());
            if(response.body().length>MAX_BODY)throw new IllegalStateException("Feed is too large");
            return parse(response.body()).stream().filter(item->!item.publishedAt().isBefore(since)).toList();
        }catch(IOException error){throw new IllegalStateException("Feed transport failed",error);}catch(InterruptedException error){Thread.currentThread().interrupt();throw new IllegalStateException("Feed interrupted",error);}
    }
    private List<ProviderNewsItem> parse(byte[] bytes){
        try{
            var factory=XMLInputFactory.newFactory();factory.setProperty(XMLInputFactory.SUPPORT_DTD,false);factory.setProperty("javax.xml.stream.isSupportingExternalEntities",false);
            var reader=factory.createXMLStreamReader(new ByteArrayInputStream(bytes));var results=new ArrayList<ProviderNewsItem>();
            Map<String,String> current=null;String field=null;StringBuilder text=null;
            while(reader.hasNext()){
                int event=reader.next();
                if(event==XMLStreamConstants.START_ELEMENT){String name=reader.getLocalName().toLowerCase(Locale.ROOT);
                    if(name.equals("item")||name.equals("entry"))current=new HashMap<>();
                    else if(current!=null){field=name;text=new StringBuilder();if(name.equals("link")){String href=reader.getAttributeValue(null,"href");if(href!=null)current.put("link",href);}}
                }else if((event==XMLStreamConstants.CHARACTERS||event==XMLStreamConstants.CDATA)&&text!=null)text.append(reader.getText());
                else if(event==XMLStreamConstants.END_ELEMENT&&current!=null){String name=reader.getLocalName().toLowerCase(Locale.ROOT);
                    if((name.equals("item")||name.equals("entry"))){results.add(toItem(current));current=null;field=null;text=null;}
                    else if(field!=null&&field.equals(name)){current.putIfAbsent(name,text.toString());field=null;text=null;}
                }
            }return List.copyOf(results);
        }catch(XMLStreamException error){throw new IllegalArgumentException("Malformed RSS/Atom feed",error);}
    }
    private ProviderNewsItem toItem(Map<String,String> values){
        String content=first(values,"content","encoded","description","summary");String link=required(values,"link");String title=required(values,"title");
        Instant published=parseTime(first(values,"published","updated","pubdate"));
        String sourceItemId=first(values,"guid","id");
        return new ProviderNewsItem(link,title,content,language,published,
                sourceItemId.isBlank()?Optional.empty():Optional.of(sourceItemId),assets);
    }
    private static String required(Map<String,String> values,String key){String value=values.get(key);if(value==null||value.isBlank())throw new IllegalArgumentException("Feed item missing "+key);return value.trim();}
    private static String first(Map<String,String> values,String...keys){for(String key:keys){String value=values.get(key);if(value!=null&&!value.isBlank())return value.trim();}return "";}
    private static Instant parseTime(String value){try{return Instant.parse(value);}catch(Exception ignored){return ZonedDateTime.parse(value,DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();}}
}
