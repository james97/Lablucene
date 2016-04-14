/**
 * 
 */
package org.dutir.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.Searcher;
import org.apache.tools.Docno2DocInnerID;
import org.dutir.lucene.util.ApplicationSetup;
import org.dutir.lucene.util.LdaReportingHandler;
import org.dutir.util.Arrays;

import com.aliasi.cluster.LatentDirichletAllocation;
import com.aliasi.symbol.MapSymbolTable;
import com.aliasi.symbol.SymbolTable;

/**
 * @author James Miao
 * The 
 *
 */
public class TopicModeling {

    /**
     * 
     */
    private static Logger logger = Logger.getLogger(TopicModeling.class);
    protected Searcher searcher = null;
    private String topicModelName = ApplicationSetup.getProperty("Topic.Model.Name", "lda"); 
    static private Random RANDOM = new Random(43);
    private String field = ApplicationSetup.getProperty(  
            "Lucene.QueryExpansion.FieldName", "content");
    
    
    public TopicModeling() {
        // TODO Auto-generated constructor stub
        this.searcher = ISManager.getSearcheFromPropertyFile();
    }
    
    public void getTopics() throws IOException{
        /**More models can be implemented with different parameter settings.**/
        if (this.topicModelName == "lda")
            this.get_LDA_Topics();
        
    }
    
    private void get_LDA_Topics() throws IOException{
        
        String outputFilePath = ApplicationSetup.getProperty("TopicModel.outputPath", "./lda_topic.txt"); 

        /*Initialize LDA parameters*/
        short numOfTopics = Short.parseShort(ApplicationSetup.getProperty("TopicModel.Topic_Number","500"));
        double docTopicPrior = 2d/numOfTopics;
        double topicWordPrior = Double.parseDouble(ApplicationSetup.getProperty("TopicModel.Lda_Word_Prior","0.01"));
        int burninEpochs = Integer.parseInt(ApplicationSetup.getProperty("TopicModel.Lda_Burnin","10"));
        int sampleLag = Integer.parseInt(ApplicationSetup.getProperty("TopicModel.Lda_Sample_Lag","5"));
        int numOfSamples = Integer.parseInt(ApplicationSetup.getProperty("TopicModel.Lda_Sample_Number","5"));
        
       //Get doc id list and build the DOC_WORDS matrix
        SymbolTable SYMBOL_TABLE = new MapSymbolTable();
        IndexReader indexReader = this.searcher.getIndexReader();
        int collectionSize = indexReader.maxDoc();
        int[][] DOC_WORDS = new int[collectionSize][];  
        TermEnum terms = indexReader.terms();
        HashSet<String> uniqueTerms = new HashSet<String>();
        while (terms.next()) {
                final Term term = terms.term();
                uniqueTerms.add(term.text());
        }
        Iterator<String> termItr = uniqueTerms.iterator();
        while (termItr.hasNext())
            SYMBOL_TABLE.getOrAddSymbol((String) termItr.next());
        
        
        logger.info("Start to construct Doc_Word Matrix ");
        int [] docIds = new int[indexReader.maxDoc()];
        for (int i = 0; i<indexReader.maxDoc(); i++) {
            if (indexReader.isDeleted(i))
                continue;
            Document doc = indexReader.document(i);           
            int docId = Integer.parseInt(Docno2DocInnerID.getDocId(doc.get("DOCNO")));
            docIds[i] = docId;
            // Get terms and according frequencies for this document
            TermFreqVector tfv = null;
            logger.debug("Get term info from Document " + docId);
            try {
                tfv = indexReader.getTermFreqVector(docId,
                        field);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (tfv == null){
                logger.warn("document " + docId + " not found, field=" + field);
                DOC_WORDS[i] = new int[1];
                int pos = 0;
                while  (pos < DOC_WORDS[i].length){
                    int range = uniqueTerms.size();
                    int sampleTerm = new Random().nextInt(range);
                    ArrayList<String> list = new ArrayList<String>(uniqueTerms);
                    DOC_WORDS[i][pos++] = SYMBOL_TABLE.symbolToID(list.get(sampleTerm));
                }
            }
            else {
                String strterms[] = tfv.getTerms();
                int freqs[] = tfv.getTermFrequencies();
                String termCache[] = strterms;
                int[] termFreq = freqs;
                int len = Arrays.sum(termFreq);
                DOC_WORDS[i] = new int[len];
                int pos = 0;
                //logger.debug(String.format("The first term of doc %d is %s", docId, termCache[0]));
                
                for (int termIndex = 0; termIndex < termCache.length; termIndex++) {
                    String term = termCache[termIndex];
                    int id = SYMBOL_TABLE.getOrAddSymbol(term);
                    for (int k = 0; k < termFreq[termIndex]; k++) {
                        DOC_WORDS[i][pos++] = id;
                    }
                }
                assert len == pos;
            }
            
        }
        
        logger.info("Constructing Doc_Word Matrix complete ");
        /*Shuffle terms in a doc/word vector for sampling*/
//        logger.debug("Shuffle terms in the Doc_Word Matrix ");
//                
//        for (int[] words : DOC_WORDS){
//            Arrays.permute(words, RANDOM);
//        }
//        logger.debug("Shuffle done");
//      
        logger.info("Start LDA... ");
        LdaReportingHandler handler
        = new LdaReportingHandler(SYMBOL_TABLE);
        
        LatentDirichletAllocation.GibbsSample sample
        = LatentDirichletAllocation .gibbsSampler(DOC_WORDS,
                      numOfTopics,
                      docTopicPrior,
                      topicWordPrior,
                      burninEpochs,
                      sampleLag,
                      numOfSamples,
                      RANDOM,
                      handler);
        logger.info("LDA sampling is done! ");
        
        handler.storeDocTopicReport(sample,docIds,false, outputFilePath);
        
    }

    /**
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        // TODO Auto-generated method stub
        
        String[] WORDS = new String[] {
            "river",  "stream",  "bank",  "money",  "loan"
        };

        SymbolTable SYMBOL_TABLE = new MapSymbolTable();
        {
            for (String word : WORDS)
                SYMBOL_TABLE.getOrAddSymbol(word);
        }

       final int[][] DOC_WORDS = new int[][] {
            { 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4 },
            { 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4 },
            { 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 4, 4, 4, 4 },
            { 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 4, 4, 4 },
            { 2, 2, 2, 2, 2, 2, 2, 3, 3, 4, 4, 4, 4, 4, 4, 4 },
            { 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 4, 4, 4, 4 },
            { 0, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4 },
            { 0, 1, 1, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4 },
            { 0, 1, 1, 1, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4 },
            { 0, 0, 1, 1, 1, 2, 2, 2},
            { 0, 0, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 4 },
            { 0, 0, 0, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 3 },
            { 0, 0, 0, 0, 0, 0, 1, 1, 1, 2, 2, 2, 2, 2, 2, 4 },
            { 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2 },
            { 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2 },
            { 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1 }
        };

            int[] docIds = new int[DOC_WORDS.length];
            for(int i =0; i< DOC_WORDS.length; i++)
                docIds[i] = i;
        
            for (int[] words : DOC_WORDS)
                Arrays.permute(words,RANDOM);
        
            short NUM_TOPICS = 5;
            double DOC_TOPIC_PRIOR = 0.1;
            double TOPIC_WORD_PRIOR = 0.01;

            int BURNIN_EPOCHS = 10;
            int SAMPLE_LAG = 1;
            int NUM_SAMPLES = 16;

            Random RANDOM = new Random(43);
            
            LdaReportingHandler handler
            = new LdaReportingHandler(SYMBOL_TABLE);

        LatentDirichletAllocation.GibbsSample sample
            = LatentDirichletAllocation
            .gibbsSampler(DOC_WORDS,
                          NUM_TOPICS,
                          DOC_TOPIC_PRIOR,
                          TOPIC_WORD_PRIOR,
                          BURNIN_EPOCHS,
                          SAMPLE_LAG,
                          NUM_SAMPLES,
                          RANDOM,
                          handler);

        handler.storeDocTopicReport(sample,docIds,false, "topic.txt");
      

    }

}
