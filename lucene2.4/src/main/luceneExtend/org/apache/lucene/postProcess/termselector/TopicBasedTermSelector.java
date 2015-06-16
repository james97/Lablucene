/**
 * 
 */
package org.apache.lucene.postProcess.termselector;

import gnu.trove.TObjectIntHashMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.FailureMode;
import net.spy.memcached.MemcachedClient;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.log4j.Logger;
import org.apache.lucene.MetricsUtils;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.index.TermPositionVector;
import org.apache.lucene.postProcess.QueryExpansionModel;
import org.apache.lucene.postProcess.termselector.LatentDirichletAllocation.GibbsSample;
import org.apache.lucene.search.model.Idf;
import org.dutir.lucene.util.ApplicationSetup;
import org.dutir.lucene.util.ExpansionTerms.ExpansionTerm;
import org.dutir.lucene.util.Rounding;
import org.dutir.lucene.util.TermsCache;
import org.dutir.util.Arrays;
import org.dutir.util.Normalizer;
import org.dutir.util.symbol.MapSymbolTable;
import org.dutir.util.symbol.SymbolTable;


/**
 * @author Jun Miao
 * 
 */

public class TopicBasedTermSelector extends TermSelector{
	private static Logger logger = Logger.getLogger(TopicBasedTermSelector.class);
	private static MemcachedClient mcc = null;
	String address = "127.0.0.1:11211";

	static boolean LanguageModel = Boolean.parseBoolean(ApplicationSetup
			.getProperty("Lucene.Search.LanguageModel", "false"));
	static int strategy = Integer.parseInt(ApplicationSetup.getProperty(
			"TopicBasedTermSelector.strategy", "3"));
	static boolean expTag = Boolean.parseBoolean(ApplicationSetup.getProperty(
			"TopicBasedTermSelector.expTag", "false"));
	static int expNum = Integer.parseInt(ApplicationSetup.getProperty(
			"TopicBasedTermSelector.expNum", "15"));
	static int expDoc = Integer.parseInt(ApplicationSetup.getProperty(
			"TopicBasedTermSelector.expDoc", "10"));

	static boolean associationTag = Boolean.parseBoolean(ApplicationSetup
			.getProperty("TopicBasedTermSelector.associationTag", "false"));

	static float threshold = Float.parseFloat(ApplicationSetup.getProperty(
			"TopicBasedTermSelector.threshold", "0.2"));
	static float lambda = Float.parseFloat(ApplicationSetup.getProperty(
			"TopicBasedTermSelector.lambda", "0.5"));
	static float beta = Float.parseFloat(ApplicationSetup.getProperty(
			"TopicBasedTermSelector.beta", "0.3"));
	static int winSize = Integer.parseInt(ApplicationSetup.getProperty(
			"Association.winSize", "50"));
	static boolean withOrgScore = Boolean.parseBoolean(ApplicationSetup.getProperty(
            "TopicBasedTermSelector.withOrgScore", "false"));
	static boolean useMemCacheDB = Boolean.parseBoolean(ApplicationSetup.getProperty(
            "TopicBasedTermSelector.useMemCacheDB", "false"));
	static boolean useAdapTopicNumber = Boolean.parseBoolean(ApplicationSetup.getProperty(
            "TopicBasedTermSelector.useAdapTopicNumber", "true"));
	
	static Random RANDOM = new Random(43);
	static short NUM_TOPICS = Short.parseShort(ApplicationSetup.getProperty(
			"TopicBasedTermSelector.NUM_TOPICS", "5"));
	
	private String bm25_b = ApplicationSetup.getProperty(
			"bm25.b", "0.35");
	

	// static double DOC_TOPIC_PRIOR = 0.01;
	static double TOPIC_WORD_PRIOR = 0.01;
	static double DOC_TOPIC_PRIOR = 2d / NUM_TOPICS;
	static int numSamples = 30;
	static int burnin = 30;
	static int sampleLag = 10;

	static int BURNIN_EPOCHS = 10;
	static int SAMPLE_LAG = 30;
	static int NUM_SAMPLES = 30;


	protected int EXPANSION_MIN_DOCUMENTS;
	float dscores[];
	Map<String, ExpansionTerm> tmpTermMap = null;

	TObjectIntHashMap<String> dfMap = null;

	public TopicBasedTermSelector() throws IOException {
		super();
		TopicBasedTermSelector.setMcc(new MemcachedClient(
				new ConnectionFactoryBuilder().setDaemon(true).setFailureMode(FailureMode.Retry).build(),
				AddrUtil.getAddresses(address)));
		this.setMetaInfo("normalize.weights", "true");
		this.EXPANSION_MIN_DOCUMENTS = Integer.parseInt(ApplicationSetup
				.getProperty("expansion.mindocuments", "2"));
		}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.lucene.postProcess.termselector.TermSelector#assignTermWeights
	 * (int[], org.apache.lucene.postProcess.QueryExpansionModel)
	 */
	@Override
	public void assignTermWeights(int[] docids, float scores[],
			QueryExpansionModel QEModel) {
		dscores = new float[scores.length];
		System.arraycopy(scores, 0, dscores, 0, scores.length);

		if (LanguageModel) {
			indriNorm(dscores);
		}
		Normalizer.norm2(dscores);

		String[][] termCache = null;
		int[][] termFreq = null;
		termMap = new HashMap<String, ExpansionTerm>();
		this.feedbackSetLength = 0;
		termCache = new String[docids.length][];
		termFreq = new int[docids.length][];
		dfMap = new TObjectIntHashMap<String>();
		for (int i = 0; i < docids.length; i++) {
			int docid = docids[i];
			TermFreqVector tfv = null;
			try {
				tfv = this.searcher.getIndexReader().getTermFreqVector(docid,
						field);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (tfv == null)
				logger.warn("document " + docid + " not found, field=" + field);
			else {
				String strterms[] = tfv.getTerms();
				int freqs[] = tfv.getTermFrequencies();
				termCache[i] = strterms;
				termFreq[i] = freqs;
			}
		}

		// //////////LDA clustering/////////////////////
		MapSymbolTable SYMBOL_TABLE = new MapSymbolTable();
		int[][] DOC_WORDS = new int[docids.length][];
		int querytermid[] = new int[this.originalQueryTermidSet.size()];
		int pos = 0;
		int backids[] = new int[0];
		if (expTag) {
			int reallExp = Math.min(expDoc, docids.length);
			int expDocs[] = new int[reallExp];
			float expscores[] = new float[reallExp];
			System.arraycopy(docids, 0, expDocs, 0, reallExp);
			System.arraycopy(scores, 0, expscores, 0, reallExp);
			TermSelector selector = TermSelector.getTermSelector(
					"RocchioTermSelector", this.searcher);
			selector.setField(field);
			selector.setMetaInfo("normalize.weights", "false");
			selector.setOriginalQueryTerms(originalQueryTermidSet);
			selector.assignTermWeights(expDocs, expscores, QEModel);
			this.tmpTermMap = selector.termMap;

			HashMap<String, ExpansionTerm> map = selector
					.getMostWeightedTermsInHashMap(expNum);
			assert map.size() <= expNum;
			Set<String> keyset = new HashSet<String>(map.keySet());
			keyset.addAll(this.originalQueryTermidSet);
			querytermid = new int[keyset.size()];
			for (String term : keyset) {
				int id = SYMBOL_TABLE.getOrAddSymbol(term);
				querytermid[pos++] = id;
			}

			// //////////////////////////////////////
			// ExpansionTerm exTerms[] =
			// selector.getMostWeightedTerms(selector.getNumberOfUniqueTerms());
			// TIntHashSet set = new TIntHashSet();
			// for(int i = exTerms.length-1; i > 0 && i > exTerms.length-1
			// -expNum; i--){
			// int id = SYMBOL_TABLE.getOrAddSymbol(exTerms[i].getTerm());
			// set.add(id);
			// }
			// backids = set.toArray();
			// /////////////////////////////////////

		} else {
			for (String term : this.originalQueryTermidSet) {
				int id = SYMBOL_TABLE.getOrAddSymbol(term);
				querytermid[pos++] = id;
			}
		}

		for (int i = 0; i < docids.length; i++) {
			int len = Arrays.sum(termFreq[i]);
			DOC_WORDS[i] = new int[len];
			pos = 0;
			for (int j = 0; j < termCache[i].length; j++) {
				String term = termCache[i][j];
				dfMap.adjustOrPutValue(term, 1, 1);
				int id = SYMBOL_TABLE.getOrAddSymbol(term);
				for (int k = 0; k < termFreq[i][j]; k++) {
					DOC_WORDS[i][pos++] = id;
				}
			}
			assert len == pos;
		}
		for (int[] words : DOC_WORDS)
			Arrays.permute(words, RANDOM);
		// LdaReportingHandler handler = new LdaReportingHandler(SYMBOL_TABLE);

		// get a co-occurrence lookup map. ////////////
		MapSymbolTable coTable = SYMBOL_TABLE.clone();
		TermAssociation tAss = null;
		if (associationTag) {
			tAss = TermAssociation.built(this.searcher, this.topDoc, coTable,
					this.field, winSize);
		}

		// ////////////////////////////////////////////
		LatentDirichletAllocation.GibbsSample sample = null;
		if(!useAdapTopicNumber)
			 sample = LatentDirichletAllocation.gibbsSampler(
					 DOC_WORDS, NUM_TOPICS, DOC_TOPIC_PRIOR,
					 TOPIC_WORD_PRIOR, BURNIN_EPOCHS, SAMPLE_LAG,
					 NUM_SAMPLES, RANDOM, querytermid, backids, null);
		else {
			// set a large number
			//if((!useMemCacheDB) || (this.mcc == null))
			String key = "Topic" + topicId + "b"+ bm25_b + "expDoc" + expDoc;
			if((useMemCacheDB) && (TopicBasedTermSelector.getMcc() != null)){
				if (TopicBasedTermSelector.getMcc().get(key) != null){
					Short optTopicNum = (Short)TopicBasedTermSelector.getMcc().get(key);
					logger.info(String.format("Find optimal topic number %d for %s", optTopicNum.shortValue(), key));
					sample = LatentDirichletAllocation.gibbsSampler(
							 DOC_WORDS, optTopicNum.shortValue(), DOC_TOPIC_PRIOR,
							 TOPIC_WORD_PRIOR, BURNIN_EPOCHS, SAMPLE_LAG,
							 NUM_SAMPLES, RANDOM, querytermid, backids, null);
					NUM_TOPICS = optTopicNum.shortValue();
				}else{
					double pre_perplexity = 99999999;
					double perplexity = 9999999;
					double THRESHOLD = 0.005d;
					short topicNumber = 5;
					System.out.println("Start to obtain optimal topic number");
					while (Math.abs((pre_perplexity - perplexity) / pre_perplexity) > THRESHOLD) {
						pre_perplexity = perplexity;
						topicNumber += 5;
						DOC_TOPIC_PRIOR = 2d / topicNumber;
						sample = LatentDirichletAllocation.gibbsSampler(DOC_WORDS,
								topicNumber, DOC_TOPIC_PRIOR, TOPIC_WORD_PRIOR,
								BURNIN_EPOCHS, SAMPLE_LAG, NUM_SAMPLES, RANDOM,
								querytermid, backids, null);
						perplexity = this.getPerplexity(sample);

					}
					logger.info("Optimal topic number for " + expDoc + " docs is : "
							+ topicNumber);
					TopicBasedTermSelector.getMcc().add(key, 86400, topicNumber);
					//remember to update NUM_TOPICS for theta
					NUM_TOPICS = topicNumber;
				}
			}else{
				double pre_perplexity = 99999999;
				double perplexity = 9999999;
				double THRESHOLD = 0.005d;
				short topicNumber = 5;
				System.out.println("Start to obtain optimal topic number");
				while (Math.abs((pre_perplexity - perplexity) / pre_perplexity) > THRESHOLD) {
					pre_perplexity = perplexity;
					topicNumber += 5;
					DOC_TOPIC_PRIOR = 2d / topicNumber;
					sample = LatentDirichletAllocation.gibbsSampler(DOC_WORDS,
							topicNumber, DOC_TOPIC_PRIOR, TOPIC_WORD_PRIOR,
							BURNIN_EPOCHS, SAMPLE_LAG, NUM_SAMPLES, RANDOM,
							querytermid, backids, null);
					perplexity = this.getPerplexity(sample);

				}
				logger.info("Optimal topic number for " + expDoc + " docs is : "
						+ topicNumber);
				TopicBasedTermSelector.getMcc().add(key, 86400, topicNumber);
				//remember to update NUM_TOPICS for theta
				NUM_TOPICS = topicNumber;
			}
			
		}
				
//		for(int epoch = 1; epoch <=BURNIN_EPOCHS; epoch = epoch + 1){
//			sample = LatentDirichletAllocation.gibbsSampler(DOC_WORDS,
//			topicNumber, DOC_TOPIC_PRIOR, TOPIC_WORD_PRIOR,
//			epoch, SAMPLE_LAG, NUM_SAMPLES, RANDOM,
//			querytermid, backids, null);
//			perplexity = this.getPerplexity(sample);
//			logger.warn("Perplexity when epoch is "+ epoch +" for " + expDoc + " docs is : " + perplexity);
//		}

		LatentDirichletAllocation lda = sample.lda();
		short[][] qsamples = lda.sampleTopics(querytermid, numSamples, burnin,
				sampleLag, RANDOM);

		float theta[] = new float[NUM_TOPICS];
		java.util.Arrays.fill(theta, 0);
		for (int i = 0; i < qsamples.length; i++) {
			for (int j = 0; j < qsamples[i].length; j++) {
				theta[qsamples[i][j]]++;
			}
		}
		float total = querytermid.length * numSamples;
		for (int i = 0; i < theta.length; i++) {
			theta[i] = theta[i] / total;
		}

		selectTerm(SYMBOL_TABLE, sample, QEModel,
				theta, querytermid, lda, tAss);
		
		TopicBasedTermSelector.getMcc().shutdown();
	}

	  float[] sampleTheta(int numTopics, LatentDirichletAllocation lda,
			int[] words) {	
		short[][] qsamples = lda.sampleTopics(words, numSamples, burnin,
				sampleLag, RANDOM);
		
		float theta[] = new float[numTopics];
		java.util.Arrays.fill(theta, 0);

		for (int i = 0; i < qsamples.length; i++) {
			for (int j = 0; j < qsamples[i].length; j++) {
				theta[qsamples[i][j]]++;
			}
		}
		float total = words.length * numSamples;
		for (int i = 0; i < theta.length; i++) {
			theta[i] = theta[i] / total;
		}
		return theta;
	}

	float[][] sampleThetas(int times, int numTopics,
			LatentDirichletAllocation lda, int[] words) {
		float retValue[][] = new float[times][];
		for (int i = 0; i < times; i++) {
			retValue[i] = sampleTheta(numTopics, lda, words);
		}
		return retValue;
	}

	float[] sampleThetasAver(int times, int numTopics,
			LatentDirichletAllocation lda, int[] words) {
		float[][] aver = sampleThetas(times, numTopics, lda, words);
		float retV[] = new float[numTopics];
		for (int i = 0; i < aver.length; i++) {
			for (int j = 0; j < aver[0].length; j++) {
				retV[j] = aver[i][j];
			}
		}
		for (int i = 0; i < numTopics; i++) {
			retV[i] = retV[i] / numTopics;
		}
		return retV;
	}

	@SuppressWarnings("unused")
    private ExpansionTerm[] selectTerm(SymbolTable SYMBOL_TABLE,
			GibbsSample sample, QueryExpansionModel QEModel, float theta[],
			int querytermid[], LatentDirichletAllocation lda,
			TermAssociation tAss) {
		ExpansionTerm[] allTerms = new ExpansionTerm[SYMBOL_TABLE.numSymbols()];

		int index[] = Arrays.indexSort(theta);
		double[][] docTopicProbs = new double[sample.numDocuments()][sample.numTopics()];

		if (logger.isDebugEnabled()) {
			for (int i = 0; i < index.length; i++) {
				float prob = 0;
				for (int j = 0; j < querytermid.length; j++) {
					prob += Idf.log(sample.topicWordProb(index[i],
							querytermid[j]));
				}
				if (logger.isDebugEnabled())
					logger.debug("topic: " + index[i] + " - " + theta[index[i]]
							+ ", topicCount: " + sample.topicCount(index[i])
							+ ", prob: " + prob);
			}
			StringBuilder buf = new StringBuilder();
			for (int i = 0, n = sample.numDocuments(); i < n; i++) {
				buf.append(i + ":\t");
				for (int j = 0; j < sample.numTopics(); j++) {
					buf.append(Rounding.round(sample.documentTopicProb(i, j), 5)
							+ "\t");
					docTopicProbs[i][j] =  Rounding.round(sample.documentTopicProb(i, j), 5);
				}
				buf.append("\n");
			}
			if (logger.isDebugEnabled())
				logger.debug("doc topic distribution\n" + buf.toString());
		}

		final int len = allTerms.length;
		int maxTopic = index[index.length - 1];
		if (logger.isDebugEnabled())
			logger.debug("max topic: " + maxTopic);
		
		 Iterator<String> it = originalQueryTermidSet.iterator();
		 String[] qterms = new String[originalQueryTermidSet.size()];
         int qCount = 0;
         while (it.hasNext()) {
                 qterms[qCount] = (String) it.next();
                 qCount++;
         }
         
         // get query terms topic probabilities
         
         double qtermTopicProb[][] = new double[qterms.length][index.length];
         for(int m = 0; m < qterms.length; m++){
             int id = SYMBOL_TABLE.symbolToID(qterms[m]);
             for (int k = 0; k < index.length; k++)
                 qtermTopicProb[m][k] = (float) sample.topicWordProb(index[k], id);
         }
     
        int maxQueryTermId = querytermid[querytermid.length - 1];
		if (strategy == 1) { // take advantage of the topic with the highest
								// prob
			float totalweight = 0;
			int feedbackNum = sample.numDocuments();
			for (int i = 0; i < len; i++) {
				String term = SYMBOL_TABLE.idToSymbol(i);
				TermsCache.Item item = getItem(term);
				float TF = item.ctf;
				float DF = item.df;
				float weight = 0;

				weight = 0;
				// use probability in top 1 topic as original weight in one
				// feedback doc, add all the score up and divide by the doc num
				// then rank
				for (int d = 0; d < feedbackNum; d++) {
					double docProb = sample.docWordCount(d, i)
							/ (float) sample.documentLength(d);
					if (docProb == 0) {
						continue;
					}
					double topicProb = sample.topicWordProb(maxTopic, i);
					double onedocWeight = docProb;
					// one doc weight is the original doc weight smoothed by the
					// topic prob
					QEModel.setTotalDocumentLength(1);
					weight += QEModel.score((float) onedocWeight, TF, DF);
				}
				weight /= feedbackNum;
				//nerv the performance of basic topic method
				double topicProb = sample.topicWordProb(maxTopic, i);
					weight = (float) ((1 - beta) * weight + beta
	                        * topicProb);
	                System.out.println("topic weight is " + topicProb + 
	                        "doc weight " + weight);
				
				if (dfMap.get(term) < EXPANSION_MIN_DOCUMENTS) {
					weight = 0;
				}
				allTerms[i] = new ExpansionTerm(term, 0);
				allTerms[i].setWeightExpansion(weight);
				this.termMap.put(term, allTerms[i]);
				totalweight += weight;
			}

			java.util.Arrays.sort(allTerms);
			// determine double normalizing factor
			float normaliser = allTerms[0].getWeightExpansion();
			for (ExpansionTerm term : allTerms) {
				if (normaliser != 0) {
					term.setWeightExpansion(term.getWeightExpansion()
							/ totalweight);
				}
			}
		} else if (strategy == 2) { // add by Jun Miao
			// take advantage of the topic with the highest
			// prob and rank terms by their deviation in topics
			float totalweight = 0;
			int feedbackNum = sample.numDocuments();
			for (int i = 0; i < len; i++) {
				String term = SYMBOL_TABLE.idToSymbol(i);
				TermsCache.Item item = getItem(term);
				float TF = item.ctf;
				float DF = item.df;
				float weight = 0;

				weight = 0;
				
				double []topicProb = new double[index.length];
				for (int k = 0; k < index.length; k++)
					topicProb[k] = sample.topicWordProb(index[k], i);
				
				StandardDeviation sd =  new StandardDeviation();
				double termDeviation = sd.evaluate(topicProb);
				
				double topicWeight = (float) Math.sqrt(termDeviation * sample.topicWordProb(maxTopic, i));
				
				
				for (int d = 0; d < feedbackNum; d++) {
					double docProb = sample.docWordCount(d, i)
							/ (float) sample.documentLength(d);
					if (docProb == 0) {
						continue;
					}
					double onedocWeight = docProb;
					
					// one doc weight is the original doc weight smoothed by the
					// topic prob
					QEModel.setTotalDocumentLength(1);
					weight += QEModel.score((float) onedocWeight, TF, DF);
				}
				weight /= feedbackNum;
				
				if (this.tmpTermMap.get(term) != null) 
				    logger.debug(String.format("term %s has a rocchio weight %f", 
				            term, this.tmpTermMap.get(term).getWeightExpansion()));
				
				if(expTag){
				    if(this.tmpTermMap.get(term) != null)
				        weight = (float) ((1 - beta) * this.tmpTermMap.get(term).getWeightExpansion() + beta *topicWeight);
				    else
				        weight = (float) ((1 - beta) * weight + beta * topicWeight);;
				}
				
				if (dfMap.get(term) < EXPANSION_MIN_DOCUMENTS) {
					weight = 0;
				}
				allTerms[i] = new ExpansionTerm(term, 0);
				allTerms[i].setWeightExpansion(weight);
				this.termMap.put(term, allTerms[i]);
				totalweight += weight;
			}

			java.util.Arrays.sort(allTerms);
			// determine double normalizing factor
			float normaliser = allTerms[0].getWeightExpansion();
			for (ExpansionTerm term : allTerms) {
				if (normaliser != 0) {
					term.setWeightExpansion(term.getWeightExpansion()
							/ totalweight);
				}
			}

		} else if (strategy == 5) { // add by Jun Miao
			// compare the probability distribution of query terms and 
			//candidate feedback terms.

			float totalweight = 0;
			int feedbackNum = sample.numDocuments();
			String key = "Topic" + topicId + "b"+ bm25_b + "expDoc" + expDoc;
			short topicNum  = 0;
			if(TopicBasedTermSelector.mcc.get(key) != null)
				 topicNum = ((Short)TopicBasedTermSelector.mcc.get(key)).shortValue();
			
            for (int i = 0; i < len; i++) {
            	 String term = SYMBOL_TABLE.idToSymbol(i);
                 TermsCache.Item item = getItem(term);
                 float TF = item.ctf;
                 float DF = item.df;
                 float weight = 0;
                 double topicWeight = 0;
                 
                 String topicWeightKey = key +"topicNum" + topicNum + term; 
                 
                 //get the topic distribution of current term
                 if ((TopicBasedTermSelector.mcc != null) && (topicNum != 0) && 
                		 (useMemCacheDB) && (TopicBasedTermSelector.mcc.get(topicWeightKey) != null))
                	 topicWeight = (Double)TopicBasedTermSelector.mcc.get(topicWeightKey);
                 else{
					double[] topicProb = new double[index.length];
					for (int k = 0; k < index.length; k++)
						topicProb[k] = (float) sample
								.topicWordProb(index[k], i);

					for (int m = 0; m < qterms.length; m++) {
						topicWeight += MetricsUtils.cosine_similarity(
								topicProb, qtermTopicProb[m]);
					}

					topicWeight /= qterms.length;
					TopicBasedTermSelector.mcc.set(topicWeightKey, 86400, topicWeight);
				}
                 
//                 for (int d = 0; d < feedbackNum; d++) {
//                     double docProb = sample.docWordCount(d, i)
//                                     / (float) sample.documentLength(d);
//                     if (docProb == 0) {
//                             continue;
//                     }                                   
//                     double docWeight = QEModel.score((float) docProb, TF, DF);
//                     
//                     //topicWeight and doc weight are not on the same level
////                     double onedocWeight = (1 - beta) * docWeight + beta
////                                     * topicWeight;
//                     
//                    // double onedocWeight = (1 + beta * topicWeight) * docWeight;
//                     double onedocWeight = beta * (1 +  topicWeight)* sample.topicWordProb(maxTopic, i)
//                             + (1 - beta) * docWeight;
//                     // one doc weight is the original doc weight smoothed by the
//                     // topic prob
//                     QEModel.setTotalDocumentLength(1);
//                     //weight += QEModel.score((float) onedocWeight, TF, DF);
//                     weight += onedocWeight;
//             }

				if (this.tmpTermMap.get(term) != null) 
				    logger.debug(String.format("term %s has a rocchio weight %f", 
				            term, this.tmpTermMap.get(term).getWeightExpansion()));
				
				if(expTag){
				    if(this.tmpTermMap.get(term) != null)
				        weight = (float) ((1 - beta) * this.tmpTermMap.get(term).getWeightExpansion() + beta *topicWeight);
				    else
				        weight = (float) ((1 - beta) * weight + beta * topicWeight);;
				}
            	
            	if (dfMap.get(term) < EXPANSION_MIN_DOCUMENTS) {
            		weight = 0;
            	}
            	allTerms[i] = new ExpansionTerm(term, 0);
            	allTerms[i].setWeightExpansion(weight);
            	this.termMap.put(term, allTerms[i]);
            	totalweight += weight;
            }

            java.util.Arrays.sort(allTerms);
            // determine double normalizing factor
            float normaliser = allTerms[0].getWeightExpansion();
            for (ExpansionTerm term : allTerms) {
            	if (normaliser != 0) {
            		term.setWeightExpansion(term.getWeightExpansion()
            				/ totalweight);
            	}
            }

		}else if (strategy == 6) { // add by Jun Miao
		//Combine the dev and similarity method	

			float totalweight = 0;
			int feedbackNum = sample.numDocuments();
			String key = "Topic" + topicId + "b"+ bm25_b + "expDoc" + expDoc;
			short topicNum  = 0;
			if(TopicBasedTermSelector.mcc.get(key) != null)
				 topicNum = ((Short)TopicBasedTermSelector.mcc.get(key)).shortValue();
			
            for (int i = 0; i < len; i++) {
            	 String term = SYMBOL_TABLE.idToSymbol(i);
                 TermsCache.Item item = getItem(term);
                 float TF = item.ctf;
                 float DF = item.df;
                 float weight = 0;
                 double topicSimWeight = 0;
                 
                 String topicWeightKey = key +"topicNum" + topicNum + term; 
                 
                 //get the topic distribution of current term
                 if ((TopicBasedTermSelector.mcc != null) && (topicNum != 0) && 
                		 (useMemCacheDB) && (TopicBasedTermSelector.mcc.get(topicWeightKey) != null))
                	 topicSimWeight = (Double)TopicBasedTermSelector.mcc.get(topicWeightKey);
                 else{
					double[] topicProb = new double[index.length];
					for (int k = 0; k < index.length; k++)
						topicProb[k] = (float) sample
								.topicWordProb(index[k], i);

					for (int m = 0; m < qterms.length; m++) {
						topicSimWeight += MetricsUtils.cosine_similarity(
								topicProb, qtermTopicProb[m]);
					}

					topicSimWeight /= qterms.length;
					TopicBasedTermSelector.mcc.set(topicWeightKey, 86400, topicSimWeight);
				}

				double[] topicProb = new double[index.length];
				for (int k = 0; k < index.length; k++)
					topicProb[k] = sample.topicWordProb(index[k], i);

				StandardDeviation sd = new StandardDeviation();
				double termDeviation = sd.evaluate(topicProb);

				double topicDevWeight = (float) Math.sqrt(termDeviation
						* sample.topicWordProb(maxTopic, i));

				if (this.tmpTermMap.get(term) != null) 
				    logger.debug(String.format("term %s has a rocchio weight %f", 
				            term, this.tmpTermMap.get(term).getWeightExpansion()));
				
				if(expTag){
				    if(this.tmpTermMap.get(term) != null)
				        weight = (float) ((1 + topicSimWeight/4)*((1 - beta) * this.tmpTermMap.get(term).getWeightExpansion() +
				        		beta *topicDevWeight));
				    else
				        weight = (float) ((1 + topicSimWeight/4)* ((1 - beta) * weight + beta * topicDevWeight));
				}
            	
            	if (dfMap.get(term) < EXPANSION_MIN_DOCUMENTS) {
            		weight = 0;
            	}
            	allTerms[i] = new ExpansionTerm(term, 0);
            	allTerms[i].setWeightExpansion(weight);
            	this.termMap.put(term, allTerms[i]);
            	totalweight += weight;
            }

            java.util.Arrays.sort(allTerms);
            // determine double normalizing factor
            float normaliser = allTerms[0].getWeightExpansion();
            for (ExpansionTerm term : allTerms) {
            	if (normaliser != 0) {
            		term.setWeightExpansion(term.getWeightExpansion()
            				/ totalweight);
            	}
            }

		}else if(strategy == 9){
		    // Used for testing and printing useful information
		    float totalweight = 0;
            int feedbackNum = sample.numDocuments();
            TermScores [] termScores = new TermScores[len];
            for (int i = 0; i < len; i++) {
                String term = SYMBOL_TABLE.idToSymbol(i);
                termScores[i] = new TermScores(term);
                TermsCache.Item item = getItem(term);
                float TF = item.ctf;
                float DF = item.df;
                float weight = 0;

                weight = 0;
                // use probability in top 1 topic as original weight in one
                // feedback doc, add all the score up and divide by the doc num
                // then rank
                for (int d = 0; d < feedbackNum; d++) {
                    double docProb = sample.docWordCount(d, i)
                            / (float) sample.documentLength(d);
                    if (docProb == 0) {
                        continue;
                    }
                    QEModel.setTotalDocumentLength(1);
                    weight += QEModel.score((float) docProb, TF, DF);
                }
                weight /= feedbackNum;
                termScores[i].setDocScore(weight);
                double topicProb = sample.topicWordProb(maxTopic, i);
                termScores[i].setTopicScore(topicProb);
                
                double [] topicPro = new double[index.length];
                for (int k = 0; k < index.length; k++)
                    topicPro[k] = sample.topicWordProb(index[k], i);
                    
                StandardDeviation sd =  new StandardDeviation();
                double termDeviation = sd.evaluate(topicPro);
                termScores[i].setDevScore(termDeviation);
                
                double similarity = 0d;
                for(int m = 0; m < qterms.length; m++){
                    similarity += MetricsUtils.cosine_similarity(topicPro, qtermTopicProb[m]);
                }

                similarity /= qterms.length;
                termScores[i].setsimScore(similarity);
                
                
                
                
                
                if (dfMap.get(term) < EXPANSION_MIN_DOCUMENTS) {
                    weight = 0;
                }
                allTerms[i] = new ExpansionTerm(term, 0);
                allTerms[i].setWeightExpansion(weight);
                this.termMap.put(term, allTerms[i]);
                totalweight += weight;
            }
            
            

            java.util.Arrays.sort(allTerms);
            // determine double normalizing factor
            float normaliser = allTerms[0].getWeightExpansion();
            for (ExpansionTerm term : allTerms) {
                if (normaliser != 0) {
                    term.setWeightExpansion(term.getWeightExpansion()
                            / totalweight);
                }
            }
            
            System.out.println("Query is " + java.util.Arrays.toString(qterms));
            java.util.Arrays.sort(termScores, TermScores.TermDocScoreComparator);
            for (int count = 0; (count < 50 && count < len); count++)
                System.out.println(String.format("Doc Score of %d th term %s is %f ",
                        count, termScores[count].getTerm(), termScores[count].getDocScore()));
            
            System.out.println();
            System.out.println();
            java.util.Arrays.sort(termScores, TermScores.TermTopicScoreComparator);            
            for (int count = 0; (count < 50 && count < len); count++)
                System.out.println(String.format("Topic Score of %d th term %s is %f ",
                        count, termScores[count].getTerm(), termScores[count].getTopicScore()));
            
         
            System.out.println();
            System.out.println();
            java.util.Arrays.sort(termScores, TermScores.TermSimScoreComparator);
            for (int count = 0; (count < 50 && count < len); count++)
                System.out.println(String.format("Sim Score of %d th term %s is %f ",
                        count, termScores[count].getTerm(), termScores[count].getsimScore()));
                        
            System.out.println();
            System.out.println();            
            java.util.Arrays.sort(termScores, TermScores.TermDevScoreComparator);
            for (int count = 0; (count < 50 && count < len); count++)
                System.out.println(String.format("Dev Score of %d th term %s is %f ",
                        count, termScores[count].getTerm(), termScores[count].getDevScore()));
            
		    
		}else if (strategy >9){
            // term weight based on feedback quality
		    System.out.println("I am in strategy " + strategy + "\n");
	        double []feedDocScores = getFeedDocScores(strategy, sample.numDocuments(), 
	                dscores, docTopicProbs, withOrgScore, beta);
	        
	        for (int i = 0; i < feedDocScores.length; i++)
	            System.out.println("the score of feedback doc " + i + " is " + feedDocScores[i] + "\n");
	        
            float totalweight = 0;
            int feedbackNum = sample.numDocuments();
            for (int i = 0; i < len; i++) {
                String term = SYMBOL_TABLE.idToSymbol(i);
                TermsCache.Item item = getItem(term);
                float TF = item.ctf;
                float DF = item.df;
                float weight = 0;

                weight = 0;
                // use probability in top 1 topic as original weight in one
                // feedback doc, add all the score up and divide by the doc num
                // then rank
                for (int d = 0; d < feedbackNum; d++) {
                    double docProb = sample.docWordCount(d, i)
                            / (float) sample.documentLength(d);
                    if (docProb == 0) {
                        continue;
                    }
                    double onedocWeight = docProb * feedDocScores[d]; //Multiple the term prob by the doc score

                    QEModel.setTotalDocumentLength(1);
                    weight += QEModel.score((float) onedocWeight, TF, DF);
                }
                weight /= feedbackNum;
                if (dfMap.get(term) < EXPANSION_MIN_DOCUMENTS) {
                    weight = 0;
                }
                allTerms[i] = new ExpansionTerm(term, 0);
                allTerms[i].setWeightExpansion(weight);
                this.termMap.put(term, allTerms[i]);
                totalweight += weight;
            }

            java.util.Arrays.sort(allTerms);
            // determine double normalizing factor
            float normaliser = allTerms[0].getWeightExpansion();
            for (ExpansionTerm term : allTerms) {
                if (normaliser != 0) {
                    term.setWeightExpansion(term.getWeightExpansion()
                            / totalweight);
                }
            }

		    
		    
		}
		return allTerms;
	}

	static String dmu = ApplicationSetup.getProperty("dlm.mu", "500");
	static float mu = Integer.parseInt(ApplicationSetup.getProperty(
			"topicSL.mu", dmu));

	// float numOfTokens = this.searcher.getNumTokens(field);
	public float score(float tf, float docLength, float termFrequency,
			float numberOfTokens) {
		float pc = termFrequency / numberOfTokens;
		return (tf + mu * pc) / (docLength + mu);
	}

	private void indriNorm(float[] pQ) {

		float K = pQ[0]; // first is max
		float sum = 0;
		for (int i = 0; i < pQ.length; i++) {
			pQ[i] = Idf.exp(K + pQ[i]);
			sum += pQ[i];
		}
		for (int i = 0; i < pQ.length; i++) {
			pQ[i] /= sum;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.lucene.postProcess.termselector.TermSelector#getInfo()
	 */
	@Override
	public String getInfo() {
		if(useAdapTopicNumber)
			return "TopicSel_s=" + strategy + "t=" + "Adap" + "beta=" + beta
				+ "expTag=" + expTag;
		else
			return "TopicSel_s=" + strategy + "t=" + NUM_TOPICS + "beta=" + beta
					+ "expTag=" + expTag;
	}
	
	private double[] getFeedDocScores(int strategy, int feedDocNum, float[] orgDocScores, double[][] docTopicProbs, 
	        boolean withOrgScore, float docScoreAlpha){
	    if (orgDocScores.length == 0) 
	        return null;
	    double[] feedbackDocScores = new double[feedDocNum];
	    java.util.Arrays.fill(feedbackDocScores, 1);
	    int topicNum = docTopicProbs[0].length;

	    if ((feedDocNum <= 3) || (strategy > 13) || (strategy < 10))
	        return feedbackDocScores;
	    
	    switch(strategy){
	        case 10://doc similarity score
	            for (int i = 3; i< feedDocNum; i++){
	                for(int j = 0; j < 3; j++)
	                    feedbackDocScores[i] += MetricsUtils.getCosineSimilarity(docTopicProbs[i], docTopicProbs[j]);
	                
	                feedbackDocScores[i] = (1 + (feedbackDocScores[i] - 1)/3)/2; //normalized to range 0-1
	            }
	            break;
	        case 11: //Norm2
	            for (int i = 3; i< feedDocNum; i++){
                    for(int j = 0; j < 3; j++)
                        feedbackDocScores[i] += MetricsUtils.distL2(docTopicProbs[i], docTopicProbs[j]);
                    
                    feedbackDocScores[i] = feedbackDocScores[i] /(3*topicNum); //normalized to range 0-1
                }
                break;
	        case 12:// topic entropy. Smaller, better
	            for (int i = 0; i< feedDocNum; i++){
	                   for(int j = 0; j< topicNum; j++)
                        feedbackDocScores[i] += -1 * docTopicProbs[i][j] * Math.log(docTopicProbs[i][j]);
	            
	            for (int k = 0; k< feedDocNum; k++)
	                feedbackDocScores[k] = 1- (feedbackDocScores[k]/Math.log(topicNum));
                }
                break;
	        case 13: //Jenson-Shannon divergence from avg distribution
	            double[] avgTopicDist = new double[topicNum];
	            for (int i = 0; i< feedDocNum; i++)
	                for(int j = 0; j< topicNum; j++)
	                    avgTopicDist[j] += docTopicProbs[i][j];
	            
	            for (int i = 0; i< feedDocNum; i++)
	                avgTopicDist[i] /= feedDocNum;
	            
	            for (int i = 0; i< feedDocNum; i++)
                    feedbackDocScores[i] = MetricsUtils.jsd(docTopicProbs[i], avgTopicDist);

                break;
                
             default: break;
	        
	    }
	    
	    if (withOrgScore)
	        for (int i = 0; i< feedDocNum; i++)
	            feedbackDocScores[i] = feedbackDocScores[i] * (1 - docScoreAlpha) 
	            + docScoreAlpha * orgDocScores[i];
	    
	    return feedbackDocScores;
	}
	
	public double getPerplexity(LatentDirichletAllocation.GibbsSample sample ){
        int docNum = sample.numDocuments();
        int topicNum = sample.numTopics();
        int termNum = sample.numWords();
        int  wordNum = sample.numTokens();
        
        double [][] docTopicProb = new double[docNum][topicNum];
        double [][] topicTermProb = new double[topicNum][termNum];
        for (int doc = 0; doc < docNum; doc++)
            for (int topic = 0; topic < topicNum; topic++)
                docTopicProb[doc][topic] = sample.documentTopicProb(doc, topic);
        
        for(int topic = 0; topic < topicNum; topic++)
            for (int term = 0; term < termNum; term++)
               topicTermProb[topic][term] = sample.topicWordProb(topic, term);
        
        double [][] pword = new double[docNum][termNum];  
		for (int doc = 0; doc < docNum; doc++)
			for (int term = 0; term < termNum; term++)
				for (int topic = 0; topic < topicNum; topic++)
					pword[doc][term] += docTopicProb[doc][topic]
							* topicTermProb[topic][term];
		
		double [] logDocProb = new double[docNum]; 
		
		for (int doc = 0; doc < docNum; doc++)
			for (int term = 0; term < termNum; term++)
				logDocProb[doc] += sample.docWordCount(doc, term) * Math.log(pword[doc][term]);
				
				
         double sum = 0.0d;
         for  (int doc = 0; doc < docNum; doc++)
        	 sum += logDocProb[doc]; 
        	 
         double perplexity = Math.exp(-1 * sum/wordNum);
         logger.debug(String.format("Topic number is %d and perplexity is %f", topicNum, perplexity));
         return perplexity;
    }
    
	
    
    

	/**
	 * @param argsqsamples
	 */
	public static void main(String[] args) {
		new StringBuilder();
	}

	@Override
	public void assignTermWeights(String[][] terms, int[][] freqs,
			TermPositionVector[] tfvs, QueryExpansionModel QEModel) {
		// TODO Auto-generated method stub

	}

	private static MemcachedClient getMcc() {
		return mcc;
	}

	public static void setMcc(MemcachedClient mcc) {
		TopicBasedTermSelector.mcc = mcc;
	}

}
