/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.lucene.analysis.cn.smart.hhmm;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.cn.smart.CharType;
import org.apache.lucene.analysis.cn.smart.Utility;
import org.apache.lucene.analysis.cn.smart.WordType;
import org.apache.lucene.analysis.cn.smart.hhmm.SegToken;//javadoc @link

/**
 * Finds the optimal segmentation of a sentence into Chinese words
 * <p><font color="#FF0000">
 * WARNING: The status of the analyzers/smartcn <b>analysis.cn.smart</b> package is experimental. 
 * The APIs and file formats introduced here might change in the future and will not be 
 * supported anymore in such a case.</font>
 * </p>
 */
public class HHMMSegmenter {

  private static WordDictionary wordDict = WordDictionary.getInstance();

  /**
   * Create the {@link SegGraph} for a sentence.
   * 
   * @param sentence input sentence, without start and end markers
   * @return {@link SegGraph} corresponding to the input sentence.
   */
  private SegGraph createSegGraph(String sentence) {
    int i = 0, j;
    int length = sentence.length();
    int foundIndex;
    int[] charTypeArray = getCharTypes(sentence);
    StringBuilder wordBuf = new StringBuilder();
    SegToken token;
    int frequency = 0; // the number of times word appears.
    boolean hasFullWidth;
    int wordType;
    char[] charArray;

    SegGraph segGraph = new SegGraph();
    while (i < length) {
      hasFullWidth = false;
      switch (charTypeArray[i]) {
        case CharType.SPACE_LIKE:
          i++;
          break;
        case CharType.HANZI:
          j = i + 1;
          wordBuf.delete(0, wordBuf.length());
          // It doesn't matter if a single Chinese character (Hanzi) can form a phrase or not, 
          // it will store that single Chinese character (Hanzi) in the SegGraph.  Otherwise, it will 
          // cause word division.
          wordBuf.append(sentence.charAt(i));
          charArray = new char[] { sentence.charAt(i) };
          frequency = wordDict.getFrequency(charArray);
          token = new SegToken(charArray, i, j, WordType.CHINESE_WORD,
              frequency);
          segGraph.addToken(token);

          foundIndex = wordDict.getPrefixMatch(charArray);
          while (j <= length && foundIndex != -1) {
            if (wordDict.isEqual(charArray, foundIndex) && charArray.length > 1) {
              // It is the phrase we are looking for; In other words, we have found a phrase SegToken
              // from i to j.  It is not a monosyllabic word (single word).
              frequency = wordDict.getFrequency(charArray);
              token = new SegToken(charArray, i, j, WordType.CHINESE_WORD,
                  frequency);
              segGraph.addToken(token);
            }

            while (j < length && charTypeArray[j] == CharType.SPACE_LIKE)
              j++;

            if (j < length && charTypeArray[j] == CharType.HANZI) {
              wordBuf.append(sentence.charAt(j));
              charArray = new char[wordBuf.length()];
              wordBuf.getChars(0, charArray.length, charArray, 0);
              // idArray has been found (foundWordIndex!=-1) as a prefix before.  
              // Therefore, idArray after it has been lengthened can only appear after foundWordIndex.  
              // So start searching after foundWordIndex.
              foundIndex = wordDict.getPrefixMatch(charArray, foundIndex);
              j++;
            } else {
              break;
            }
          }
          i++;
          break;
        case CharType.FULLWIDTH_LETTER:
          hasFullWidth = true;
        case CharType.LETTER:
          j = i + 1;
          while (j < length
              && (charTypeArray[j] == CharType.LETTER || charTypeArray[j] == CharType.FULLWIDTH_LETTER)) {
            if (charTypeArray[j] == CharType.FULLWIDTH_LETTER)
              hasFullWidth = true;
            j++;
          }
          // Found a Token from i to j. Type is LETTER char string.
          charArray = Utility.STRING_CHAR_ARRAY;
          frequency = wordDict.getFrequency(charArray);
          wordType = hasFullWidth ? WordType.FULLWIDTH_STRING : WordType.STRING;
          token = new SegToken(charArray, i, j, wordType, frequency);
          segGraph.addToken(token);
          i = j;
          break;
        case CharType.FULLWIDTH_DIGIT:
          hasFullWidth = true;
        case CharType.DIGIT:
          j = i + 1;
          while (j < length
              && (charTypeArray[j] == CharType.DIGIT || charTypeArray[j] == CharType.FULLWIDTH_DIGIT)) {
            if (charTypeArray[j] == CharType.FULLWIDTH_DIGIT)
              hasFullWidth = true;
            j++;
          }
          // Found a Token from i to j. Type is NUMBER char string.
          charArray = Utility.NUMBER_CHAR_ARRAY;
          frequency = wordDict.getFrequency(charArray);
          wordType = hasFullWidth ? WordType.FULLWIDTH_NUMBER : WordType.NUMBER;
          token = new SegToken(charArray, i, j, wordType, frequency);
          segGraph.addToken(token);
          i = j;
          break;
        case CharType.DELIMITER:
          j = i + 1;
          // No need to search the weight for the punctuation.  Picking the highest frequency will work.
          frequency = Utility.MAX_FREQUENCE;
          charArray = new char[] { sentence.charAt(i) };
          token = new SegToken(charArray, i, j, WordType.DELIMITER, frequency);
          segGraph.addToken(token);
          i = j;
          break;
        default:
          j = i + 1;
          // Treat the unrecognized char symbol as unknown string.
          // For example, any symbol not in GB2312 is treated as one of these.
          charArray = Utility.STRING_CHAR_ARRAY;
          frequency = wordDict.getFrequency(charArray);
          token = new SegToken(charArray, i, j, WordType.STRING, frequency);
          segGraph.addToken(token);
          i = j;
          break;
      }
    }

    // Add two more Tokens: "beginning xx beginning"
    charArray = Utility.START_CHAR_ARRAY;
    frequency = wordDict.getFrequency(charArray);
    token = new SegToken(charArray, -1, 0, WordType.SENTENCE_BEGIN, frequency);
    segGraph.addToken(token);

    // "end xx end"
    charArray = Utility.END_CHAR_ARRAY;
    frequency = wordDict.getFrequency(charArray);
    token = new SegToken(charArray, length, length + 1, WordType.SENTENCE_END,
        frequency);
    segGraph.addToken(token);

    return segGraph;
  }

  /**
   * Get the character types for every character in a sentence.
   * 
   * @see Utility#getCharType(char)
   * @param sentence input sentence
   * @return array of character types corresponding to character positions in the sentence
   */
  private static int[] getCharTypes(String sentence) {
    int length = sentence.length();
    int[] charTypeArray = new int[length];
    // the type of each character by position
    for (int i = 0; i < length; i++) {
      charTypeArray[i] = Utility.getCharType(sentence.charAt(i));
    }

    return charTypeArray;
  }

  /**
   * Return a list of {@link SegToken} representing the best segmentation of a sentence
   * @param sentence input sentence
   * @return best segmentation as a {@link List}
   */
  public List<SegToken> process(String sentence) {
    SegGraph segGraph = createSegGraph(sentence);
    BiSegGraph biSegGraph = new BiSegGraph(segGraph);
//    System.out.println(biSegGraph.toString());
    List<SegToken> shortPath = biSegGraph.getShortPath();
    return shortPath;
  }
  
	public static void main(String args[]){
		HHMMSegmenter seg = new HHMMSegmenter();
		List<String> testStr = new ArrayList<String>();
		testStr.add("this is a boring 第");
		testStr.add("12.第");
		testStr.add("一九九五年12月31日,");
		testStr.add("1/++ ￥+400 ");
		testStr.add("-2e-12 xxxx1E++300/++"); 
		testStr.add("1500名常用的数量和人名的匹配 超过22万个");
		testStr.add("据路透社报道，印度尼西亚社会事务部一官员星期二(29日)表示，" 
				+ "日惹市附近当地时间27日晨5时53分发生的里氏6.2级地震已经造成至少5427人死亡，" 
				+ "20000余人受伤，近20万人无家可归。");
		testStr.add("古田县城关六一四路四百零五号");
		testStr.add("欢迎使用阿江统计2.01版");
		testStr.add("51千克五十一千克五万一千克两千克拉 五十一");
		testStr.add("十一点半下班十一点下班");
		testStr.add("福州第一中学福州一中福州第三十六中赐进士及第");
		
		
		for(String t : testStr){
			System.out.println(t);	
			List<SegToken> list = seg.process(t);
			for(SegToken token: list){
				System.err.print(token.toString()+" | ");
			}
			
		}
		System.out.println("***************");	
	}
  
}
