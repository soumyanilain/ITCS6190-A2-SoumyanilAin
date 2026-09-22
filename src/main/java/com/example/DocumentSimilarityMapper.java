package com.example;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

/**
 * Emits (documentID, "word1 word2 ...") where the value is the document's
 * distinct words after tokenization.
 */
public class DocumentSimilarityMapper extends Mapper<LongWritable, Text, Text, Text> {

    private final Text docId = new Text();
    private final Text words = new Text();

    @Override
    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {
        String line = value.toString().trim();
        if (line.isEmpty()) {
            return;
        }

        String[] parts = line.split("\\s+", 2);
        Set<String> wordSet = new TreeSet<>();

        if (parts.length > 1) {
            for (String token : parts[1].toLowerCase().split("\\s+")) {
                String cleaned = token.replaceAll("[^a-z0-9]", "");
                if (!cleaned.isEmpty()) {
                    wordSet.add(cleaned);
                }
            }
        }

        docId.set(parts[0]);
        words.set(String.join(" ", wordSet));
        context.write(docId, words);
    }
}