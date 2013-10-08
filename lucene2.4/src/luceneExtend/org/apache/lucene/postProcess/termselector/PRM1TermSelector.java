/**
 * 
 */
package org.apache.lucene.postProcess.termselector;

import gnu.trove.TObjectFloatHashMap;

import java.io.IOException;
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
		float wordDoc[];
		float ctf =0; //collection term frequency
		float cf =0; 
		float collectWeight = -1;
		int df = 0;
		int []termPositions;

		public Structure(int len) {
			wordDoc = new float[len];
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
	static String dmu = ApplicationSetup.getProperty("dlm.mu", "500");
	static float mu = Integer.parseInt(ApplicationSetup.getProperty("rm.mu", dmu));
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
		float numOfTokens = this.searcher.getNumTokens(field);
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
					stru.wordDoc[i] = score(freqs[j], dl, stru.ctf, numOfTokens); //calculating the score of a term in a feedback doc
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

	

}
