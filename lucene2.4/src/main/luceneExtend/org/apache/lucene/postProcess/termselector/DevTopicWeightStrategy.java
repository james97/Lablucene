/**
 * 
 */
package org.apache.lucene.postProcess.termselector;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.lucene.postProcess.QueryExpansionModel;
import org.apache.lucene.postProcess.termselector.LatentDirichletAllocation.GibbsSample;
import org.dutir.lucene.util.TermsCache.Item;

/**
 * @author James
 * Original strategy 2. 
 * Assume terms with larger deviation of probability distribution
 *
 */
public class DevTopicWeightStrategy implements ExpTermWeightStrategy {

    /**
     * 
     */
    public DevTopicWeightStrategy() {
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see org.apache.lucene.postProcess.termselector.ExpTermWeightStrategy#getTermWeight(org.apache.lucene.postProcess.termselector.LatentDirichletAllocation.GibbsSample, org.apache.lucene.postProcess.QueryExpansionModel, int, org.dutir.lucene.util.TermsCache.Item, int, double)
     */
    public double getTermWeight(GibbsSample sample, 
            QueryExpansionModel QEModel, int termIndex, 
            Item item, 
            double [][]qtermTopicProb,
            int[] thetaIndex,
            int maxTopic, double beta) {
        
        float TF = item.ctf;
        float DF = item.df;
        float weight = 0;
        int feedbackNum = sample.numDocuments();
        
        
        double []topicProb = new double[thetaIndex.length];
        for (int k = 0; k < thetaIndex.length; k++)
            topicProb[k] = sample.topicWordProb(thetaIndex[k], termIndex);
        
        StandardDeviation sd =  new StandardDeviation();
        double termDeviation = sd.evaluate(topicProb);
        
        double topicWeight = (float) Math.sqrt(termDeviation * sample.topicWordProb(maxTopic, termIndex));
        
        
        for (int d = 0; d < feedbackNum; d++) {
            double docProb = sample.docWordCount(d, termIndex)
                    / (float) sample.documentLength(d);
            if (docProb == 0) {
                continue;
            }
            double onedocWeight = (1 - beta) * docProb + beta
                    * topicWeight;
            // one doc weight is the original doc weight smoothed by the
            // topic prob
            QEModel.setTotalDocumentLength(1);
            weight += QEModel.score((float) onedocWeight, TF, DF);
        }
        weight /= feedbackNum;
        
        return weight;
        
    }

}
