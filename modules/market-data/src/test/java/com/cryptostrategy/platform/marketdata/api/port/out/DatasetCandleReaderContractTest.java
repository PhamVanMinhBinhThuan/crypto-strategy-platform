package com.cryptostrategy.platform.marketdata.api.port.out;

import static com.cryptostrategy.platform.marketdata.support.MarketFixtures.candle;
import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.domain.api.market.CandleId;
import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.marketdata.api.model.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DatasetCandleReaderContractTest {
    private static final DatasetVersionId ID=new DatasetVersionId("01ARZ3NDEKTSV4RRFFQ69G5FAY");
    @Test void traversesInclusiveZeroBasedPagesWithoutDuplicatesOrOmissions(){DatasetCandleReader reader=reader(7);List<Integer> sequences=new ArrayList<>();int cursor=0;boolean more;do{CandleBatch batch=reader.readCandles(ID,cursor,2);batch.members().forEach(member->sequences.add(member.sequenceNo()));cursor=batch.nextSequence();more=batch.hasMore();}while(more);assertEquals(List.of(0,1,2,3,4,5,6),sequences);assertTrue(reader.readCandles(ID,7,1).members().isEmpty());assertThrows(IllegalArgumentException.class,()->reader.readCandles(ID,8,1));}
    @Test void acceptsBoundaryBatchSizesAndRejectsInvalidSizes(){DatasetCandleReader reader=reader(1);assertEquals(1,reader.readCandles(ID,0,1).members().size());assertEquals(1,reader.readCandles(ID,0,5000).members().size());assertThrows(IllegalArgumentException.class,()->reader.readCandles(ID,0,0));assertThrows(IllegalArgumentException.class,()->reader.readCandles(ID,0,5001));}
    private static DatasetCandleReader reader(int count){List<DatasetMembership> members=new ArrayList<>();for(int i=0;i<count;i++)members.add(new DatasetMembership(ID,i,new PersistedCandle(new CandleId("01ARZ3NDEKTSV4RRFFQ69G5FAZ"),candle(i,"2"))));return(id,from,size)->{if(from<0||from>members.size()||size<1||size>CandleBatch.MAX_BATCH_SIZE)throw new IllegalArgumentException();int end=Math.min(from+size,members.size());return new CandleBatch(id,from,members.subList(from,end),end,end<members.size());};}
}
