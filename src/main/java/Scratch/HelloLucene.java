package Scratch;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import java.io.IOException;
import java.nio.file.Files;

public class HelloLucene {
    public static void main(String[] args) throws IOException, ParseException {
        // 0. Specify the analyzer for tokenizing text.
        //    The same analyzer should be used for indexing and searching
        StandardAnalyzer analyzer = new StandardAnalyzer();

        // 1. create the index
        Directory index = new MMapDirectory(Files.createTempDirectory("lucene").toAbsolutePath());

        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        IndexWriter w = new IndexWriter(index, config);
        addDoc(w, "Lucene in Action", "Suresh", 193398817);
        addDoc(w, "Lucene for Dummies", "Lucene for Dummies",55320055);
        addDoc(w, "Managing Gigabytes", "Managing Gigabytes", 55063554);
        addDoc(w, "The Art of Computer Science", "The Art of Computer Science", 9900333);
        addDoc(w, "Manage", "Manage", 99003333);
        w.close();

        // 2. query
        String querystr = args.length > 0 ? args[0] : "questionPos:lucene in action, questionPos2:suresh";

        // the "title" arg specifies the default field to use
        // when no field is explicitly specified in the query.
        Query q = new QueryParser("questionPos", analyzer).parse(querystr);

        // 3. search
        int hitsPerPage = 10;
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, 10);
        searcher.search(q, collector);
        ScoreDoc[] hits = collector.topDocs().scoreDocs;

        // 4. display results
        System.out.println("Found " + hits.length + " hits.");
        for(int i=0;i<hits.length;++i) {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);
            System.out.println((i + 1) + ". " + d.get("questionId") + "\t" + d.get("questionPos")+" "+hits[i].score);
        }
        reader.close();
    }

    private static void addDoc(IndexWriter w, String questionPos, String questionPos2, int questionId) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("questionPos", questionPos, Field.Store.YES));
        doc.add(new TextField("questionPos2", questionPos2, Field.Store.YES));
        doc.add(new StoredField("questionId",questionId));
        w.addDocument(doc);
    }
}