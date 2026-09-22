# Assignment #2 — Report

**Name:** Soumyanil Ain
**Student ID:** 801488534
**Email:** sain@charlotte.edu

---

## Design

I went with **Design A**: one document per key, and all the comparing happens in the reducer.

**Mapper.** For each input line, the mapper takes the first token as the document ID and treats the rest as the text. It lowercases the text, splits on whitespace, strips anything that isn't a–z or 0–9, and drops empty tokens. The cleaned tokens go into a set, so each word is kept only once. It then emits:

- **key:** the document ID (e.g. `Doc01`)
- **value:** the document's distinct words joined by spaces (e.g. `a containing document is sample this words`)

This is the right thing to emit because Jaccard only needs each document's word _set_. Removing duplicates in the mapper makes the reducer's input smaller. It also means the reducer doesn't have to repeat the tokenization rules.

**Reducer.** There's only one reducer, so every document ends up there, one per `reduce()` call. `reduce()` can't compare anything yet because it hasn't seen the other documents. All it does is store the word set in a `TreeMap<String, Set<String>>` keyed by document ID. The Jaccard similarity is computed in `cleanup()`, which Hadoop calls once after the last `reduce()` call. By then the map holds every document. `cleanup()` loops over every pair (i < j), counts how many words the two sets share (the intersection), and computes the union as |A| + |B| − intersection. It writes `intersection / union` rounded to two decimals with `Locale.US`, so the decimal point is always a dot. Using a `TreeMap` keeps the IDs sorted, so every pair comes out as smaller ID first (`Doc01, Doc02`) without an extra sort step. Pairs with no shared words are skipped.

**Driver.** Compared to L4's `Controller`, I had to add:

- `conf.set("mapreduce.output.textoutputformat.separator", " ")` before `Job.getInstance(...)`. Otherwise `TextOutputFormat` puts a tab between the pair and `Similarity:`, and the output format would be wrong.
- `job.setNumReduceTasks(1)`. Design A only works if every document meets in the same reducer. With more than one reducer, each would see only some of the documents and some pairs would be missing.
- No combiner. L4 reused the reducer as a combiner, but here that would be wrong. The reducer's output is similarity lines, not document word sets, so it can't be fed back into another reduce step.
- `setMapOutputKeyClass` / `setMapOutputValueClass` and the output classes, all `Text`.

---

## How I ran it

```bash
mvn clean package
docker compose up -d
docker cp target/DocumentSimilarity-0.0.1-SNAPSHOT.jar resourcemanager:/tmp/
docker cp shared-folder/input/data/small_dataset.txt resourcemanager:/tmp/
docker cp shared-folder/input/data/dataset.txt resourcemanager:/tmp/
docker exec -it resourcemanager bash
cd /tmp
hadoop fs -mkdir -p /input/data
hadoop fs -put ./small_dataset.txt /input/data
hadoop fs -put ./dataset.txt /input/data
hadoop fs -ls /input/data
hadoop jar /tmp/DocumentSimilarity-0.0.1-SNAPSHOT.jar com.example.controller.DocumentSimilarityDriver /input/data/small_dataset.txt /output/small_dataset
hadoop fs -cat /output/small_dataset/*
hadoop jar /tmp/DocumentSimilarity-0.0.1-SNAPSHOT.jar com.example.controller.DocumentSimilarityDriver /input/data/dataset.txt /output/dataset
hadoop fs -cat /output/dataset/*
hadoop fs -cat /output/dataset/* | wc -l
hdfs dfs -get /output /tmp/
exit
New-Item -ItemType Directory -Force shared-folder/output
docker cp resourcemanager:/tmp/output/. shared-folder/output/
docker compose down
```

I made two changes to the README steps:

- I ran each `hadoop jar` command on one line instead of splitting it with `\`. The split version broke when I pasted it (see Problems and fixes).
- I'm on Windows with PowerShell, so I used `New-Item -ItemType Directory -Force` to create the local output folder instead of `mkdir -p`.

---

## Output

### `small_dataset.txt` (3 lines)

```
Document1, Document2 Similarity: 0.18
Document1, Document3 Similarity: 0.20
Document2, Document3 Similarity: 0.10
```

### `dataset.txt` (66 lines)

```
Doc01, Doc02 Similarity: 0.16
Doc01, Doc03 Similarity: 0.13
Doc01, Doc04 Similarity: 0.07
Doc01, Doc05 Similarity: 0.10
Doc01, Doc06 Similarity: 0.09
Doc01, Doc07 Similarity: 0.11
Doc01, Doc08 Similarity: 0.10
Doc01, Doc09 Similarity: 0.11
Doc01, Doc10 Similarity: 0.09
Doc01, Doc11 Similarity: 0.07
Doc01, Doc12 Similarity: 0.19
Doc02, Doc03 Similarity: 0.20
Doc02, Doc04 Similarity: 0.13
Doc02, Doc05 Similarity: 0.10
Doc02, Doc06 Similarity: 0.09
Doc02, Doc07 Similarity: 0.06
Doc02, Doc08 Similarity: 0.09
Doc02, Doc09 Similarity: 0.05
Doc02, Doc10 Similarity: 0.10
Doc02, Doc11 Similarity: 0.06
Doc02, Doc12 Similarity: 0.14
Doc03, Doc04 Similarity: 0.17
Doc03, Doc05 Similarity: 0.11
Doc03, Doc06 Similarity: 0.08
Doc03, Doc07 Similarity: 0.16
Doc03, Doc08 Similarity: 0.11
Doc03, Doc09 Similarity: 0.07
Doc03, Doc10 Similarity: 0.10
Doc03, Doc11 Similarity: 0.12
Doc03, Doc12 Similarity: 0.11
Doc04, Doc05 Similarity: 0.09
Doc04, Doc06 Similarity: 0.11
Doc04, Doc07 Similarity: 0.18
Doc04, Doc08 Similarity: 0.09
Doc04, Doc09 Similarity: 0.08
Doc04, Doc10 Similarity: 0.10
Doc04, Doc11 Similarity: 0.09
Doc04, Doc12 Similarity: 0.09
Doc05, Doc06 Similarity: 0.20
Doc05, Doc07 Similarity: 0.14
Doc05, Doc08 Similarity: 0.15
Doc05, Doc09 Similarity: 0.07
Doc05, Doc10 Similarity: 0.13
Doc05, Doc11 Similarity: 0.14
Doc05, Doc12 Similarity: 0.11
Doc06, Doc07 Similarity: 0.17
Doc06, Doc08 Similarity: 0.15
Doc06, Doc09 Similarity: 0.08
Doc06, Doc10 Similarity: 0.10
Doc06, Doc11 Similarity: 0.12
Doc06, Doc12 Similarity: 0.13
Doc07, Doc08 Similarity: 0.15
Doc07, Doc09 Similarity: 0.07
Doc07, Doc10 Similarity: 0.08
Doc07, Doc11 Similarity: 0.12
Doc07, Doc12 Similarity: 0.11
Doc08, Doc09 Similarity: 0.19
Doc08, Doc10 Similarity: 0.13
Doc08, Doc11 Similarity: 0.22
Doc08, Doc12 Similarity: 0.12
Doc09, Doc10 Similarity: 0.13
Doc09, Doc11 Similarity: 0.12
Doc09, Doc12 Similarity: 0.13
Doc10, Doc11 Similarity: 0.12
Doc10, Doc12 Similarity: 0.12
Doc11, Doc12 Similarity: 0.11
```

---

## Analysis

**Most similar pairs:**

- **Doc08 / Doc11 (0.22):** Spark, and machine learning with Spark MLlib.
- **Doc02 / Doc03 (0.20):** virtualization, and containers.
- **Doc05 / Doc06 (0.20):** Hadoop/HDFS, and MapReduce.
- **Doc01 / Doc12 (0.19):** cloud computing in general, and AWS.
- **Doc08 / Doc09 (0.19):** Spark, and Spark DataFrames/SQL.

**Least similar pairs:**

- **Doc02 / Doc09 (0.05):** hypervisors, and Spark SQL.
- **Doc02 / Doc07 (0.06):** virtualization, and YARN.
- **Doc02 / Doc11 (0.06):** virtualization, and MLlib.

The top pairs mostly make sense. Each one is two documents on closely related topics that share real content words. Doc08 and Doc11 share Spark, distributed, cluster and algorithms. Doc02 and Doc03 both talk about virtual machines, operating systems and sharing a host. Doc05 and Doc06 both describe Hadoop and processing big data. Doc01 and Doc12 share cloud, computing, storage, virtual machines and on demand. The bottom pairs are also sensible: Doc02 is about hardware-level virtualization, which has little in common with a SQL query engine or an ML library.

The ranking still isn't fully trustworthy, though. Some pairs that should be close aren't near the top. For example, Doc03/Doc04 (containers and Docker Compose) is 0.17, and Doc06/Doc07 (MapReduce and YARN) is also 0.17. That's barely above several pairs that have little to do with each other.

**Why the values are low and close together.** Every document shares words like _the, a, and, of, on, is, with, to_. These words add about the same amount to the intersection of every pair, so they don't separate related pairs from unrelated ones. Each document also has many distinct words, which makes every union large. The result is that the few meaningful shared words get diluted, and all 66 values end up between 0.05 and 0.22.

**One change to make the numbers more meaningful:** remove stop words during tokenization. Without them, the intersection would mostly contain topic words like _spark, hadoop, virtual, container_, and the union would shrink. Related pairs would then score clearly higher than unrelated ones, and the spread between them would widen.

---

## Scalability

Design A sends every document to a single reducer, and that reducer holds all of them in memory until `cleanup()`. With a million documents, this breaks in three concrete ways:

- **Memory.** Every word set sits in the reducer's Java heap at the same time, as millions of `String` objects inside `HashSet`s, each with Java's per-object overhead. A reducer container gets a few GB of heap, and a large collection easily exceeds that. The task then fails with `OutOfMemoryError` or gets killed by YARN for exceeding its container memory.
- **Compute.** A million documents make about 1,000,000 × 999,999 / 2 ≈ 5 × 10¹¹ pairs. `cleanup()` compares all of them one after another in a single task on a single machine. Even at millions of comparisons per second, that takes days.
- **No parallelism.** The rest of the cluster sits idle while that one reducer works. The whole shuffle also funnels into one node. If that task fails, all of the work starts over.

**How Design B avoids this.** Design B builds an inverted index. The mapper emits `(word, docID)` for each distinct word in a document, so the shuffle groups documents by word instead of sending them all to one place. Each word's reducer gets only the list of documents that contain that word. It emits `((docA, docB), 1)` for every pair in that list. Summing those 1s per pair gives |A ∩ B|. For the union, the mapper can attach each document's size to its ID (e.g. `Doc01:34`) and carry it through, so each pair knows |A| and |B|. A second job, or a second reduce stage, then sums the counts per pair and computes the Jaccard value.

This spreads the work across many reducers, one per word, and no single task has to hold the whole collection. Pairs that share no words never get generated at all, which avoids most of the 5 × 10¹¹ comparisons. Design B does have its own weak spot: a very common word like "the" puts almost every document into one list and creates a huge number of pairs for one reducer. That's another reason to remove stop words, or to skip words that appear in almost every document.

---

## Problems and fixes

The build and the job code worked on the first try. The one problem was running the job. I pasted the `hadoop jar` command from the README, and the line break got lost, so the command reached the shell like this:

```
hadoop jar /tmp/DocumentSimilarity-0.0.1-SNAPSHOT.jar \  com.example.controller.DocumentSimilarityDriver /input/data/small_dataset.txt /output/small_dataset
```

It failed with:

```
Exception in thread "main" java.lang.ClassNotFoundException:
        at java.net.URLClassLoader.findClass(URLClassLoader.java:382)
        at java.lang.ClassLoader.loadClass(ClassLoader.java:424)
        at java.lang.ClassLoader.loadClass(ClassLoader.java:357)
        at java.lang.Class.forName0(Native Method)
        at java.lang.Class.forName(Class.java:348)
        at org.apache.hadoop.util.RunJar.run(RunJar.java:321)
        at org.apache.hadoop.util.RunJar.main(RunJar.java:241)
```

The class name in the error is blank, and that was the clue. A `\` only continues a command when it's the last character on a line. In the middle of a line, `\ ` escapes the space that follows it. So Hadoop got a single space as the main class name instead of `com.example.controller.DocumentSimilarityDriver`. I fixed it by running the whole command on one line. The job never started, so there was no output directory to delete before rerunning.

---

## Use of generative AI

I used Claude (Anthropic) to help write the Mapper, Reducer and Driver code and to check the job's output against a reference computed separately. I also used it to debug the `ClassNotFoundException` above and to draft this report. I ran every step on my own machine, checked that the outputs match the expected results, and reviewed and edited the report before submitting.
