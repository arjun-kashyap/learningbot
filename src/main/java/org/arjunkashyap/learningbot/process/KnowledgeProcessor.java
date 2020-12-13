package org.arjunkashyap.learningbot.process;

import org.arjunkashyap.learningbot.Entity.*;
import org.arjunkashyap.learningbot.common.WordnetUtils;
import org.arjunkashyap.learningbot.common.SearchIndex;
import org.arjunkashyap.learningbot.common.SentenceAnalyzer;
import org.arjunkashyap.learningbot.common.Utilities;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Component
public class KnowledgeProcessor {
    @Autowired
    private JdbcTemplate jtm;
    @Autowired
    private SentenceAnalyzer sentenceAnalyzer;
    @Autowired
    private SearchIndex searchIndex;
    @Autowired
    WordnetUtils wordnetUtils;

    public List<Match> search(String question) {
        List<Match> response = new ArrayList<>();
        try {
            int exactCount, synonymCount, hypernymCount, hyponymCount;
            int hitsPerPage, totalHitsThreshold;
            double synScore;

            hitsPerPage = totalHitsThreshold = 1000; //TODO: Move to properties/database
            TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, totalHitsThreshold);
            List<WordClassification> wordClassifications = sentenceAnalyzer.getPosElements(question);
            StringBuilder nouns = new StringBuilder();
            StringBuilder verbs = new StringBuilder();
            StringBuilder cardinalNumber = new StringBuilder();
            StringBuilder whClause = new StringBuilder();
            StringBuilder entities = new StringBuilder();
            for (WordClassification word : wordClassifications) {
                if (word.getPos().startsWith("NN") && !(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.contains(word.getLemma()))) {
                    nouns.append(" ").append(word.getLemma() + "~"); // ~ is appended for Fuzzy query
                } else if (word.getPos().startsWith("VB") && !(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.contains(word.getLemma()))) {
                    verbs.append(" ").append(word.getLemma() + "~");
                } else if (word.getPos().equals("CD")) {
                    cardinalNumber.append(" ").append(word.getLemma()); //exact search for cardinal nos.
                } else if (word.getPos().startsWith("W")) {
                    whClause.append(" ").append(word.getLemma() + "~");
                } else if (word.getPos().equals("ENTITY")) {
                    entities.append(" ").append(word.getLemma() + "~");
                }
                else if (word.getPos().equals("X-DERVB")) {
                    Synonym syn = wordnetUtils.getTopVerbForNounWithPreposition(word.getLemma());
                    if (syn != null) {
                        verbs.append(" ").append(syn.getWord() + "~");
                    }
                    else {
                        nouns.append(" ").append(word.getLemma() + "~");
                    }
                }
            }
            StringBuilder query = new StringBuilder();
            if (nouns.length() > 0) {
                query.append("+nouns:(" + nouns.toString().trim() + ")");
            }
            if (verbs.length() > 0) {
                query.append(" +verbs:(" + verbs.toString().trim() + ")");
            }
            if (cardinalNumber.length() > 0) {
                query.append(" +cd:(" + cardinalNumber.toString().trim() + ")");
            }
            if (whClause.length() > 0) {
                query.append(" +wc:(" + whClause.toString().trim() + ")");
            }
            if (entities.length() > 0) {
                query.append(" +namedEntity:(" + entities.toString().trim() + ")^2");
            }
            //System.out.println("Query is:" + query.toString().trim());
            if (query.length() > 0) {
                System.out.println("QUERY is: " + query.toString());
                Query q = new QueryParser("", searchIndex.getAnalyzer()).parse(query.toString());
                searchIndex.getIndexSearcher().search(q, collector);
                ScoreDoc[] hits = collector.topDocs().scoreDocs;

                //System.out.println("Found " + hits.length + " hits.");
                for (int i = 0; i < hits.length; ++i) {
                    Match match = new Match();
                    match.setSearcherScore(hits[i].score);
                    match.setQuestion(new Question());
                    int docId = hits[i].doc;
                    Document d = searchIndex.getIndexSearcher().doc(docId);

                    //System.out.println((i + 1) + ". " + d.get("questionId") + "\t" + d.get("question") + "\t" + hits[i].score+ "\t" +
                    //        d.get("exactCount")+ "\t" +d.get("synonymCount")+ "\t" +d.get("hypernymCount")+ "\t" +d.get("hyponymCount"));
                    match.getQuestion().setQuestionId((Integer.parseInt(d.get("questionId"))));
                    match.getQuestion().setIsQuestion(true);
                    exactCount = Integer.parseInt(d.get("exactCount"));
                    synonymCount = Integer.parseInt(d.get("synonymCount"));
                    hypernymCount = Integer.parseInt(d.get("hypernymCount"));
                    hyponymCount = Integer.parseInt(d.get("hyponymCount"));
                    //TODO: Move weightage to properties/database and come up with a good number
                    synScore = (exactCount * 1 + synonymCount * 0.9 + hypernymCount * 0.8 + hyponymCount * 0.7) / (exactCount + synonymCount + hypernymCount + hyponymCount);
                    match.setSynonymScore((float) synScore);
                    match.setDebug(d.get("debug"));
                    response.add(match);
                }
            }
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    //@PostConstruct
    public void indexAllQuestions() {
        try {
            String sql = "select question_id, question from question";
            List<Question> questionList = jtm.query(sql, new RowMapper<Question>() {
                @Override
                public Question mapRow(ResultSet rs, int rowNum) throws SQLException {
                    Question question = new Question();
                    question.setQuestionId(rs.getInt("question_id"));
                    question.setQuestionString(rs.getString("question"));
                    return question;
                }
            });
            IndexWriter indexWriter = searchIndex.getIndexWriterForWrite();
            for (Question question : questionList) {
                addQuestionToIndex(indexWriter, question);
            }
            searchIndex.closeIndexWriter(indexWriter);

            StringBuilder nouns;
            StringBuilder verbs;
            StringBuilder cardinalNumber;
            StringBuilder whClause;
            StringBuilder entities;
            StringBuilder query;
            Query q;
            TopScoreDocCollector collector;

            for (Question question : questionList) {
                List<WordClassification> wordClassifications = sentenceAnalyzer.getPosElements(question.getQuestionString());
                nouns = new StringBuilder();
                verbs = new StringBuilder();
                cardinalNumber = new StringBuilder();
                whClause = new StringBuilder();
                entities = new StringBuilder();
                for (WordClassification word : wordClassifications) {
                    if (word.getPos().startsWith("NN") && !(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.contains(word.getLemma()))) {
                        nouns.append(" ").append(word.getLemma() + "~"); // ~ is appended for Fuzzy query
                    } else if (word.getPos().startsWith("VB") && !(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.contains(word.getLemma()))) {
                        verbs.append(" ").append(word.getLemma() + "~");
                    } else if (word.getPos().equals("CD")) {
                        cardinalNumber.append(" ").append(word.getLemma());
                    } else if (word.getPos().startsWith("W")) {
                        whClause.append(" ").append(word.getLemma());
                    } else if (word.getPos().equals("ENTITY")) {//TODO: put named entity in quotes, other places too
                        entities.append(" ").append(word.getLemma());
                    } else if (word.getPos().equals("X-DERVB")) {
                        Synonym syn = wordnetUtils.getTopVerbForNounWithPreposition(word.getLemma());
                        if (syn != null) {
                            verbs.append(" ").append(syn.getWord() + "~");
                        }
                        else {
                            nouns.append(" ").append(word.getLemma() + "~"); //consider as noun
                        }
                    }
                }
                query = new StringBuilder();
                if (nouns.length() > 0) {
                    query.append("+nouns:(" + nouns.toString().trim() + ")");
                }
                if (verbs.length() > 0) {
                    query.append(" +verbs:(" + verbs.toString().trim() + ")");
                }
                if (cardinalNumber.length() > 0) {
                    query.append(" +cd:(" + cardinalNumber.toString().trim() + ")");
                }
                if (whClause.length() > 0) {
                    query.append(" +wc:(" + whClause.toString().trim() + ")");
                }
                if (entities.length() > 0) {
                    query.append(" +namedEntity:(" + entities.toString().trim() + ")");
                }


                //System.out.println("Query is:" + query.toString().trim());
                if (query.length() > 0) { // Question with no nouns or verbs or CD (all stop words?)
                    q = new QueryParser("", searchIndex.getAnalyzer()).parse(query.toString());
                    collector = TopScoreDocCollector.create(1, 1);
                    IndexSearcher searcher = searchIndex.getIndexSearcher();
                    searcher.search(q, collector);
                    ScoreDoc[] hits = collector.topDocs().scoreDocs;
                    if (hits.length > 0) {
                        question.setMaxPossibleSearcherScore(hits[0].score);
                        jtm.update("update question set max_possible_searcher_score = ? where question_id = ?",
                                question.getMaxPossibleSearcherScore(), question.getQuestionId());
                    }
                } else {
                    System.out.println("Strange question: " + question.getQuestionString());
                }
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private void addQuestionToIndex(IndexWriter indexWriter, Question question) throws IOException {
        Document doc;
        List<List<Synonym>> listOfRelatedWords = new ArrayList<>();
        int exactCount, synonymCount, hypernymCount, hyponymCount;

        List<WordClassification> wordClassifications = sentenceAnalyzer.getPosElements(question.getQuestionString());

        int count = 0;
        for (WordClassification word : wordClassifications) {
            if ((word.getPos().startsWith("NN") || word.getPos().startsWith("VB") || word.getPos().equals("ENTITY")) && !(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.contains(word.getLemma()))) { // || word.getPos().equals("X-DERVB")
                count++;
            }
        }

        int limit = Math.min(10, Math.max(1, 100000 / (int) Math.pow(10, count)));//TODO: drive via properties

        for (WordClassification word : wordClassifications) {
            BotPOS pos = null;
            if (word.getPos().startsWith("NN") && !(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.contains(word.getLemma()))) {
                pos = BotPOS.noun;
            } else if (word.getPos().startsWith("VB") && !(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.contains(word.getLemma()))) {
                pos = BotPOS.verb;
            } else if (word.getPos().equals("ENTITY")) {
                pos = BotPOS.namedEntity;
            } else if (word.getPos().equals("CD")) {
                pos = BotPOS.cardinalNumber;
            } else if (word.getPos().startsWith("W")) {
                pos = BotPOS.whClause;
            } else if (word.getPos().equals("X-DERVB")) {
                pos = BotPOS.derivation;
            }

            if (pos == BotPOS.derivation) {
                System.out.println("Hit preposition "+word.getLemma());
                Synonym verb = wordnetUtils.getTopVerbForNounWithPreposition(word.getLemma().toLowerCase());
                //List<Synonym> verbsForNounWithPreposition = wordnetUtils.getVerbForNounWithPreposition(word.getLemma().toLowerCase());
                //if (verbsForNounWithPreposition.size() > 0) {
                if (verb != null) {
                    //listOfRelatedWords.add(verbsForNounWithPreposition);
                    listOfRelatedWords.add(wordnetUtils.getTopSynonyms(verb.getWord(), BotPOS.verb, limit));
                }
                else {
                    listOfRelatedWords.add(wordnetUtils.getTopSynonyms(word.getLemma().toLowerCase(), BotPOS.noun, limit));
                }
            } else if (pos != null) {
                listOfRelatedWords.add(wordnetUtils.getTopSynonyms(word.getLemma().toLowerCase(), pos, limit));
            }
        }

        System.out.println(question.getQuestionString());
        int[] counts = new int[listOfRelatedWords.size()];
        int product = 1;
        for (int i = 0; i < listOfRelatedWords.size(); i++) {
            counts[i] = listOfRelatedWords.get(i).size();
            product *= counts[i];
        }
        int[][] allCombinations = Utilities.getAllCombinations(counts, product);

        for (int j = 0; j < product; j++) {
            doc = new Document();
            StringBuilder nouns = new StringBuilder();
            StringBuilder verbs = new StringBuilder();
            StringBuilder cardinalNumber = new StringBuilder();
            StringBuilder whClause = new StringBuilder();
            StringBuilder namedEntities = new StringBuilder();
            exactCount = synonymCount = hypernymCount = hyponymCount = 0;
            for (int i = 0; i < listOfRelatedWords.size(); i++) {
                Synonym synonym = listOfRelatedWords.get(i).get(allCombinations[i][j]);
                switch (synonym.getLinkType()) {
                    case also:
                        exactCount++;
                        break;
                    case syns:
                        synonymCount++;
                        break;
                    case hype:
                        hypernymCount++;
                        break;
                    case hypo:
                        hyponymCount++;
                        break;
                }

                if (synonym.getPos() == BotPOS.noun) {
                    nouns.append(" " + synonym.getWord());
                } else if (synonym.getPos() == BotPOS.verb) {
                    verbs.append(" " + synonym.getWord());
                } else if (synonym.getPos() == BotPOS.cardinalNumber) {
                    cardinalNumber.append(" " + synonym.getWord());
                } else if (synonym.getPos() == BotPOS.whClause) {
                    whClause.append(" " + synonym.getWord());
                } else if (synonym.getPos() == BotPOS.namedEntity) {
                    namedEntities.append(" " + synonym.getWord());
                }
            }
            //System.out.println("Adding to Index. Nouns: "+ nouns+" verbs: "+verbs);
            if (nouns.length() > 0) {
                doc.add(new TextField("nouns", nouns.toString(), Field.Store.NO));
            }
            if (verbs.length() > 0) {
                doc.add(new TextField("verbs", verbs.toString(), Field.Store.NO));
            }
            if (cardinalNumber.length() > 0) {
                doc.add(new TextField("cd", cardinalNumber.toString(), Field.Store.NO));
            }
            if (whClause.length() > 0) {
                doc.add(new TextField("wc", whClause.toString(), Field.Store.NO));
            }
            if (namedEntities.length() > 0) {
                doc.add(new TextField("namedEntity", namedEntities.toString(), Field.Store.NO));
            }
            doc.add(new StoredField("exactCount", exactCount));
            doc.add(new StoredField("synonymCount", synonymCount));
            doc.add(new StoredField("hypernymCount", hypernymCount));
            doc.add(new StoredField("hyponymCount", hyponymCount));
            doc.add(new StoredField("questionId", question.getQuestionId()));
            doc.add(new StoredField("debug", "nouns:" + nouns.toString() + " verbs: " + verbs.toString() + " wc: " + whClause.toString() + " cd: " + cardinalNumber.toString() + " namedEntity: " + namedEntities.toString())); //Storing the field for analyzing
            indexWriter.addDocument(doc);
        }
    }
}

//TODO: Compound nouns should be together. E.g. Abraham Lincoln, or Treasury Act