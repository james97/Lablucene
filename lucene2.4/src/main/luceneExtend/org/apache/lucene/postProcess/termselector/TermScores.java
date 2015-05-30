package org.apache.lucene.postProcess.termselector;

import java.util.Comparator;

/**
 * TermScores class stores the different scores used in the {@link}TopicBasedTermSelector class
 * */
public class TermScores implements Comparable<TermScores> {
	private String term;
	private double docScore;
	private double topicScore;
	private double simScore;
	private double devScore;

	public TermScores(String term) {
		super();
		this.term = term;
		this.docScore = 0d;
		this.topicScore = 0d;
		this.simScore = 0d;
		this.devScore = 0d;
	}

	/**
	 * @return the term
	 */
	public String getTerm() {
		return term;
	}

	/**
	 * @param term
	 *            the term to set
	 */
	public void setTerm(String term) {
		this.term = term;
	}

	/**
	 * @return the docScore
	 */
	public double getDocScore() {
		return docScore;
	}

	/**
	 * @param docScore
	 *            the docScore to set
	 */
	public void setDocScore(double docScore) {
		this.docScore = docScore;
	}

	/**
	 * @return the topicScore
	 */
	public double getTopicScore() {
		return topicScore;
	}

	/**
	 * @param topicScore
	 *            the topicScore to set
	 */
	public void setTopicScore(double topicScore) {
		this.topicScore = topicScore;
	}

	/**
	 * @return the simScore
	 */
	public double getsimScore() {
		return simScore;
	}

	/**
	 * @param simScore
	 *            the simScore to set
	 */
	public void setsimScore(double simScore) {
		this.simScore = simScore;
	}

	/**
	 * @return the devScore
	 */
	public double getDevScore() {
		return devScore;
	}

	/**
	 * @param devScore
	 *            the devScore to set
	 */
	public void setDevScore(double devScore) {
		this.devScore = devScore;
	}

	// alphabetical order by default
	public int compareTo(TermScores other) {
		return this.term.compareTo(other.term);

	}
	
	public static  Comparator<TermScores> TermDocScoreComparator = new Comparator<TermScores>() {

        public int compare(TermScores TermScores1, TermScores TermScores2) {

            double TermDocScores1 = TermScores1.getDocScore();
            double TermDocScores2 = TermScores2.getDocScore();

            // descending order
            return -1 * Double.compare(TermDocScores1, TermDocScores2);
        }

    };
    
    
    public static Comparator<TermScores> TermTopicScoreComparator = new Comparator<TermScores>() {

        public int compare(TermScores TermScores1, TermScores TermScores2) {

            double TermProbScores1 = TermScores1.getTopicScore();
            double TermProbScores2 = TermScores2.getTopicScore();

            // descending order
            return -1 * Double.compare(TermProbScores1, TermProbScores2);
        }

    };
    
    public static Comparator<TermScores> TermSimScoreComparator = new Comparator<TermScores>() {

        public int compare(TermScores TermScores1, TermScores TermScores2) {

            double TermSimScores1 = TermScores1.getsimScore();
            double TermSimScores2 = TermScores2.getsimScore();

            // descending order
            return -1 * Double.compare(TermSimScores1, TermSimScores2);
        }

    };
    
    public static  Comparator<TermScores> TermDevScoreComparator = new Comparator<TermScores>() {

        public int compare(TermScores TermScores1, TermScores TermScores2) {

            double TermDevScores1 = TermScores1.getDevScore();
            double TermDevScores2 = TermScores2.getDevScore();

            // descending order
            return -1 * Double.compare(TermDevScores1, TermDevScores2);
        }

    };

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("TermScores of %s are Doc: %f  Topic: %f"
				+ " Similarity: %f Dev: %f \n", this.term, this.docScore,
				this.topicScore, this.simScore, this.devScore);
	}

}