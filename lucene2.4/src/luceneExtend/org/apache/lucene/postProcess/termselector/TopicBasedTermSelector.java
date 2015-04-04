/**
 * 
 */
package org.apache.lucene.postProcess.termselector;

import gnu.trove.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.apache.log4j.Logger;
import org.apache.lucene.MetricsUtils;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
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

public class TopicBasedTermSelector extends TermSelector {
	private static Logger logger = Logger.getLogger(TopicBasedTermSelector.class);
	static boolean LanguageModel = Boolean.parseBoolean(ApplicationSetup
			.getProperty("Lucene.Search.LanguageModel", "false"));
	static int strategy = Integer.parseInt(ApplicationSetup.getProperty(
			"TopicBasedTermSelector.strategy", "3"));
	static boolean expTag = Boolean.parseBoolean(ApplicationSetup.getProperty(
			"TopicBasedTermSelector.expTag", "false"));
	static boolean withOrgScore = Boolean.parseBoolean(ApplicationSetup.getProperty(
	            "TopicBasedTermSelector.withOrgScore", "false"));
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

	static Random RANDOM = new Random(43);
	static short NUM_TOPICS = Short.parseShort(ApplicationSetup.getProperty(
			"TopicBasedTermSelector.NUM_TOPICS", "5"));
	
	static String word2vecDataPath = ApplicationSetup.getProperty(
			"TopicBasedTermSelector.word2vecDataPath", "text8.model.txt");

	// static double DOC_TOPIC_PRIOR = 0.01;
	static double TOPIC_WORD_PRIOR = 0.01;
	static double DOC_TOPIC_PRIOR = 2d / NUM_TOPICS;
	static int numSamples = 30;
	static int burnin = 30;
	static int sampleLag = 10;

	static int BURNIN_EPOCHS = 10;
	static int SAMPLE_LAG = 30;
	static int NUM_SAMPLES = 30;
	//static HashMap<String, float[]> vectorOfTerms = new HashMap<String, float[]>();
	static HashMap<String, float[]> vectorOfTerms = null;
	protected int EXPANSION_MIN_DOCUMENTS;
	float dscores[];

	TObjectIntHashMap<String> dfMap = null;

	public TopicBasedTermSelector() throws IOException {
		super();
		this.setMetaInfo("normalize.weights", "true");
		this.EXPANSION_MIN_DOCUMENTS = Integer.parseInt(ApplicationSetup
				.getProperty("expansion.mindocuments", "2"));
		//read training data of word2vec
		String vectline;
		if (vectorOfTerms != null){
			br = new BufferedReader(new FileReader(word2vecDataPath));
			vectline = br.readLine();
			int vctDimension = Integer.parseInt(vectline.split("\\s+")[1]);
			
			while  ((vectline = br.readLine()) != null){
				//logger.info("Start reading word2vec file  " + this.word2vecDataPath);
				String [] pairs = vectline.split("\\s+");
				String term = pairs[0];
				float [] vector = new float[vctDimension];
				
				for (int k = 0; k < vctDimension; k++)
					vector[k] = Float.parseFloat(pairs[k+1]);
				
				TopicBasedTermSelector.vectorOfTerms.put(term, vector);
				
			}//end of read word2vec file
		}
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
        logger.info("sum of doc weights:" + Arrays.sum(dscores));
        Normalizer.norm_MaxMin_0_1(dscores); //normalized doc scores
        if (logger.isInfoEnabled())
        {
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < dscores.length; i++) {
                buf.append("" + dscores[i] + ", ");
            }
            logger.info("4.doc weights:" + buf.toString());
        }
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
		
		System.out.println("Start LDA...\n");

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

		// LatentDirichletAllocation.GibbsSample sample =
		// LatentDirichletAllocation
		// .gibbsSampler(DOC_WORDS, NUM_TOPICS, DOC_TOPIC_PRIOR,
		// TOPIC_WORD_PRIOR, BURNIN_EPOCHS, SAMPLE_LAG,
		// NUM_SAMPLES, RANDOM, querytermid, backids, null, tAss);
				LatentDirichletAllocation.GibbsSample sample = LatentDirichletAllocation
				.gibbsSampler(DOC_WORDS, NUM_TOPICS, DOC_TOPIC_PRIOR,
						TOPIC_WORD_PRIOR, BURNIN_EPOCHS, SAMPLE_LAG,
						NUM_SAMPLES, RANDOM, querytermid, backids, null);

		LatentDirichletAllocation lda = sample.lda();
				short[][] qsamples = lda.sampleTopics(querytermid, numSamples, burnin,
				sampleLag, RANDOM);
		System.out.println("End LDA...\n");
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

		allTerms = selectTerm(SYMBOL_TABLE, sample, QEModel,
				theta, querytermid, lda, tAss);
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

	private ExpansionTerm[] selectTerm(SymbolTable SYMBOL_TABLE,
			GibbsSample sample, QueryExpansionModel QEModel, float theta[],
			int querytermid[], LatentDirichletAllocation lda,
			TermAssociation tAss) {
		ExpansionTerm[] allTerms = new ExpansionTerm[SYMBOL_TABLE.numSymbols()];

		int index[] = Arrays.indexSort(theta);
		System.out.println("I am in the selectTerm method\n");
		double[][] docTopicProbs = new double[sample.numDocuments()][sample.numTopics()];
		if (true) {
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
					double onedocWeight = (1 - beta) * docProb + beta
							* topicProb;
					// one doc weight is the original doc weight smoothed by the
					// topic prob
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
					double onedocWeight = (1 - beta) * docProb + beta
							* topicWeight;
					// one doc weight is the original doc weight smoothed by the
					// topic prob
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

		} else if (strategy == 3) { // add by Jun Miao
			// take advantage of the topic with the highest
			// prob and rank terms by PMI in the collection

			try {
				float totalweight = 0;
				IndexReader ir = this.searcher.getIndexReader();
				
				Iterator<String> it = originalQueryTermidSet.iterator();
                String[] qterms = new String[originalQueryTermidSet.size()];
                int qCount = 0;
                while (it.hasNext()) {
                    qterms[qCount] = it.next();
                    qCount++;
                }
                int docInColl = ir.numDocs();
				// get query collection probability P(q)
                double[] qCollProb = new double[qterms.length];

                // get the postingList and of query terms
                ArrayList<HashSet<Integer>> qTermPostings = new ArrayList<HashSet<Integer>>();
                for (qCount = 0; qCount < qterms.length; qCount++) {
                    Term qterm = new Term(this.field, qterms[qCount]);
                    TermDocs docIds;
                    docIds = ir.termDocs(qterm);

                    HashSet<Integer> qPostings = new HashSet<Integer>();

                    while (docIds.next())
                        qPostings.add(docIds.doc());

                    qTermPostings.add(qPostings);
                    qCollProb[qCount] = qPostings.size()
                            / (double) docInColl;
                }

                
				for (int i = 0; i < len; i++) {
					String term = SYMBOL_TABLE.idToSymbol(i);
					float weight = 0;
					
					
					// Get the posting list of the current term
					weight = 0;

					Term currTerm = new Term(this.field, term);
					TermDocs currTermDocIds = ir.termDocs(currTerm);
					int currTermPostingNum = 0;
					int[] commonPostingNum = new int[qterms.length];

					while (currTermDocIds.next()) {
						for (int l = 0; l < qterms.length; l++) {
							int id = currTermDocIds.doc();
							if (qTermPostings.get(l).contains(id))
								commonPostingNum[l]++;
						}
						currTermPostingNum++;
					}
					// Get all the P(term), P(term, qterm)
					// Calculate the PMI^2 weight of this term
					double currTermCollProb = (double) currTermPostingNum
							/ docInColl;
					double[] jointProbs = new double[qterms.length];
					for (int l = 0; l < qterms.length; l++) {
						jointProbs[l] = (double) commonPostingNum[l]
								/ docInColl;
						weight += (Math.log(jointProbs[l]
								/ (qCollProb[l] * currTermCollProb)))
								/ (-jointProbs[l]);
					}

					double topicWeight = sample.topicWordProb(maxTopic, i);
					weight /= qterms.length * topicWeight;

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
				System.out.println("What the hell is going on here?\n");
				float normaliser = allTerms[0].getWeightExpansion();
				for (ExpansionTerm term : allTerms) {
					if (normaliser != 0) {
						term.setWeightExpansion(term.getWeightExpansion()
								/ totalweight);
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		} else if (strategy == 4) { // add by Jun Miao
			// take advantage of word2Vec

			float totalweight = 0;
            for (int i = 0; i < len; i++) {
            	String term = SYMBOL_TABLE.idToSymbol(i);
            	float weight = 0;
            	Iterator<String> it = originalQueryTermidSet.iterator();
            	String[] qterms = new String[originalQueryTermidSet.size()];
            	int qCount = 0;
            	while (it.hasNext()) {
            		qterms[qCount] = it.next();
            		qCount++;
            	}

            	//calculate the weight of a feedback term based on it's word2vec
            	//similarity to query terms
            	float [] termVector = TopicBasedTermSelector.vectorOfTerms.get(term);
            	for (int t = 0; t < qterms.length; t++){
            		float [] qtermVector = TopicBasedTermSelector.vectorOfTerms.get(qterms[t]);
            		weight += cosine_similarity(termVector, qtermVector);
            	}
            		weight /= qterms.length;

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

		}else if (strategy == 5) { // add by Jun Miao
			// compare the probability distribution of query terms and 
			//candidate feedback terms.

			float totalweight = 0;
            for (int i = 0; i < len; i++) {
            	String term = SYMBOL_TABLE.idToSymbol(i);
            	float weight = 0;
            	Iterator<String> it = originalQueryTermidSet.iterator();
            	String[] qterms = new String[originalQueryTermidSet.size()];
            	int qCount = 0;
            	while (it.hasNext()) {
            		qterms[qCount] = it.next();
            		qCount++;
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
    private BufferedReader br;
    private ExpansionTerm[] allTerms;

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
	                    feedbackDocScores[i] += getCosineSimilarity(docTopicProbs[i], docTopicProbs[j]);
	                
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
	
	private static double cosine_similarity(float[] vec1, float[] vec2) { 
        double dp = dot_product(vec1,vec2); 
        double magnitudeA = find_magnitude(vec1); 
        double magnitudeB = find_magnitude(vec2); 
        return (dp)/(magnitudeA*magnitudeB); 
    } 
	
	   private static double getCosineSimilarity(double[] vec1, double[] vec2) { 
	        double dp = dot_product(vec1,vec2); 
	        double magnitudeA = find_magnitude(vec1); 
	        double magnitudeB = find_magnitude(vec2); 
	        return (dp)/(magnitudeA*magnitudeB); 
	    } 

    private static double find_magnitude(float[] vec) { 
        double sum_mag=0; 
        for(int i=0;i<vec.length;i++) 
        { 
            sum_mag = sum_mag + vec[i]*vec[i]; 
        } 
        return Math.sqrt(sum_mag); 
    } 
    
    private static double find_magnitude(double[] vec) { 
        double sum_mag=0; 
        for(int i=0;i<vec.length;i++) 
        { 
            sum_mag = sum_mag + vec[i]*vec[i]; 
        } 
        return Math.sqrt(sum_mag); 
    } 

    private static double dot_product(float[] vec1, float[] vec2) { 
        double sum=0; 
        for(int i=0;i<vec1.length;i++) 
        { 
            sum = sum + vec1[i]*vec2[i]; 
        } 
        return sum; 
    } 
    
    private static double dot_product(double[] vec1, double[] vec2) { 
        double sum=0; 
        for(int i=0;i<vec1.length;i++) 
        { 
            sum = sum + vec1[i]*vec2[i]; 
        } 
        return sum; 
    } 

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		StringBuilder buf = new StringBuilder();
	}

	@Override
	public void assignTermWeights(String[][] terms, int[][] freqs,
			TermPositionVector[] tfvs, QueryExpansionModel QEModel) {
		// TODO Auto-generated method stub

	}

}
