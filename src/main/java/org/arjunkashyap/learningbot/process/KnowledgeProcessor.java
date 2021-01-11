package org.arjunkashyap.learningbot.process;

import edu.cmu.lti.jawjaw.pobj.Link;
import net.sf.extjwnl.JWNLException;
import org.arjunkashyap.learningbot.Entity.*;
import org.arjunkashyap.learningbot.common.*;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.aspectj.weaver.patterns.IToken;
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
    private BotProperties props;
    @Autowired
    private SentenceAnalyzer sentenceAnalyzer;
    @Autowired
    private SearchIndex searchIndex;
    @Autowired
    WordnetUtils wordnetUtils;

    public List<Match> search(String question) throws JWNLException {
        List<Match> response = new ArrayList<>();
        int exactCount, synonymCount, hypernymCount, hyponymCount;
        int hitsPerPage, totalHitsThreshold;
        double synScore;
        ScoreDoc[] hits;
        TopScoreDocCollector collector;
        hitsPerPage = totalHitsThreshold = props.HITS_THRESHOLD;

        List<Word> words = sentenceAnalyzer.getPosElements(question); //decompose the question string into words with POS tags
        List<List<Word>> synsetCombinations = new ArrayList<>();

        try {
            synsetCombinations.add(words); //Add the input words as the first set

            Word synonymSynset;
            List<Word> combination = new ArrayList<>();

            for (Word word : words) {
                if (word.getPos() == BotPOS.CARDINAL_NUMBER || word.getPos() == BotPOS.WH_QUESTION) {
                    synonymSynset = word;
                } else {
                    synonymSynset = wordnetUtils.getSynsetsOffsetInSameLevel(word.getLemma(), word.getPos());
                    if (synonymSynset == null) {
                        synonymSynset = word;
                    }
                }
                combination.add(synonymSynset);
            }
            if (!(combination.containsAll(words) && words.containsAll(combination))) { //We could not get synset combination for any of the words in Q
                synsetCombinations.add(combination);
            }

            System.out.println("Total search combinations: " + synsetCombinations.size());
            boolean fuzzy = true; // for the first combination that is the word
            for (List<Word> synsetCombination : synsetCombinations) {
                collector = TopScoreDocCollector.create(hitsPerPage, totalHitsThreshold);
                hits = query(formQuery(synsetCombination, fuzzy), collector);
                fuzzy = false; // for the second combination that is the synsets
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
                        if (synonymCount + hypernymCount + hyponymCount == 0) {
                            synScore = 1;
                        } else {
                            synScore = 0.99;//(exactCount * 1 + synonymCount * 0.9 + hypernymCount * 0.8 + hyponymCount * 0.7) / (exactCount + synonymCount + hypernymCount + hyponymCount);
                        }
                        match.setSynonymScore((float) synScore);
                        match.setDebug(d.get("debug"));
                        response.add(match);
                    }
                }
            }

        } catch (IOException | ParseException ioException) {
            ioException.printStackTrace();
        }
        return response;
    }

    public void indexQuestion(Question question) {
        //Open the index for Append
        TopScoreDocCollector collector;
        try {
            IndexWriter indexWriter = searchIndex.getIndexWriterForWrite("APPEND");
            List<String> top2QueriesForTheQuestion = null;
            top2QueriesForTheQuestion = addQuestionToIndex(indexWriter, question);
            searchIndex.closeIndexWriter(indexWriter);

            String query = top2QueriesForTheQuestion.get(0);
            collector = TopScoreDocCollector.create(1, 1); //need to create this for every query
            ScoreDoc[] hits = query(query, collector);
            if (hits.length > 0) {
                question.setMaxPossibleScoreForMainWords(hits[0].score);
                System.out.println("Calc max: " + query + " " + question.getMaxPossibleScoreForMainWords());
            } else {
                System.out.println("Strange question: " + question.getQuestionString());
            }

            query = top2QueriesForTheQuestion.get(1);
            collector = TopScoreDocCollector.create(1, 1); //need to create this for every query
            hits = query(query, collector);
            if (hits.length > 0) {
                question.setMaxPossibleScoreForSynsets(hits[0].score);
                System.out.println("Calc max syn: " + query + " " + question.getMaxPossibleScoreForSynsets());
            } else {
                System.out.println("No hits for synsets!: " + question.getQuestionString());
            }

            jtm.update("update question set max_possible_score_main = ?, max_possible_score_synsets = ? where question_id = ?",
                    question.getMaxPossibleScoreForMainWords(), question.getMaxPossibleScoreForSynsets(), question.getQuestionId());
        } catch (IOException | ParseException | JWNLException e) {
            e.printStackTrace();
        }
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
            IndexWriter indexWriter = searchIndex.getIndexWriterForWrite("CREATE");
            List<List<String>> topScorerList = new ArrayList<>();
            for (Question question : questionList) {
                topScorerList.add(addQuestionToIndex(indexWriter, question));
            }
            searchIndex.closeIndexWriter(indexWriter);

            //For all the questions, set the max score
            int i = 0;
            for (Question question : questionList) {
                //List<Word> words = sentenceAnalyzer.getPosElements(question.getQuestionString());
                String query = topScorerList.get(i).get(0);
                collector = TopScoreDocCollector.create(1, 1); //need to create this for every query
                ScoreDoc[] hits = query(query, collector);
                if (hits.length > 0) {
                    question.setMaxPossibleScoreForMainWords(hits[0].score);
                    System.out.println("Calc max: " + query + " " + question.getMaxPossibleScoreForMainWords());
                } else {
                    System.out.println("Strange question: " + question.getQuestionString());
                }

                query = topScorerList.get(i).get(1);
                collector = TopScoreDocCollector.create(1, 1); //need to create this for every query
                hits = query(query, collector);
                if (hits.length > 0) {
                    question.setMaxPossibleScoreForSynsets(hits[0].score);
                    System.out.println("Calc max syn: " + query + " " + question.getMaxPossibleScoreForSynsets());
                } else {
                    System.out.println("No hits for synsets!: " + question.getQuestionString());
                }

                jtm.update("update question set max_possible_score_main = ?, max_possible_score_synsets = ? where question_id = ?",
                        question.getMaxPossibleScoreForMainWords(), question.getMaxPossibleScoreForSynsets(), question.getQuestionId());
                i++;
            }
        } catch (IOException | ParseException | JWNLException e) {
            e.printStackTrace();
        }
    }

    private List<String> addQuestionToIndex(IndexWriter indexWriter, Question question) throws IOException, JWNLException {
        Document doc;
        List<Synonym> mainSentence = new ArrayList<>();
        List<List<SynonymSynset>> listOfRelatedSynsets = new ArrayList<>();
        int exactCount, synonymCount, hypernymCount, hyponymCount;

        List<Word> words = sentenceAnalyzer.getPosElements(question.getQuestionString());
        List<String> top2Queries = new ArrayList<>();

        int count = 0;
        for (Word word : words) {
            if (!(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.contains(word.getLemma())) &&
                    (word.getPos() == BotPOS.NOUN ||
                            word.getPos() == BotPOS.VERB ||
                            word.getPos() == BotPOS.ENTITY)) {//||
                //word.getPos() == BotPOS.DERIVE_VERB)) { //Not getting synonyms for ADJ and ADV
                count++;
            }
        }

        int limit = Math.min(10, Math.max(1, props.COMBINATIONS_THRESHOLD / (int) Math.pow(10, count)));

        for (Word word : words) {
            BotPOS pos = word.getPos();
            //System.out.println("DEBUGGING" + word.getLemma() + " "+pos);
            if (EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.contains(word.getLemma())) {
                continue;
            }
            Synonym mainWord = new Synonym();
            mainWord.setWord(word.getWord());
            mainWord.setLemma(word.getLemma());
            mainWord.setScore(100);
            mainWord.setLinkType(Link.also);
            mainWord.setPos(pos);
            List<SynonymSynset> topSynsets;
            mainSentence.add(mainWord);
            if (pos != BotPOS.CARDINAL_NUMBER || pos != BotPOS.WH_QUESTION) {
                topSynsets = wordnetUtils.getTopSynsets(word.getLemma(), pos, limit);
                if (topSynsets.size() == 0) {
                    topSynsets = null;
                }
            } else {
                topSynsets = null;
            }
            listOfRelatedSynsets.add(topSynsets);
        }

        System.out.println(question.getQuestionString());
        int[] counts = new int[mainSentence.size()];
        int product = 1;
        for (int i = 0; i < listOfRelatedSynsets.size(); i++) {
            if (listOfRelatedSynsets.get(i) != null && listOfRelatedSynsets.get(i).size() > 0) { //NPE for WH clause etc
                counts[i] = listOfRelatedSynsets.get(i).size();
                product *= counts[i];
            } else {
                counts[i] = 1;
            }
        }
        int[][] allCombinations = Utilities.getAllCombinations(counts, product);

        for (int j = 0; j < product + 1; j++) { //Added +1 for words in the main sentence
            doc = new Document();
            StringBuilder nouns = new StringBuilder();
            StringBuilder verbs = new StringBuilder();
            StringBuilder cardinalNumber = new StringBuilder();
            StringBuilder whClause = new StringBuilder();
            StringBuilder namedEntities = new StringBuilder();
            StringBuilder ads = new StringBuilder();
            StringBuilder mainWords = new StringBuilder();

            exactCount = synonymCount = hypernymCount = hyponymCount = 0;
            Link linkType;
            SynonymSynset synonymSynset;
            String wordString;
            BotPOS pos;

            for (int i = 0; i < mainSentence.size(); i++) {
                if (j < product && listOfRelatedSynsets.get(i) != null) {
                    synonymSynset = listOfRelatedSynsets.get(i).get(allCombinations[i][j]);
                    linkType = synonymSynset.getLinkType();
                    wordString = synonymSynset.getWord();
                    pos = synonymSynset.getPos();
                } else {
                    wordString = mainSentence.get(i).getLemma();
                    linkType = mainSentence.get(i).getLinkType();
                    pos = mainSentence.get(i).getPos();
                }

                switch (linkType) {
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

                if (pos == BotPOS.ADVERB || pos == BotPOS.ADJECTIVE) {
                    ads.append(" " + wordString);
                } else if (pos == BotPOS.WH_QUESTION) {
                    whClause.append(" " + wordString);
                } else if (pos == BotPOS.CARDINAL_NUMBER) {
                    cardinalNumber.append(" " + wordString);
                } else {
                    mainWords.append(" " + wordString);
                }

                //Remove if mainwords take care
                if (pos == BotPOS.NOUN) {
                    nouns.append(" " + wordString);
                } else if (pos == BotPOS.VERB) {
                    verbs.append(" " + wordString);
                } else if (pos == BotPOS.ENTITY) {
                    namedEntities.append(" " + wordString);
                }
            }
            //System.out.println("Adding to Index. nouns: "+ nouns+" verbs: "+verbs + " namedEntity: "+namedEntities+" wc: "+whClause+" ads: "+ads+" mainWords: "+mainWords);

            if (cardinalNumber.length() > 0) {
                doc.add(new TextField("cd", cardinalNumber.toString(), Field.Store.NO));
            }
            if (whClause.length() > 0) {
                doc.add(new TextField("wc", whClause.toString(), Field.Store.NO));
            }
           /* if (namedEntities.length() > 0) {
                doc.add(new TextField("namedEntity", namedEntities.toString(), Field.Store.NO));
                            query.append(" namedEntity:(" + entities.toString().trim() + ")^2");
            }*/ //TODO: Should we add named entity?
            if (ads.length() > 0) {
                doc.add(new TextField("ads", ads.toString(), Field.Store.NO));
            }
            if (mainWords.length() > 0) {
                doc.add(new TextField("mainWords", mainWords.toString(), Field.Store.NO));
            }

            //TODO: OK: Limit the System.out.printlns

            doc.add(new StoredField("exactCount", exactCount));
            doc.add(new StoredField("synonymCount", synonymCount));
            doc.add(new StoredField("hypernymCount", hypernymCount));
            doc.add(new StoredField("hyponymCount", hyponymCount));
            doc.add(new StoredField("questionId", question.getQuestionId()));
            doc.add(new StoredField("debug", " wc: " + whClause.toString() +
                    //"nouns:" + nouns.toString() +
                    //" verbs: " + verbs.toString() +
                    " cd: " + cardinalNumber.toString() +
                    //" namedEntity: " + namedEntities.toString() +
                    " ads: " + ads.toString() +
                    " mainwords: " + mainWords.toString()
            )); //Storing the field for analyzing

            String token;
            if (j == 0 || j == product) { // Return the first (main) and last combination to help calculate top score
                StringBuilder query = new StringBuilder();
                if (j == product) {
                    token="$1~$2";
                } else {
                    token="$1$2";
                }
                if (cardinalNumber.length() > 0)
                    query.append(" +cd:(" + cardinalNumber.toString().trim() + ")");
                if (whClause.length() > 0)
                    query.append(" +wc:(" + whClause.toString().trim() + ")");
                if (ads.length() > 0)
                    query.append(" ads:(" + ads.toString().trim().replaceAll("([^\\s])(\\s|$)", token) + ")");
                if (mainWords.length() > 0)
                    query.append(" mainWords:(" + mainWords.toString().trim().replaceAll("([^\\s])(\\s|$)", token) + ")^10");
                if (query.length() > 0)
                    top2Queries.add(0, query.toString());
            }
            indexWriter.addDocument(doc);
        }
        return top2Queries;
    }

    private String formQuery(List<Word> words, boolean fuzzy) throws IOException, ParseException {
        StringBuilder whClause = new StringBuilder();
        //StringBuilder entities = new StringBuilder();
        StringBuilder cardinalNumber = new StringBuilder();
        StringBuilder mainWords = new StringBuilder();
        StringBuilder ads = new StringBuilder();

        for (Word word : words) {
            if (EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.contains(word.getLemma())) {
                continue;
            }

            if (word.getPos() == BotPOS.CARDINAL_NUMBER) {
                cardinalNumber.append(" ").append(word.getLemma()); //exact search for cardinal nos.
            } else if (word.getPos() == BotPOS.WH_QUESTION) {
                whClause.append(" ").append(word.getLemma());
            } else if (word.getPos() == BotPOS.ADVERB || word.getPos() == BotPOS.ADJECTIVE) {
                ads.append(" " + word.getLemma()+(fuzzy?"~":""));
            } else {
                mainWords.append(" " + word.getLemma()+(fuzzy?"~":""));
            }
        }
        StringBuilder query = new StringBuilder();

        if (cardinalNumber.length() > 0) {
            query.append(" +cd:(" + cardinalNumber.toString().trim() + ")");
        }
        if (whClause.length() > 0) {
            query.append(" +wc:(" + whClause.toString().trim() + ")");
        }

       /* if (entities.length() > 0) {
            query.append(" namedEntity:(" + entities.toString().trim() + ")^2");
        }*/
        if (ads.length() > 0) {
            query.append(" ads:(" + ads.toString().trim() + ")");
        }
        if (mainWords.length() > 0) {
            query.append(" mainWords:(" + mainWords.toString().trim() + ")^10");
        }
        //TODO: Implement fuzzy query?
        //System.out.println("Query is:" + queryToUse);
        return query.toString();
    }

    private ScoreDoc[] query(String queryToUse, TopDocsCollector collector) throws IOException, ParseException {
        ScoreDoc[] hits = null;
        if (queryToUse.length() > 0) {
            Query q = new QueryParser("", searchIndex.getAnalyzer()).parse(queryToUse);
            searchIndex.getIndexSearcher().search(q, collector);
            hits = collector.topDocs().scoreDocs;
        }
        if (hits.length == 0) {
            System.out.println("No results.");
        } else {
            System.out.println("Query is:" + queryToUse + "highest hit: " + hits[0].score);
        }
        return hits;
    }
}