/**
 * 
 */
package org.apache.lucene.postProcess.termselector;

import org.apache.lucene.MetricsUtils;
import org.apache.lucene.postProcess.QueryExpansionModel;
import org.apache.lucene.postProcess.termselector.LatentDirichletAllocation.GibbsSample;
import org.dutir.lucene.util.TermsCache.Item;

/**
 * @author James
 *
 */
public class SimilarityTopicWeightStrategy implements ExpTermWeightStrategy {

    /**
     * 
     */
    public SimilarityTopicWeightStrategy() {
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see org.apache.lucene.postProcess.termselector.ExpTermWeightStrategy#getTermWeight(org.apache.lucene.postProcess.termselector.LatentDirichletAllocation.GibbsSample, org.apache.lucene.postProcess.QueryExpansionModel, int, org.dutir.lucene.util.TermsCache.Item, int[], int, double)
     */
    public double getTermWeight(GibbsSample sample, QueryExpansionModel QEModel, int termIndex, Item item,
            double [][]qtermTopicProb,
            int[] thetaIndex, 
            int maxTopic, double beta) {
        // TODO Auto-generated method stub
        float TF = item.ctf;
        float DF = item.df;
        float weight = 0;
        int feedbackNum = sample.numDocuments();
        
        
        double []topicProb = new double[thetaIndex.length];
        for (int k = 0; k < thetaIndex.length; k++)
            topicProb[k] = sample.topicWordProb(thetaIndex[k], termIndex);
        
        double similarity = 0d;
        for(int m = 0; m < qtermTopicProb.length; m++){
            similarity += MetricsUtils.cosine_similarity(topicProb, qtermTopicProb[m]);
        }

        similarity /= qtermTopicProb.length;
        
        for (int d = 0; d < feedbackNum; d++) {
            double docProb = sample.docWordCount(d, termIndex)
                    / (float) sample.documentLength(d);
            if (docProb == 0) {
                continue;
            }
            double onedocWeight = docProb * (1 + beta * similarity);
                    
            // one doc weight is the original doc weight smoothed by the
            // topic prob
            QEModel.setTotalDocumentLength(1);
            weight += QEModel.score((float) onedocWeight, TF, DF);
        }
        weight /= feedbackNum;
        
        return weight;
    }

}
