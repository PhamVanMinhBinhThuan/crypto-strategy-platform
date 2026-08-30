package com.cryptostrategy.platform.combination.api;
import com.cryptostrategy.platform.combination.internal.MajorityVotePolicy;
import java.util.List;
public final class CombinationPolicies { private CombinationPolicies(){} public static List<CombinationPolicy> supported(){return List.of(new MajorityVotePolicy());} }
