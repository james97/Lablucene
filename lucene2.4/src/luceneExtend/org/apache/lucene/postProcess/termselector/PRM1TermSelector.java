/**
 * 
 */
package org.apache.lucene.postProcess.termselector;

import gnu.trove.TObjectFloatHashMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

/** This is an implementation for the Positional Relevance Model variant 1 proposed by Yuanhua Lv & Chengxiang Zhai
 *  Paper url: http://dl.acm.org/citation.cfm?id=1835546
 *  
 * @author Jun Miao 
 * 
 */
public class PRM1TermSelector extends TermSelector {

	private static Logger logger = Logger.getLogger(PRM1TermSelector.class);
//	private float lambda = Float.parseFloat(ApplicationSetup.getProperty(
//			"expansion.lavrenko.lambda", "0.15"));
	IndexUtility indexUtil = null; //used to obtain statistical information for terms,e.g., idf 
	TermSelector selector = null;

	class Structure {
		/**
		 * document level LM score (dir smoothing) for each candidate word in docs. 
		 */
		class termPositionVector{
			int []termPostions;
		}
		
		float wordDoc[];
		float ctf =0; //collection term frequency
		float cf =0; 
		float collectWeight = -1;
		int df = 0;
		ArrayList<termPositionVector> termPositions; //term position vectors in each feedback document

		public Structure(int len) {
			wordDoc = new float[len];
			termPositions = new ArrayList<termPositionVector>();
		}
		
		public String toString(){
			StringBuilder buf = new StringBuilder();
			buf.append("df = " + df +", ");
			for(int i =0; i < wordDoc.length; i++){
				buf.append("" +Rounding.round( wordDoc[i], 5) + ", ");
			}
			return buf.toString();
		}
	}
	
	float scores[] = null;
	public void setScores(float _scores[]){
		scores = new float[_scores.length];
		float max = _scores[0];
		for(int i=0; i < _scores.length; i++){
//			scores[i] = (float) Math.exp(_scores[i] +_scores[0]);
			scores[i] = Idf.exp(max + _scores[i]);
		}
	}
	
	static double alpha = Double.parseDouble(ApplicationSetup.getProperty("prmJM.alpha", "0.5"));
//	float numOfTokens = this.searcher.getNumTokens(field);
	
	private float score(float tf, float docLength, float termFrequency, float numberOfTokens) {
		float pc = termFrequency / numberOfTokens;
		return  (tf + mu * pc) / (docLength + mu);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.lucene.postProcess.termselector.TermSelector#assignTermWeights
	 * (int[], org.apache.lucene.postProcess.QueryExpansionModel)
	 * This is used to assign weights for the candidate feedback terms
	 */
	@Override
	public void assignTermWeights(int[] docids, float scores[], QueryExpansionModel QEModel) {
		
		//initialize attributes of this class. termMap is used to store return feedback terms
		//by Jun Miao 08/10/2013
		float collectionLength = this.searcher.getNumTokens(field);
		feedbackSetLength = 0;
		termMap = new HashMap<String, ExpansionTerm>();
		float PD[] = new float[docids.length];
		float docLens[] = new float[docids.length];
		
		
		
		if (indexUtil == null)
			indexUtil = new IndexUtility(this.searcher);
		// get all terms in feedback documents as candidate expansion terms
		// for each positive feedback document
		//by Jun Miao 08/10/2013
		String sterms[][] = new String[docids.length][];
		int termFreqs[][] = new int[docids.length][];
		
		
		
		
		HashMap<String, Structure> map = new HashMap<String, Structure>();
		for (int i = 0; i < docids.length; i++) {
			int docid = docids[i];
			TermPositionVector tfv = null;
			try {
				tfv = (TermPositionVector)this.searcher.getIndexReader().getTermFreqVector(docid,
						field);
			} catch (IOException e) {
				e.printStackTrace();
			}//Interesting! TermPositionVector is actually the same as TermFreqVector 
			if (tfv == null)
				logger.warn("document " + docid + " not found, field=" + field);
			else {
				String strterms[] = tfv.getTerms();
				int freqs[] = tfv.getTermFrequencies();
				float dl = Arrays.sum(freqs);
				docLens[i] = dl;
				feedbackSetLength += dl;

				for (int j = 0; j < strterms.length; j++) {
					Structure stru = map.get(strterms[j]);
					if (stru == null) {
						stru = new Structure(docids.length);
						Item item = getItem(strterms[j]);
						stru.ctf = item.ctf; 
						java.util.Arrays.fill(stru.wordDoc, 0);
						map.put(strterms[j], stru);
					}
					//calculating the score of a term in a feedback doc.
					//TODO: Modify this to apply PRM1
					stru.wordDoc[i] = score(freqs[j], dl, stru.ctf, numOfTokens); 																				 
					stru.df++;
				}
			}
		}//extract term positions from the index for each feedback document. streterms[] stores the terms in doc[i] 
		//in an ascending order
		
		
	}

	// In:  log(x1) log(x2) ... log(xN)
	// Out: x1/sum, x2/sum, ... xN/sum
	//
	// Extra care is taken to make sure we don't overflow
	// machine precision when taking exp (log x)
	// This is done by adding a constant K which cancels out
	// Right now K is set to maximally preserve the highest value
	// but could be altered to a min or average, or whatever...
	
	
	
	
	/* Return the query probability given a document and a position. 
	 * Implementation of Formula 17
	 * 
	 * @param queryTermMap   All query terms denoted by the fbTermInfo objects
	 * @param i     The position which feedback term w can associate
	 * @lamda		The tune parameter for JM smoothing in calling queryTermProbabilityWithPosition() method
	 * @param sigma The parameter for the Guassian kernel in calling queryTermProbabilityWithPosition() method
	 * @param docid The id of feedback document
	 * @return 		The query probability given the docid-th feedback document and a position
	 * @author Jun Miao 10/15/2013
	 * **/
	
	 //TODO:finish the method
	private double probQueryWithPositions(HashMap<String, fbTermInfo> queryTermMap, int i, double lamda, double sigma, int docid){
	
		return 0;
	}
	
	
	
	
	
	
	
	
	
	
	/* Return the smoothed probability of a term w appearing at position i in a feedback document based on
	 * positional language model (plm). 
	 * Implementation of Formula 16
	 * 
	 * @param positionVector   All positions of term w in the feedback document
	 * @param i     The position which term w can associate
	 * @lamda		The tune parameter for JM smoothing
	 * @param sigma The parameter for the Guassian kernel
	 * @param colProbability    The collection probability of term w
	 * @return 		The smoothed probability of a term w appearing at position i in a feedback document
	 * @author Jun Miao 10/11/2013
	 * **/
	
	private double queryTermProbabilityWithPosition(int[] positionVector, int i, double lamda, double sigma, double colProbability){
		double probability;
		
		double plmProbability = propagatedCount(positionVector, i, sigma) / (2 * Math.PI * Math.pow(sigma, 2.0));
		probability = (1 - lamda) * plmProbability + lamda * colProbability;
		return probability;
				
	}
	
	
	/* Return the total propagated count of term w at position i from the occurrences of
	 * w in all the positions. An implementation of c'(w,i) and Gaussian kernel is used.
	 * Implementation of Formula 13
	 * 
	 * @param positionVector   All positions of term w in the feedback document
	 * @param i     The position which term w can associate
	 * @param sigma The parameter for the Guassian kernel
	 * @return 		The total propagated count of term w at position i from the occurrences of
	 * w in all the positions. Actually, it denotes the association of a term w on the term at position i.
	 * @author Jun Miao 10/08/2013
	 * **/
	
	private double propagatedCount(int[] positionVector, int i,double sigma){
		
		double count = 0;
		for (int j = 0; j < positionVector.length; j++){
			count += Math.exp(-Math.pow((i - positionVector[j])/(2 * Math.pow(sigma, 2.0)), 2.0));
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
		 * Information of feedback terms, including tf in each feedback document, 
		 * feedback document length, collection probability and position vectors in 
		 * all feedback documents 
		 * Jun Miao 10/11/2013
		 */
		double tfPerDoc[];
		double fbDoclength[];
		double weightPerDoc[];
		int positionPerDoc[][] = null;
		private double collectionProbability = -1;
		
		protected void setTfPerDoc(double tf, int docid){
			tfPerDoc[docid] = tf;
		}
		
		protected double getTfPerDoc(int docid){
			return tfPerDoc[docid];
		}
		
		protected void setfbDocLength(double docLength, int docid){
			fbDoclength[docid] = docLength;
		}
		
		protected double getfbDocLength(int docid){
			return fbDoclength[docid];
		}
		
		protected void setWeightPerDoc(double docLength, int docid){
			fbDoclength[docid] = docLength;
		}
		
		protected double setWeightPerDoc(int docid){
			return weightPerDoc[docid];
		}
		
		protected void setpositionPerDoc(double weight, int docid){
			weightPerDoc[docid] = weight;
		}
		
		protected int[] getpositionPerDoc(int docid){
			return positionPerDoc[docid];
		}
		
		protected void setcollectionProbability(double colprobability){
			collectionProbability = colprobability;
		}
		
		protected double getcollectionProbability(){
			return collectionProbability;
		}


		public fbTermInfo(int len) {
			tfPerDoc = new double[len];
			fbDoclength = new double[len];
			weightPerDoc = new double[len];
			positionPerDoc = new int[len][];
		}
	}
	

	

}
