/**
 * 
 */
package org.dutir.lucene;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
        File file = new File(outputFilePath);
        if (!file.exists()) {
            file.createNewFile();
        }

        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        short numOfTopics = Short.parseShort(ApplicationSetup.getProperty("TopicModel.Topic_Number","100"));
        double docTopicPrior = 2d/numOfTopics;
        double topicWordPrior = Double.parseDouble(ApplicationSetup.getProperty("TopicModel.Lda_Word_Prior","0.01"));
        int burninEpochs = Integer.parseInt(ApplicationSetup.getProperty("TopicModel.Lda_Burnin","80"));
        int sampleLag = Integer.parseInt(ApplicationSetup.getProperty("TopicModel.Lda_Sample_Lag","30"));
        int numOfSamples = Integer.parseInt(ApplicationSetup.getProperty("TopicModel.Lda_Sample_Number","30"));
        
        /*Start LDA*/
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
        
        
        //Get doc id list and build the DOC_WORDS matrix
        int [] docIds = new int[indexReader.maxDoc()];
        for (int i = 0; i<indexReader.maxDoc(); i++) {
            if (indexReader.isDeleted(i))
                continue;
            Document doc = indexReader.document(i);
            int docId = Integer.parseInt(doc.get("docId"));
            docIds[i] = docId;
            // Get terms and according frequencies for this document
            TermFreqVector tfv = null;
            try {
                tfv = indexReader.getTermFreqVector(docId,
                        field);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (tfv == null)
                logger.warn("document " + docId + " not found, field=" + field);
            else {
                String strterms[] = tfv.getTerms();
                int freqs[] = tfv.getTermFrequencies();
                String termCache[] = strterms;
                int[] termFreq = freqs;
                int len = Arrays.sum(termFreq);
                DOC_WORDS[i] = new int[len];
                int pos = 0;
                
                for (int termIndex = 0; termIndex < termCache.length; termIndex++) {
                    String term = termCache[termIndex];
                    int id = SYMBOL_TABLE.getOrAddSymbol(term);
                    for (int k = 0; k < termFreq[termIndex]; k++) {
                        DOC_WORDS[i][pos++] = id;
                    }
                }
                assert len == pos;
            }
            
            /*Shuffle terms in a doc/word vector for sampling*/
            for (int[] words : DOC_WORDS)
                Arrays.permute(words, RANDOM);
            
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
            
            handler.fullReport(sample,5,2,true);
            
        }
        
        
        bw.close();
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
