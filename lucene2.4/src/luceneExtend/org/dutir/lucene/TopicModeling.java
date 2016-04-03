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

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.Searcher;
import org.dutir.lucene.util.ApplicationSetup;
import org.dutir.util.symbol.MapSymbolTable;

/**
 * @author James Miao
 * The 
 *
 */
public class TopicModeling {

    /**
     * 
     */
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
        MapSymbolTable SYMBOL_TABLE = new MapSymbolTable();
        IndexReader indexReader = this.searcher.getIndexReader();
        int collectionSize = indexReader.maxDoc();
        int[][] DOC_WORDS = new int[collectionSize][];  
        TermEnum terms = indexReader.terms();
        HashSet<String> uniqueTerms = new HashSet<String>();
        while (terms.next()) {
                final Term term = terms.term();
                uniqueTerms.add(term.text());
        }
        Iterator termItr = uniqueTerms.iterator();
        while (termItr.hasNext())
            SYMBOL_TABLE.getOrAddSymbol((String) termItr.next());
        
        for (int i=0; i<indexReader.maxDoc(); i++) {
            if (indexReader.isDeleted(i))
                continue;
            Document doc = indexReader.document(i);
            String docId = doc.get("docId");
            // do something with docId here...
            
            
        }
        
        
        
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
