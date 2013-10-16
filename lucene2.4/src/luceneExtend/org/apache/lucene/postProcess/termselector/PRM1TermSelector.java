/**
 * 
 */
package org.apache.lucene.postProcess.termselector;

import gnu.trove.TObjectFloatHashMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.index.TermPositionVector;
import org.apache.lucene.postProcess.QueryExpansionModel;
import org.apache.lucene.postProcess.termselector.ProxTermSelector.GeoExpansionTerm;
import org.apache.lucene.postProcess.termselector.RM3TermSelector.Structure;
import org.apache.lucene.search.model.Idf;
import org.dutir.lucene.IndexUtility;
import org.dutir.lucene.util.ApplicationSetup;
import org.dutir.lucene.util.Rounding;
import org.dutir.lucene.util.ExpansionTerms.ExpansionTerm;
import org.dutir.lucene.util.TermsCache.Item;
import org.dutir.util.Arrays;

/**
 * This is an implementation for the Positional Relevance Model variant 1
 * proposed by Yuanhua Lv & Chengxiang Zhai Paper url:
 * http://dl.acm.org/citation.cfm?id=1835546
 * 
 * @author Jun Miao
 * 
 */
public class PRM1TermSelector extends TermSelector {

	private static Logger logger = Logger.getLogger(PRM1TermSelector.class);
	// private float lambda = Float.parseFloat(ApplicationSetup.getProperty(
	// "expansion.lavrenko.lambda", "0.15"));
	IndexUtility indexUtil = null; // used to obtain statistical information for
									// terms,e.g., idf
	TermSelector selector = null;

	float scores[] = null;

	public void setScores(float _scores[]) {
		scores = new float[_scores.length];
		float max = _scores[0];
		for (int i = 0; i < _scores.length; i++) {
			// scores[i] = (float) Math.exp(_scores[i] +_scores[0]);
			scores[i] = Idf.exp(max + _scores[i]);
		}
	}

	static double lamda = Double.parseDouble(ApplicationSetup.getProperty(
			"prmJM.lamda", "0.5"));
	
	static double sigma = Double.parseDouble(ApplicationSetup.getProperty(
			"guassian.sigma", "0.5"));

	// float numOfTokens = this.searcher.getNumTokens(field);

	
	/*
	 * Return the score of a feedback term with positional information in a feedback term
	 * Implementation of the main part of Formula 6
	 * 
	 * @param fbTerm The object which contains relative information of a feedback term
	 * 
	 * @param queryTermMap The HashMap which contains the information of all original query terms
	 * 
	 * @lamda The tune parameter for JM smoothing in calling queryTermProbabilityWithPosition() method
	 * 
	 * @param sigma The parameter for the Guassian kernel in calling queryTermProbabilityWithPosition() method
	 * 
	 * @param index The index of feedback document in the feedback doc set (Different to document ids in the collection index)
	 * 
	 * @return The score of a feedback term given the original query with positional information in a feedback document
	 * 
	 * @author Jun Miao 10/15/2013 *
	 */
	private double score(fbTermInfo fbTerm, HashMap<String, fbTermInfo> queryTermMap, double sigma,
			double lamda, int index) {

		double returnScore = 0.0;
		
		int [] positions = fbTerm.getpositionPerDoc(index);
		for (int i = 0; i < positions.length; i++){
			returnScore += probQueryWithPositions(queryTermMap, positions[i], sigma, lamda, index);
		}
		returnScore = returnScore / fbTerm.getfbDocLength(index);
		
		return returnScore;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.lucene.postProcess.termselector.TermSelector#assignTermWeights
	 * (int[], org.apache.lucene.postProcess.QueryExpansionModel) This is used
	 * to assign weights for the candidate feedback terms
	 */
	@Override
	public void assignTermWeights(int[] docids, float scores[],
			QueryExpansionModel QEModel) {

		// initialize attributes of this class. termMap is used to store return
		// feedback terms
		// by Jun Miao 08/10/2013
		float collectionLength = this.searcher.getNumTokens(field);
		feedbackSetLength = 0;
		termMap = new HashMap<String, ExpansionTerm>();
		float PD[] = new float[docids.length];
		float docLens[] = new float[docids.length];

		if (indexUtil == null)
			indexUtil = new IndexUtility(this.searcher);
		
		// for each positive feedback document
		// by Jun Miao 08/10/2013
		String sterms[][] = new String[docids.length][];
		int termFreqs[][] = new int[docids.length][];

		HashMap<String, fbTermInfo> feedbackTermMap = new HashMap<String, fbTermInfo>();
		HashMap<String, fbTermInfo> queryTermMap = new HashMap<String, fbTermInfo>();
		
		
		// get all terms in feedback documents as candidate expansion terms
		
		for (int i = 0; i < docids.length; i++) {
			int docid = docids[i];
			TermPositionVector tfv = null;
			try {
				tfv = (TermPositionVector) this.searcher.getIndexReader()
						.getTermFreqVector(docid, field);
			} catch (IOException e) {
				e.printStackTrace();
			}// Interesting! TermPositionVector is actually the same as
				// TermFreqVector
			if (tfv == null)
				logger.warn("document " + docid + " not found, field=" + field);
			else {
				String strterms[] = tfv.getTerms();
				int freqs[] = tfv.getTermFrequencies();
				float dl = Arrays.sum(freqs);
				docLens[i] = dl;
				//feedbackSetLength += dl;

				//Get all the relative information and store it in the two HashMaps
				for (int j = 0; j < strterms.length; j++) {
					fbTermInfo fbTerm = feedbackTermMap.get(strterms[j]);
					if (fbTerm == null) {
						fbTerm = new fbTermInfo(docids.length);
						fbTerm.setdocIds(docids[i], i);
						Item item = getItem(strterms[j]);
						fbTerm.setcollectionProbability(item.ctf);
						fbTerm.setpositionPerDoc(tfv.getTermPositions(j), i);
						fbTerm.setfbDocLength(dl, i);
						fbTerm.setTfPerDoc(freqs[j], i);
						feedbackTermMap.put(strterms[j], fbTerm);
						
						if (this.originalQueryTermidSet.contains(strterms[j]))
							queryTermMap.put(strterms[j], fbTerm);
						
					}
					
				}
				
				//set term scores in each doc by applying position information
				for (int j = 0; j < strterms.length; j++) {
					fbTermInfo fbterm = feedbackTermMap.get(strterms[j]);
					if (fbterm != null){
						double positionalScore = 0;
						positionalScore = score(fbterm, queryTermMap, sigma, lamda, docid);
						fbterm.setWeightPerDoc(positionalScore, docid);
						feedbackTermMap.put(strterms[j], fbterm);
						
					}
					
				}				
				
			}//end of ELSE
		}//end of LOOP for docs
		
		
		
		
		//Add up the scores of each feedback terms and normalize the final score to [0,1]
		
		int termNum = feedbackTermMap.size();
		ExpansionTerm[] exTerms = new ExpansionTerm[termNum];
		if(logger.isDebugEnabled()) logger.debug("the total number of terms in feedback docs: " + termNum);
		
		float total = 0;
		int fbTermCount = 0;
		float sum = 0;
		for (Entry<String, fbTermInfo> entry : feedbackTermMap.entrySet()) {
			String w = entry.getKey();
			fbTermInfo fbTerm = entry.getValue();
			float weight = 0;
			for (int i = 0; i < fbTerm.docNumber; i++) {
				weight += fbTerm.getWeightPerDoc(i);
			}

			if (fbTerm.docNumber < EXPANSION_MIN_DOCUMENTS) {
				weight = 0;
			}

			total += weight;

			exTerms[fbTermCount] = new ExpansionTerm(w, 0);
			exTerms[fbTermCount].setWeightExpansion(weight);
			fbTermCount++;
			sum += weight;
		}

		termNum = fbTermCount;
		
		
		java.util.Arrays.sort(exTerms);

		if (logger.isDebugEnabled()) {
			StringBuilder buf = new StringBuilder();
			int tmpPos = 0;
			for (int i = 0; tmpPos < 40 && i < exTerms.length; i++) {
				if (true || exTerms[i].getWeightExpansion() < 1) {
					tmpPos++;
					buf.append(exTerms[i] + "\t");
				}
			}
			if(logger.isDebugEnabled()) logger.debug("original: " + buf.toString());
		}

		if(logger.isDebugEnabled()) logger.debug("the total weight: " + total);
		if(logger.isDebugEnabled()) logger.debug("maxWeight=" + exTerms[0].getWeightExpansion()
				+ ", minWeight="
				+ exTerms[exTerms.length - 1].getWeightExpansion());
		
		StringBuilder buf = new StringBuilder();
		for (fbTermCount = 0; fbTermCount < termNum; fbTermCount++) {

			if (logger.isDebugEnabled()
					&& this.originalQueryTermidSet.contains(exTerms[fbTermCount]
							.getTerm())) {
				buf.append(exTerms[fbTermCount] + "\t");
			}
			exTerms[fbTermCount].setWeightExpansion(exTerms[fbTermCount].getWeightExpansion() / sum);
			this.termMap.put(exTerms[fbTermCount].getTerm(), exTerms[fbTermCount]);
		}

		if (logger.isDebugEnabled()) {
			logger.debug("original Query Weight: " + buf.toString());
		}

	}

	
	

	/*
	 * Return the query probability given a document and a position.
	 * Implementation of Formula 17
	 * 
	 * @param queryTermMap All query terms denoted by the fbTermInfo objects
	 * 
	 * @param i The position which feedback term w can associate
	 * 
	 * @lamda The tune parameter for JM smoothing in calling
	 * queryTermProbabilityWithPosition() method
	 * 
	 * @param sigma The parameter for the Guassian kernel in calling
	 * queryTermProbabilityWithPosition() method
	 * 
	 * @param index The index of feedback document in the feedback document set
	 * 
	 * @return The query probability given the index-th feedback document and a
	 * position
	 * 
	 * @author Jun Miao 10/15/2013 *
	 */

	private double probQueryWithPositions(
			HashMap<String, fbTermInfo> queryTermMap, int i, double lamda,
			double sigma, int docid) {
		
		double probability = 1;
		
		Iterator<Map.Entry<String, fbTermInfo>> it = queryTermMap.entrySet().iterator();  
        while (it.hasNext()) {  
            Entry<String, fbTermInfo> entry = it.next();  
            String fbterm = entry.getKey();
            fbTermInfo fbterminfo = entry.getValue();
            int []positions = fbterminfo.getpositionPerDoc(docid);
            double colProbability = fbterminfo.getcollectionProbability();
            
            probability *= queryTermProbabilityWithPosition(positions,
        			i, lamda, sigma, colProbability);              
    
        }  
		
        return probability;
	}

	/*
	 * Return the smoothed probability of a term w appearing at position i in a
	 * feedback document based on positional language model (plm).
	 * Implementation of Formula 16
	 * 
	 * @param positionVector All positions of term w in the feedback document
	 * 
	 * @param i The position which term w can associate
	 * 
	 * @lamda The tune parameter for JM smoothing
	 * 
	 * @param sigma The parameter for the Guassian kernel
	 * 
	 * @param colProbability The collection probability of term w
	 * 
	 * @return The smoothed probability of a term w appearing at position i in a
	 * feedback document
	 * 
	 * @author Jun Miao 10/11/2013 *
	 */

	private double queryTermProbabilityWithPosition(int[] positionVector,
			int i, double lamda, double sigma, double colProbability) {
		double probability;

		double plmProbability = propagatedCount(positionVector, i, sigma)
				/ (2 * Math.PI * Math.pow(sigma, 2.0));
		probability = (1 - lamda) * plmProbability + lamda * colProbability;
		return probability;

	}

	/*
	 * Return the total propagated count of term w at position i from the
	 * occurrences of w in all the positions. An implementation of c'(w,i) and
	 * Gaussian kernel is used. Implementation of Formula 13
	 * 
	 * @param positionVector All positions of term w in the feedback document
	 * 
	 * @param i The position which term w can associate
	 * 
	 * @param sigma The parameter for the Guassian kernel
	 * 
	 * @return The total propagated count of term w at position i from the
	 * occurrences of w in all the positions. Actually, it denotes the
	 * association of a term w on the term at position i.
	 * 
	 * @author Jun Miao 10/08/2013 *
	 */

	private double propagatedCount(int[] positionVector, int i, double sigma) {

		double count = 0;
		for (int j = 0; j < positionVector.length; j++) {
			count += Math.exp(-Math.pow(
					(i - positionVector[j]) / (2 * Math.pow(sigma, 2.0)), 2.0));
		}
		return count;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.lucene.postProcess.termselector.TermSelector#getInfo()
	 */
	@Override
	public String getInfo() {
		// TODO Auto-generated method stub
		return "PRM1";
	}

	@Override
	public void assignTermWeights(String[][] terms, int[][] freqs,
			TermPositionVector[] tfvs, QueryExpansionModel QEModel) {
		// TODO Auto-generated method stub

	}

	private class fbTermInfo {
		/**
		 * Information of feedback terms, including tf in each feedback
		 * document, feedback document length, collection probability and
		 * position vectors in all feedback documents Jun Miao 10/11/2013
		 */
		protected int docNumber;
		private int docIds[];
		private double tfPerDoc[];
		private double fbDoclength[];
		private double weightPerDoc[];
		private int positionPerDoc[][] = null;
		private double collectionProbability = -1;
		
		protected void setdocIds(int docId, int index) {
			docIds[index] = docId;
		}

		protected double getdocIds(int index) {
			return docIds[index];
		}

		protected void setTfPerDoc(double tf, int index) {
			tfPerDoc[index] = tf;
		}

		protected double getTfPerDoc(int index) {
			return tfPerDoc[index];
		}

		protected void setfbDocLength(double docLength, int index) {
			fbDoclength[index] = docLength;
		}

		protected double getfbDocLength(int index) {
			return fbDoclength[index];
		}

		protected void setWeightPerDoc(double weight, int index) {
			weightPerDoc[index] = weight;
		}

		protected double getWeightPerDoc(int index) {
			return weightPerDoc[index];
		}

		protected void setpositionPerDoc(int[] positions, int index) {
			positionPerDoc[index] = positions;
		}

		protected int[] getpositionPerDoc(int index) {
			return positionPerDoc[index];
		}

		protected void setcollectionProbability(double colprobability) {
			collectionProbability = colprobability;
		}

		protected double getcollectionProbability() {
			return collectionProbability;
		}

		public fbTermInfo(int len) {
			docIds = new int[len];
			tfPerDoc = new double[len];
			fbDoclength = new double[len];
			weightPerDoc = new double[len];
			positionPerDoc = new int[len][];
			java.util.Arrays.fill(docIds, 0);
			java.util.Arrays.fill(tfPerDoc, 0);
			java.util.Arrays.fill(fbDoclength, 0);
			java.util.Arrays.fill(weightPerDoc, 0);
			docNumber = len;
			
			
		}
	}

}
