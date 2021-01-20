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

    public static void main(String[] args) {
        int limit = 100;
        int x = 9;
        System.out.println((int) Math.pow(limit * 1.0 / x, 1.0 / 3));
    }

    public List<Match> search(String question) throws JWNLException {
        List<Match> response = new ArrayList<>();
        int exactCount, synonymCount, hypernymCount, hyponymCount;
        int hitsPerPage, totalHitsThreshold;
        double synScore;
        ScoreDoc[] hits;
        TopScoreDocCollector collector;
        hitsPerPage = totalHitsThreshold = props.HITS_THRESHOLD;

        List<BotWord> botWords = sentenceAnalyzer.getPosElements(question); //decompose the question string into words with POS tags
        List<List<BotWord>> synsetCombinations = new ArrayList<>();

        try {
            synsetCombinations.add(botWords); //Add the input words as the first set

            BotWord synonymSynset;
            List<BotWord> combination = new ArrayList<>();

            for (BotWord botWord : botWords) {
                if (botWord.getPos() == BotPOS.CARDINAL_NUMBER || botWord.getPos() == BotPOS.WH_QUESTION) {
                    synonymSynset = botWord;
                } else {
                    synonymSynset = wordnetUtils.getOneSynsetForTheWord(botWord.getLemma(), botWord.getPos(), Link.sim);
                    if (synonymSynset == null) {
                        synonymSynset = botWord;
                    }
                }
                combination.add(synonymSynset);
            }
            if (!(combination.containsAll(botWords) && botWords.containsAll(combination))) { //We could not get synset combination for any of the words in Q
                synsetCombinations.add(combination);
            }

            System.out.println("Total search combinations: " + synsetCombinations.size());
            boolean fuzzy = true; // for the first combination that is the word
            for (List<BotWord> synsetCombination : synsetCombinations) {
                collector = TopScoreDocCollector.create(hitsPerPage, totalHitsThreshold);
                hits = query(formQueryFromWords(synsetCombination, fuzzy), collector);
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
        try {
            IndexWriter indexWriter = searchIndex.getIndexWriterForWrite("APPEND");         //Open the index for Append
            List<String> top2QueriesForTheQuestion = addQuestionToIndex(indexWriter, question);
            searchIndex.closeIndexWriter(indexWriter);
            updateQuestionScore(question, top2QueriesForTheQuestion);
        } catch (IOException | JWNLException e) {
            e.printStackTrace();
        }
    }

    private void updateQuestionScore(Question question, List<String> top2QueriesForTheQuestion) {
        TopScoreDocCollector collector;
        String query = top2QueriesForTheQuestion.get(0);
        collector = TopScoreDocCollector.create(1, 1); //need to create this for every query
        try {
            ScoreDoc[] hits = query(query, collector);
            if (hits.length > 0) {
                question.setMaxPossibleScoreForMainWords(hits[0].score);
                System.out.println("Calc max: " + query + " " + question.getMaxPossibleScoreForMainWords());
            } else {
                System.out.println("Strange question: " + question.getQuestionString());
            }

            if (top2QueriesForTheQuestion.size() > 1) {
                query = top2QueriesForTheQuestion.get(1);
                collector = TopScoreDocCollector.create(1, 1); //need to create this for every query
                hits = query(query, collector);
                if (hits.length > 0) {
                    question.setMaxPossibleScoreForSynsets(hits[0].score);
                    System.out.println("Calc max syn: " + query + " " + question.getMaxPossibleScoreForSynsets());
                } else {
                    System.out.println("No hits for synsets!: " + question.getQuestionString());
                }
            } else {
                question.setMaxPossibleScoreForSynsets(Integer.MAX_VALUE);
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        jtm.update("update question set max_possible_score_main = ?, max_possible_score_synsets = ?, question_words = ? where question_id = ?",
                question.getMaxPossibleScoreForMainWords(), question.getMaxPossibleScoreForSynsets(), top2QueriesForTheQuestion.get(0), question.getQuestionId());
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
                List<String> top2QueriesForTheQuestion = topScorerList.get(i);
                updateQuestionScore(question, top2QueriesForTheQuestion);
                i++;
            }
        } catch (IOException | JWNLException e) {
            e.printStackTrace();
        }
    }

    private List<String> addQuestionToIndex(IndexWriter indexWriter, Question question) throws IOException, JWNLException {
        Document doc;
        List<BotWord> mainSentenceList = new ArrayList<>();
        List<List<SynonymSynset>> listOfListOfRelatedSynsets = new ArrayList<>();
        int exactCount, synonymCount, hypernymCount, hyponymCount;

        List<BotWord> botWords = sentenceAnalyzer.getPosElements(question.getQuestionString());
        List<String> top2Queries = new ArrayList<>();

        int count = 0;
        for (BotWord botWord : botWords) {
            if (!(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.contains(botWord.getLemma())) &&
                    (botWord.getPos() == BotPOS.NOUN ||
                            botWord.getPos() == BotPOS.VERB ||
                            botWord.getPos() == BotPOS.ENTITY)) {//||
                //word.getPos() == BotPOS.DERIVE_VERB)) { //Not getting synonyms for ADJ and ADV
                count++;
            }
        }

        int limit = props.COMBINATIONS_THRESHOLD; //Math.min(10, Math.max(1, props.COMBINATIONS_THRESHOLD / (int) Math.pow(10, count)));

        List<Integer> countOfSynAndSim = new ArrayList<>();
        int availableCombinations = 1;
        int nonNullSynsets = 0;

        for (BotWord botWord : botWords) {
            BotPOS pos = botWord.getPos();
            //System.out.println("DEBUGGING" + word.getLemma() + " "+pos);
            if (EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.contains(botWord.getLemma())) {
                continue;
            }
            BotWord mainBotWord = new BotWord();
            mainBotWord.setWord(botWord.getWord());
            mainBotWord.setLemma(botWord.getLemma());
            mainBotWord.setLinkType(Link.also);
            mainBotWord.setPos(pos);
            List<SynonymSynset> topSynsets;
            mainSentenceList.add(mainBotWord);
            if (pos != BotPOS.CARDINAL_NUMBER || pos != BotPOS.WH_QUESTION) {
                topSynsets = wordnetUtils.getTopSynsets(botWord.getLemma(), pos, limit);
                if (topSynsets.size() == 0) {
                    topSynsets = null;
                    countOfSynAndSim.add(0);
                } else {
                    count = 0;
                    for (SynonymSynset s : topSynsets) {
                        if (s.getLinkType() == Link.syns || s.getLinkType() == Link.sim) {
                            count++;
                        }
                    }
                    countOfSynAndSim.add(count);
                    if (count > 0) {
                        availableCombinations *= count;
                        nonNullSynsets++;
                    }
                }
            } else {
                topSynsets = null;
                countOfSynAndSim.add(0);
            }
            listOfListOfRelatedSynsets.add(topSynsets);
        }
        int product;
        int[][] allCombinations = null;

        if (availableCombinations > limit || nonNullSynsets == 0) {
            product = 0;
        } else {
            //compute the fraction
            double factor = Math.pow(limit * 1.0 / availableCombinations, 1.0 / nonNullSynsets);
            // create a new list with factored values
            int[] counts = new int[mainSentenceList.size()];
            product = 1;
            for (int i = 0; i < listOfListOfRelatedSynsets.size(); i++) {
                if (listOfListOfRelatedSynsets.get(i) != null && listOfListOfRelatedSynsets.get(i).size() > 0) { // for WH clause etc
                    counts[i] = Math.min((int) (countOfSynAndSim.get(i) * factor), listOfListOfRelatedSynsets.get(i).size());
                    product *= counts[i];
                } else {
                    counts[i] = 1;
                }
            }
            if (product > 0) {
                allCombinations = Utilities.getAllCombinations(counts);
            }
        }

        System.out.println(question.getQuestionString() + " product: " + product);

        for (int j = 0; j < product + 1; j++) { //Added +1 for words in the main sentence
            doc = new Document();
            StringBuilder cardinalNumber = new StringBuilder();
            StringBuilder whClause = new StringBuilder();
            StringBuilder ads = new StringBuilder();
            StringBuilder mainWords = new StringBuilder();

            exactCount = synonymCount = hypernymCount = hyponymCount = 0;
            Link linkType;
            SynonymSynset synonymSynset;
            String wordString;
            BotPOS pos;

            for (int i = 0; i < mainSentenceList.size(); i++) {
                if (j < product && listOfListOfRelatedSynsets.get(i) != null) {
                    synonymSynset = listOfListOfRelatedSynsets.get(i).get(allCombinations[i][j]);
                    linkType = synonymSynset.getLinkType();
                    wordString = synonymSynset.getWord();
                    pos = synonymSynset.getPos();
                } else {
                    wordString = mainSentenceList.get(i).getLemma();
                    linkType = mainSentenceList.get(i).getLinkType();
                    pos = mainSentenceList.get(i).getPos();
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

                classifyWords(wordString, pos, ads, whClause, cardinalNumber, mainWords);
            }

            if (j == 0 || j == product) { // Return the first (main) and last combination to help calculate top score
                boolean fuzzy = false;
                if (j == product) {
                    fuzzy = true;
                }
                String query = getQueryString(cardinalNumber, whClause, ads, mainWords, fuzzy);

                if (query.length() > 0)
                    top2Queries.add(0, query.toString());
            }

            //System.out.println("Adding to Index. wc: " + whClause + " ads: " + ads + " mainWords: " + mainWords);
            if (cardinalNumber.length() > 0) {
                doc.add(new TextField("cd", cardinalNumber.toString(), Field.Store.NO));
            }
            if (whClause.length() > 0) {
                doc.add(new TextField("wc", whClause.toString(), Field.Store.NO));
            }
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
                    " cd: " + cardinalNumber.toString() +
                    " ads: " + ads.toString() +
                    " mainwords: " + mainWords.toString()
            )); //Storing the field for analyzing
            indexWriter.addDocument(doc);
        }
        return top2Queries;
    }

    private void classifyWords(String wordString, BotPOS pos, StringBuilder ads, StringBuilder whClause, StringBuilder cardinalNumber, StringBuilder mainWords) {
        if (!EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.contains(wordString)) {
            if (pos == BotPOS.ADVERB || pos == BotPOS.ADJECTIVE) {
                ads.append(" " + wordString);
            } else if (pos == BotPOS.WH_QUESTION) {
                whClause.append(" " + wordString);
            } else if (pos == BotPOS.CARDINAL_NUMBER) {
                cardinalNumber.append(" " + wordString);
            } else {
                mainWords.append(" " + wordString);
            }
        }
    }

    private String getQueryString(StringBuilder cardinalNumber, StringBuilder whClause, StringBuilder ads, StringBuilder mainWords, boolean fuzzy) {
        StringBuilder query = new StringBuilder();
        String token;
        if (fuzzy) {
            token = "$1~$2";
        } else {
            token = "$1$2";
        }

        if (cardinalNumber.length() > 0)
            query.append(" +cd:(" + cardinalNumber.toString().trim() + ")");
        if (whClause.length() > 0)
            query.append(" +wc:(" + whClause.toString().trim() + ")");
        if (ads.length() > 0)
            query.append(" ads:(" + ads.toString().trim().replaceAll("([^\\s])(\\s|$)", token) + ")");
        if (mainWords.length() > 0)
            query.append(" mainWords:(" + mainWords.toString().trim().replaceAll("([^\\s])(\\s|$)", token) + ")^10");
        return query.toString();
    }

    private String formQueryFromWords(List<BotWord> botWords, boolean fuzzy) throws IOException, ParseException {
        StringBuilder whClause = new StringBuilder();
        StringBuilder cardinalNumber = new StringBuilder();
        StringBuilder mainWords = new StringBuilder();
        StringBuilder ads = new StringBuilder();

        for (BotWord botWord : botWords) {
            classifyWords(botWord.getLemma(), botWord.getPos(), ads, whClause, cardinalNumber, mainWords);
        }

        return getQueryString(cardinalNumber, whClause, ads, mainWords, fuzzy);
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