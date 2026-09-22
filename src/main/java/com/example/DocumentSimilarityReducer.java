package com.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * Collects every document's word set in reduce(), then compares all pairs
 * in cleanup() once the single reducer has seen every document.
 */
public class DocumentSimilarityReducer extends Reducer<Text, Text, Text, Text> {

    // TreeMap keeps IDs sorted, so pairs come out as (smaller, larger).
    private final Map<String, Set<String>> documents = new TreeMap<>();

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {
        Set<String> wordSet = documents.computeIfAbsent(key.toString(), k -> new HashSet<>());
        for (Text value : values) {
            String text = value.toString().trim();
            if (text.isEmpty()) {
                continue;
            }
            for (String word : text.split(" ")) {
                wordSet.add(word);
            }
        }
    }

    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
        List<String> ids = new ArrayList<>(documents.keySet());
        Text pair = new Text();
        Text result = new Text();

        for (int i = 0; i < ids.size(); i++) {
            Set<String> a = documents.get(ids.get(i));
            for (int j = i + 1; j < ids.size(); j++) {
                Set<String> b = documents.get(ids.get(j));

                int intersection = 0;
                for (String word : a) {
                    if (b.contains(word)) {
                        intersection++;
                    }
                }
                if (intersection == 0) {
                    continue;
                }

                int union = a.size() + b.size() - intersection;
                double similarity = (double) intersection / union;

                pair.set(ids.get(i) + ", " + ids.get(j));
                result.set("Similarity: " + String.format(Locale.US, "%.2f", similarity));
                context.write(pair, result);
            }
        }
    }
}