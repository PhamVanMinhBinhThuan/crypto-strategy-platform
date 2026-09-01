package com.cryptostrategy.platform.news.api.port.in;

import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.news.api.model.AnalysisStatus;
import com.cryptostrategy.platform.news.api.model.NewsId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ListNewsUseCase {
    Page list(Query query);
    record Query(Set<AssetId> eitherAsset, Set<AnalysisStatus> statuses, Optional<String> cursor, int limit) {
        public Query {
            eitherAsset=Set.copyOf(eitherAsset==null?Set.of():eitherAsset);
            statuses=Set.copyOf(statuses==null?Set.of():statuses);
            cursor=cursor==null?Optional.empty():cursor.map(String::trim).filter(value->!value.isEmpty());
        }
    }
    record Item(NewsId newsId, String title, String source, String url, Instant publishedAt, AnalysisStatus analysisStatus,
                List<AssetId> relatedAssetIds, Optional<String> label, Optional<BigDecimal> confidence, Optional<BigDecimal> polarityScore) {
        public Item {
            relatedAssetIds=List.copyOf(relatedAssetIds);
            label=label==null?Optional.empty():label;
            confidence=confidence==null?Optional.empty():confidence;
            polarityScore=polarityScore==null?Optional.empty():polarityScore;
        }
    }
    record Page(List<Item> items, Optional<String> nextCursor) {
        public Page { items=List.copyOf(items);nextCursor=nextCursor==null?Optional.empty():nextCursor; }
    }
}
