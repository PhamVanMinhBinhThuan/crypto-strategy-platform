package com.cryptostrategy.platform.leaderboard.api.model;
import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;
public record LeaderboardRevisionId(String value) implements UlidIdentifier {public LeaderboardRevisionId{value=Ulids.requireValid(value);}public static LeaderboardRevisionId generate(){return new LeaderboardRevisionId(Ulids.generate());}}
