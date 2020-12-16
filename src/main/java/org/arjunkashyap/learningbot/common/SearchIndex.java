package org.arjunkashyap.learningbot.common;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class SearchIndex {
    private IndexSearcher searcher;
    private final StandardAnalyzer analyzer = new StandardAnalyzer();
    private Path indexPath = Paths.get("/Users/Suresh/demo_lucene/");//TODO: properties;
    private IndexReader reader;
    private Directory index = new MMapDirectory(indexPath);

    public SearchIndex() throws IOException {
    }

    public IndexWriter getIndexWriterForWrite() throws IOException {
        if (reader != null) {
            try {
                reader.close();
                reader = null;
                searcher = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FileUtils.deleteDirectory(indexPath.toFile());
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter indexWriter = new IndexWriter(index, config);
        return indexWriter;
    }

    public void closeIndexWriter(IndexWriter indexWriter) throws IOException {
        indexWriter.close();
    }

    public IndexSearcher getIndexSearcher() {
        if (reader == null) {
            try {
                reader = DirectoryReader.open(index);
                searcher = new IndexSearcher(reader);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return searcher;
    }

    public StandardAnalyzer getAnalyzer() {
        return analyzer;
    }
}
