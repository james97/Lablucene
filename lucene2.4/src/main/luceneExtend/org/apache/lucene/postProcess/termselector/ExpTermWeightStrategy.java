/**
 * 
 */
package org.apache.lucene.postProcess.termselector;

import org.apache.lucene.postProcess.QueryExpansionModel;
import org.apache.lucene.postProcess.termselector.LatentDirichletAllocation.GibbsSample;
import org.dutir.lucene.util.TermsCache;

/**
 * @author James
 *
 */
public interface ExpTermWeightStrategy {
    public double getTermWeight(
            GibbsSample sample, QueryExpansionModel QEModel,
            int termIndex, TermsCache.Item item,
            double [][]qtermTopicProb,
            int[] thetaIndex, int maxTopic, double beta);
}
