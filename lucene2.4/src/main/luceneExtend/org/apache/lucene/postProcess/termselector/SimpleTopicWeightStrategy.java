/**
 *@Author: Jun Miao
 *Combine the Rocchio weight with the Topic weight in the most related topic directly
 *for each candidate feedback term 
 */
package org.apache.lucene.postProcess.termselector;

import java.util.HashMap;

import org.apache.lucene.postProcess.QueryExpansionModel;
import org.apache.lucene.postProcess.termselector.LatentDirichletAllocation.GibbsSample;
import org.dutir.lucene.util.TermsCache;
import org.dutir.lucene.util.ExpansionTerms.ExpansionTerm;
import org.dutir.util.symbol.SymbolTable;

/**
 * @author James
 *
 */
public class SimpleTopicWeightStrategy implements ExpTermWeightStrategy {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.lucene.postProcess.termselector.ExpTermWeightStrategy#rankExpTerms
     * (org.dutir.util.symbol.SymbolTable,
     * org.apache.lucene.postProcess.termselector
     * .LatentDirichletAllocation.GibbsSample,
     * org.apache.lucene.postProcess.QueryExpansionModel, float[], int[],
     * org.apache.lucene.postProcess.termselector.LatentDirichletAllocation,
     * org.apache.lucene.postProcess.termselector.TermAssociation)
     */
    public double getTermWeight( GibbsSample sample, QueryExpansionModel QEModel,
            int index, TermsCache.Item item,
            double [][]qtermTopicProb,
            int[] thetaIndex,
          int maxTopic, double beta) {
        
        float TF = item.ctf;
        float DF = item.df;
        float weight = 0;
        int feedbackNum = sample.numDocuments();
        // use probability in top 1 topic as original weight in one
        // feedback doc, add all the score up and divide by the doc num
        // then rank
        for (int d = 0; d < feedbackNum; d++) {
            double docProb = sample.docWordCount(d, index)
                    / (float) sample.documentLength(d);
            if (docProb == 0) {
                continue;
            }
            double topicProb = sample.topicWordProb(maxTopic, index);
            double onedocWeight = (1 - beta) * docProb + beta
                    * topicProb;
            // one doc weight is the original doc weight smoothed by the
            // topic prob
            QEModel.setTotalDocumentLength(1);
            weight += QEModel.score((float) onedocWeight, TF, DF);
        }
        weight /= feedbackNum;
        return weight;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
