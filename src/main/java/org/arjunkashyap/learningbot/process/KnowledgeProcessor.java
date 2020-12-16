package org.arjunkashyap.learningbot.process;

import edu.cmu.lti.jawjaw.pobj.Link;
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
        int exactCount, synonymCount, hypernymCount, hyponymCount;
        int hitsPerPage, totalHitsThreshold;
        double synScore;

        hitsPerPage = totalHitsThreshold = 1000; //TODO: Move to properties/database
        TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, totalHitsThreshold);
        List<Word> words = sentenceAnalyzer.getPosElements(question);

        try {

            ScoreDoc[] hits = queryIndex(collector, words);
            if (hits != null) {
                //System.out.println("Found " + hits.length + " hits.");
                for (int i = 0; i < hits.length; ++i) {
                    Match match = new Match();
                    match.setSearcherScore(hits[i].score);
                    match.setQuestion(new Question());
                    int docId = hits[i].doc;
                    Document d = searchIndex.getIndexSearcher().doc(docId);

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
        } catch (IOException | ParseException ioException) {
            ioException.printStackTrace();
        }
        return response;
    }

    //@PostConstruct
    public void indexAllQuestions() {
        TopScoreDocCollector collector;
        try {
            //get all the Q's from the database
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

            //Open the index for write
            IndexWriter indexWriter = searchIndex.getIndexWriterForWrite();
            for (Question question : questionList) {
                addQuestionToIndex(indexWriter, question);
            }
            searchIndex.closeIndexWriter(indexWriter);

            //For all the questions, set the max score
            for (Question question : questionList) {
                List<Word> words = sentenceAnalyzer.getPosElements(question.getQuestionString());
                collector = TopScoreDocCollector.create(1, 1); //need to create this for every query
                ScoreDoc[] hits = queryIndex(collector, words);
                if (hits.length > 0) {
                    question.setMaxPossibleSearcherScore(hits[0].score);
                    jtm.update("update question set max_possible_searcher_score = ? where question_id = ?",
                            question.getMaxPossibleSearcherScore(), question.getQuestionId());
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

        List<Word> words = sentenceAnalyzer.getPosElements(question.getQuestionString());

        int count = 0;
        for (Word word : words) {
            if (!(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.contains(word.getLemma())) &&
                    (word.getPos() == BotPOS.NOUN ||
                     word.getPos() == BotPOS.VERB ||
                     word.getPos() == BotPOS.ENTITY ||
                     word.getPos() == BotPOS.DERIVE_VERB)) { //Not getting synonyms for ADJ and ADV
                count++;
            }
        }
        int maxCombinations = 100000;//TODO: drive via properties, next line too
        int limit = Math.min(10, Math.max(1, maxCombinations / (int) Math.pow(10, count)));

        for (Word word : words) {
            BotPOS pos = word.getPos();
            //System.out.println("DEBUGGING" + word.getLemma() + " "+pos);
            if (EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.contains(word.getLemma())) {
                continue;
            }
            switch (pos) {
                case CARDINAL_NUMBER:
                case WH_QUESTION:
                    List<Synonym> synonyms = new ArrayList<>();
                    Synonym mainWord = new Synonym();
                    mainWord.setWord(word.getLemma());
                    mainWord.setLemma(word.getLemma());
                    mainWord.setScore(100);
                    mainWord.setLinkType(Link.also);
                    mainWord.setPos(pos);
                    synonyms.add(mainWord);
                    listOfRelatedWords.add(synonyms);
                    break;
                case DERIVE_VERB:
                    //System.out.println("Hit preposition " + word.getLemma());
                    Synonym verb = wordnetUtils.getTopVerbForNounWithPreposition(word.getLemma().toLowerCase());
                    //List<Synonym> verbsForNounWithPreposition = wordnetUtils.getVerbForNounWithPreposition(word.getLemma().toLowerCase());
                    //if (verbsForNounWithPreposition.size() > 0) {
                    if (verb != null) {
                        //listOfRelatedWords.add(verbsForNounWithPreposition);
                        listOfRelatedWords.add(wordnetUtils.getTopSynonyms(verb.getWord(), BotPOS.VERB, limit));
                    } else {
                        listOfRelatedWords.add(wordnetUtils.getTopSynonyms(word.getLemma().toLowerCase(), BotPOS.NOUN, limit));
                    }
                    break;
                default:
                    listOfRelatedWords.add(wordnetUtils.getTopSynonyms(word.getLemma().toLowerCase(), pos, limit));
                    break;
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
            StringBuilder ads = new StringBuilder();
            StringBuilder mainWords = new StringBuilder();
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

                if (synonym.getPos() == BotPOS.ADVERB || synonym.getPos() == BotPOS.ADJECTIVE ) {
                    ads.append(" " + synonym.getWord());
                } else if (synonym.getPos() == BotPOS.WH_QUESTION) {
                    whClause.append(" " + synonym.getWord());
                } else if (synonym.getPos() == BotPOS.CARDINAL_NUMBER) {
                    cardinalNumber.append(" " + synonym.getWord());
                } else {
                    mainWords.append(" " + synonym.getWord());
                }

                //Remove if mainwords take care
                if (synonym.getPos() == BotPOS.NOUN) {
                    nouns.append(" " + synonym.getWord());
                } else if (synonym.getPos() == BotPOS.VERB) {
                    verbs.append(" " + synonym.getWord());
                } else if (synonym.getPos() == BotPOS.ENTITY) {
                    namedEntities.append(" " + synonym.getWord());
                }
            }
            System.out.println("Adding to Index. nouns: "+ nouns+" verbs: "+verbs + " namedEntity: "+namedEntities+" wc: "+whClause+" ads: "+ads+" mainWords: "+mainWords);
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
            if (ads.length() > 0) {
                doc.add(new TextField("ads", ads.toString(), Field.Store.NO));
            }
            if (mainWords.length() > 0) {
                doc.add(new TextField("mainWords", mainWords.toString(), Field.Store.NO));
            }

            //TODO: Add as a single or fewer fields
            //TODO: Can we add synsets instead of words?
            //TODO: Limit the System.out.printlns
            //TODO: Add adjectives and adverbs
            //TODO: Boosting


            doc.add(new StoredField("exactCount", exactCount));
            doc.add(new StoredField("synonymCount", synonymCount));
            doc.add(new StoredField("hypernymCount", hypernymCount));
            doc.add(new StoredField("hyponymCount", hyponymCount));
            doc.add(new StoredField("questionId", question.getQuestionId()));
            doc.add(new StoredField("debug", "nouns:" + nouns.toString() +
                    " verbs: " + verbs.toString() +
                    " wc: " + whClause.toString() +
                    " cd: " + cardinalNumber.toString() +
                    " namedEntity: " + namedEntities.toString() +
                    " ads: " + ads.toString() +
                    " mainwords: " + mainWords.toString()
            )); //Storing the field for analyzing
            indexWriter.addDocument(doc);
        }
    }

    private ScoreDoc[] queryIndex(TopScoreDocCollector collector, List<Word> words) throws IOException, ParseException {
        StringBuilder nouns = new StringBuilder();
        StringBuilder verbs = new StringBuilder();
        StringBuilder ads = new StringBuilder();
        StringBuilder mainWords = new StringBuilder();
        StringBuilder cardinalNumber = new StringBuilder();
        StringBuilder whClause = new StringBuilder();
        StringBuilder entities = new StringBuilder();
        ScoreDoc[] hits = null;

        for (Word word : words) {
            if (EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.contains(word.getLemma())) {
                continue;
            }

            if (word.getPos() == BotPOS.DERIVE_VERB) {
                Word syn = wordnetUtils.getTopVerbForNounWithPreposition(word.getLemma());
                if (syn != null) {
                    word = syn;
                } else {
                    word.setPos(BotPOS.NOUN);
                }
            }

            if (word.getPos() == BotPOS.ADVERB || word.getPos() == BotPOS.ADJECTIVE ) {
                ads.append(" " + word.getWord());
            } else if (word.getPos() == BotPOS.CARDINAL_NUMBER) {
                cardinalNumber.append(" ").append(word.getLemma()); //exact search for cardinal nos.
            } else if (word.getPos() == BotPOS.WH_QUESTION) {
                whClause.append(" ").append(word.getLemma());
            } else {
                mainWords.append(" " + word.getWord());
            }

            //Remove if mainwords take care
            if (word.getPos() == BotPOS.NOUN) {
                nouns.append(" ").append(word.getLemma());
            } else if (word.getPos() == BotPOS.VERB) {
                verbs.append(" ").append(word.getLemma());
            } else if (word.getPos() == BotPOS.ENTITY) {
                entities.append(" ").append(word.getLemma());
            }
        }
        StringBuilder query1 = new StringBuilder();
        StringBuilder query2 = new StringBuilder();

        if (nouns.length() > 0) {
            query1.append("+nouns:(" + nouns.toString().trim() + ")");
        }
        if (verbs.length() > 0) {
            query1.append(" +verbs:(" + verbs.toString().trim() + ")");
        }
        if (cardinalNumber.length() > 0) {
            query1.append(" +cd:(" + cardinalNumber.toString().trim() + ")");
            query2.append(" cd:(" + cardinalNumber.toString().trim() + ")");
        }
        if (whClause.length() > 0) {
            query1.append(" +wc:(" + whClause.toString().trim() + ")");
            query2.append(" +wc:(" + whClause.toString().trim() + ")");
        }
        if (entities.length() > 0) {
            query1.append(" +namedEntity:(" + entities.toString().trim() + ")");
            query2.append(" namedEntity:(" + entities.toString().trim() + ")^2");
        }
        if (ads.length() > 0) {
            query1.append(" +ads:(" + ads.toString().trim() + ")");
            query2.append(" ads:(" + ads.toString().trim() + ")");
        }
        if (mainWords.length() > 0) {
            query2.append(" mainWords:(" + mainWords.toString().trim() + ")^10");
        }

        //TODO: Implement fuzzy query
        String queryToUse = query2.toString(); //TODO: Remove query1
        System.out.println("Query is:" + queryToUse);
        if (queryToUse.length() > 0) {
            Query q = new QueryParser("", searchIndex.getAnalyzer()).parse(queryToUse);
            searchIndex.getIndexSearcher().search(q, collector);
            hits = collector.topDocs().scoreDocs;
        }
        return hits;
    }
}