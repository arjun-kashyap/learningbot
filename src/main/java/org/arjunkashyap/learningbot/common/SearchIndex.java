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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class SearchIndex {
    @Autowired BotProperties props;
    private IndexSearcher searcher;
    private final StandardAnalyzer analyzer = new StandardAnalyzer();
    private Path indexPath;
    private IndexReader indexReader;
    private Directory index;

    public IndexWriter getIndexWriterForWrite(String mode) throws IOException {
        if (indexReader != null) {
            try {
                indexReader.close();
                indexReader = null;
                searcher = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //FileUtils.deleteDirectory(indexPath.toFile());
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        if (mode.equals("CREATE")) {
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        } else {
            config.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
        }
        IndexWriter indexWriter = new IndexWriter(index, config);
        return indexWriter;
    }

    public void closeIndexWriter(IndexWriter indexWriter) throws IOException {
        indexWriter.close();
    }

    public IndexSearcher getIndexSearcher() {
        if (indexReader == null) {
            try {
                indexReader = DirectoryReader.open(index);
                searcher = new IndexSearcher(indexReader);
                Similarity sim = new  BM25Similarity() {
                    // We are doing this because the synsets occur many times in the index (for each combination).
                    // This boosts the match of english words rather than synsets
                    @Override
                    public float idf(long freq, long count) {
                        return 1;
                    }
                };
                searcher.setSimilarity(sim);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return searcher;
    }

    public StandardAnalyzer getAnalyzer() {
        return analyzer;
    }

    @PostConstruct
    public void init() throws IOException {
        indexPath = Paths.get(props.INDEX_LOCATION);
        index = new MMapDirectory(indexPath);
    }
}
