package com.cryptostrategy.platform.persistence.internal.news;

import com.cryptostrategy.platform.news.api.model.*;
import com.cryptostrategy.platform.news.api.port.in.ListNewsUseCase;
import com.cryptostrategy.platform.news.api.port.out.NewsQueryPort;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcNewsQueryAdapter implements NewsQueryPort {
    private final JdbcTemplate jdbc;
    public JdbcNewsQueryAdapter(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @Override public ListNewsUseCase.Page list(ListNewsUseCase.Query query) {
        var sql=new StringBuilder("""
            select n.news_item_id,n.title,n.source,n.url,n.published_at,n.analysis_status,
                   array(select a.asset_id from news.news_item_asset a where a.news_item_id=n.news_item_id order by a.asset_id),
                   s.label,s.confidence,s.polarity_score
              from news.news_item n
              left join lateral (select label,confidence,polarity_score from news.sentiment_result r where r.news_item_id=n.news_item_id order by analyzed_at desc limit 1) s on n.analysis_status='ANALYZED'
             where 1=1
            """);
        var args=new ArrayList<Object>();
        if(!query.eitherAsset().isEmpty()){
            sql.append(" and exists(select 1 from news.news_item_asset a where a.news_item_id=n.news_item_id and a.asset_id in (");
            sql.append(String.join(",",Collections.nCopies(query.eitherAsset().size(),"?"))).append("))");
            query.eitherAsset().stream().map(a->a.value()).sorted().forEach(args::add);
        }
        if(!query.statuses().isEmpty()){
            sql.append(" and n.analysis_status in (").append(String.join(",",Collections.nCopies(query.statuses().size(),"?"))).append(")");
            query.statuses().stream().map(Enum::name).sorted().forEach(args::add);
        }
        query.cursor().ifPresent(cursor->{
            var decoded=NewsCursor.decode(cursor);
            sql.append(" and (n.published_at,n.news_item_id)<(?,?)");args.add(Timestamp.from(decoded.publishedAt()));args.add(decoded.newsId().value());
        });
        sql.append(" order by n.published_at desc,n.news_item_id desc limit ?");args.add(query.limit()+1);
        var rows=jdbc.query(sql.toString(),(rs,row)->{
            Object[] raw=(Object[])rs.getArray(7).getArray();var assets=Arrays.stream(raw).map(Object::toString).map(com.cryptostrategy.platform.domain.api.market.AssetId::new).toList();
            return new ListNewsUseCase.Item(new NewsId(rs.getString(1)),rs.getString(2),rs.getString(3),rs.getString(4),rs.getTimestamp(5).toInstant(),AnalysisStatus.valueOf(rs.getString(6)),assets,Optional.ofNullable(rs.getString(8)),Optional.ofNullable(rs.getBigDecimal(9)),Optional.ofNullable(rs.getBigDecimal(10)));},args.toArray());
        Optional<String> next=Optional.empty();
        if(rows.size()>query.limit()){
            var boundary=rows.get(query.limit()-1);
            next=Optional.of(new NewsCursor(boundary.publishedAt(),boundary.newsId()).encode()); rows=new ArrayList<>(rows.subList(0,query.limit()));
        }
        return new ListNewsUseCase.Page(List.copyOf(rows),next);
    }
}
