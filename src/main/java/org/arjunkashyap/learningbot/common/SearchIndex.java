package org.arjunkashyap.learningbot.common;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class SearchIndex {
    private IndexSearcher mainSearcher;
    private final StandardAnalyzer mainAnalyzer = new StandardAnalyzer();
    private Path mainIndexPath = Paths.get("/Users/Suresh/demo_lucene/");//TODO: properties;
    private IndexReader mainIndexReader;
    private Directory mainIndex = new MMapDirectory(mainIndexPath);

    public SearchIndex() throws IOException {
    }

    public IndexWriter getMainIndexWriterForWrite() throws IOException {
        if (mainIndexReader != null) {
            try {
                mainIndexReader.close();
                mainIndexReader = null;
                mainSearcher = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FileUtils.deleteDirectory(mainIndexPath.toFile());
        IndexWriterConfig config = new IndexWriterConfig(mainAnalyzer);
        IndexWriter indexWriter = new IndexWriter(mainIndex, config);
        return indexWriter;
    }

    public void closeMainIndexWriter(IndexWriter indexWriter) throws IOException {
        indexWriter.close();
    }

    public IndexSearcher getIndexSearcher() {
        if (mainIndexReader == null) {
            try {
                mainIndexReader = DirectoryReader.open(mainIndex);
                mainSearcher = new IndexSearcher(mainIndexReader);
                Similarity sim = new  BM25Similarity() {
                    // We are doing this because the synsets occur many times in the index (for each combination).
                    // This boosts the match of english words rather than synsets
                    @Override
                    public float idf(long freq, long count) {
                        return 1;
                    }
                };
                mainSearcher.setSimilarity(sim);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return mainSearcher;
    }

    public StandardAnalyzer getMainAnalyzer() {
        return mainAnalyzer;
    }
}
