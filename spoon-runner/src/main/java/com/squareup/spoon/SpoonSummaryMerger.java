package com.squareup.spoon;

import com.google.gson.Gson;

import java.io.File;

/** Takes several {@link SpoonSummary} instances and merges them into a single one */
public class SpoonSummaryMerger {

    public SpoonSummaryMerger(Gson gson){

    }

    public SpoonSummary merge(File[] spoonResults) {
        SpoonSummary[] spoonSummaries = new SpoonSummary[spoonResults.length];
        // TODO Convert to SpoonSummary instances

        return merge(spoonSummaries);
    }

    public SpoonSummary merge(SpoonSummary[] spoonSummaries) {
        return null;
    }

}
