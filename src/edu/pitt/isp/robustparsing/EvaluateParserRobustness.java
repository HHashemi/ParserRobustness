package edu.pitt.isp.robustparsing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import TER.TERalignment;
import TER.TERcalc;
import TER.TERcost;

public class EvaluateParserRobustness {

//  ESL sample
//	static String treeFile1 = "data/ESL/FCE_10_Turbo.Good.txt"; 
//	static String treeFile2 = "data/ESL/FCE_10_Turbo.Bad.txt"; 
//	static String outputFile = "data/ESL/FCE_10_robustnessResult.txt"; // the output file	
//	static String correctionsFile = "data/ESL/FCE_random_10_sentencesCorrections.Bad.txt"; 
//	static String sentencePOSFile1 = "data/ESL/FCE_10_sentences.POS.Good.txt"; 
//	static String sentencePOSFile2 = "data/ESL/FCE_10_sentences.POS.Bad.txt"; 
//	static String sentenceSRLFile1 = "data/ESL/FCE_10_SRL.Good.txt"; 
//	static String corpusType = "ESL"; 
//	static String errorGroupNumber = "7"; // Figure 2 of EMNLP paper. ESL sentences with more than 7 errors will be in the 7+ category
	
//  MT sample
	static String treeFile1 = "data/MT/HTER_10000_Turbo.Good.txt"; 
	static String treeFile2 = "data/MT/HTER_10000_Turbo.Bad.txt"; 
	static String outputFile = "data/MT/HTER_10000_robustnessResult.txt";
	static String correctionsFile = "";
	static String sentencePOSFile1 = "data/MT/HTER_10000_sentences.POS.Good.txt"; 
	static String sentencePOSFile2 = "data/MT/HTER_10000_sentences.POS.Bad.txt"; 
	static String sentenceSRLFile1 = "data/MT/HTER_10000_SRL.Good.txt"; 
	static String corpusType = "MT";
	static String errorGroupNumber = "10"; // Figure 2 of EMNLP paper. MT Sentences with more than 10 edit distance will be in the 10+ category

	
	public static void main(String args[]) throws IOException, SQLException{
		evaluateRobustnessIgnoreErrors();
	}

	/**
	 * This method gets two dependency parse tree files and separate sentence and then calculate robustness by ignoring error related edges
	 * @throws IOException 
	 */
	private static void evaluateRobustnessIgnoreErrors() throws IOException {
		
		boolean ignorePunctuation = false ; // true: it will ignore words that are punctuations. It is because Tweebo parser does not consider punctuations and we want to compare all parsers together 
		if (treeFile1.contains("_trainTwitter")) // only ignore punctuations when parsers are trained on Twitter data
			ignorePunctuation = true;		
		
		BufferedReader brGood = new BufferedReader(new InputStreamReader(new FileInputStream(treeFile1), "UTF8"));
		BufferedReader brBad = new BufferedReader(new InputStreamReader(new FileInputStream(treeFile2), "UTF8"));
				
		BufferedReader brGoodPOS = new BufferedReader(new InputStreamReader(new FileInputStream(sentencePOSFile1), "UTF8"));
		BufferedReader brBadPOS = new BufferedReader(new InputStreamReader(new FileInputStream(sentencePOSFile2), "UTF8"));
		
		BufferedReader brGoodSRL = new BufferedReader(new InputStreamReader(new FileInputStream(sentenceSRLFile1), "UTF8"));
		
		Writer outFile = new BufferedWriter(new FileWriter(new File(outputFile)));
		
		BufferedReader brCorrections = null;
		if(corpusType.equals("ESL"))
			brCorrections = new BufferedReader(new InputStreamReader(new FileInputStream(correctionsFile), "UTF8"));

		// Initialization
		String line1 = brGood.readLine(), line2 = brBad.readLine();
		int count = 0;
		int count_matched_labeled = 0;
		int count_matched_unlabeled = 0;
		int count_all_goodsent_dependencies = 0;
		int count_all_badsent_dependencies = 0;
		int pos_closed = 0, pos_open = 0;
		
		List<Double> labeledFscoreAll = new ArrayList<>();
		List<Double> unlabeledFscoreAll = new ArrayList<>();
		
		HashMap<Integer, ArrayList<Integer>> mapErrorNumberSeparated = new HashMap<>(); 
		mapErrorNumberSeparated.put(1, new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorNumberSeparated.put(2, new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorNumberSeparated.put(3, new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));		
		mapErrorNumberSeparated.put(4, new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));		
		mapErrorNumberSeparated.put(5, new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorNumberSeparated.put(6, new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorNumberSeparated.put(7, new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		if (errorGroupNumber.equals("10")){
			mapErrorNumberSeparated.put(8, new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
			mapErrorNumberSeparated.put(9, new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
			mapErrorNumberSeparated.put(10, new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		}	
		
		HashMap<String, ArrayList<Integer>> mapErrorTypeSeparated = new HashMap<>(); 
		mapErrorTypeSeparated.put("Replacement", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("Missing", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("Unnecessary", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("OpenPOS", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("ClosedPOS", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("ReplacementOpenPOS", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("MissingOpenPOS", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("UnnecessaryOpenPOS", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("ReplacementClosedPOS", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("MissingClosedPOS", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("UnnecessaryClosedPOS", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		
		mapErrorTypeSeparated.put("Replacement3", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("Missing3", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("Unnecessary3", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("EditMixed3", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));

		
		mapErrorTypeSeparated.put("Replacement3+", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("Missing3+", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("Unnecessary3+", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("EditMixed3+", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));

		mapErrorTypeSeparated.put("Replacement4+", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("Missing4+", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("Unnecessary4+", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));	
		mapErrorTypeSeparated.put("EditMixed4+", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		
		mapErrorTypeSeparated.put("OpenPOS3", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("ClosedPOS3", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("MixedPOS3", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		
		mapErrorTypeSeparated.put("OpenPOS3+", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("ClosedPOS3+", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("MixedPOS3+", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));

		mapErrorTypeSeparated.put("OpenPOS4+", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("ClosedPOS4+", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("MixedPOS4+", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));

		mapErrorTypeSeparated.put("errDistance0", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("errDistance1", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("errDistance2", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("errDistance3", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("errDistance4", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("errDistance5", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("errDistance6", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("errDistance7", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("errDistance8", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("errDistance9", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("errDistance10", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));

		mapErrorTypeSeparated.put("srlVerb1", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgs1", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlNone1", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		
		mapErrorTypeSeparated.put("srlArgs1A1", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgs1A0", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgs1AM-Mod", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgs1A2", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgs1AM-TMP", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgs1AM-ADV", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgs1AM-DIS", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgs1AM-MNR", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgs1AM-NEG", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgs1AM-LOC", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgs1AM-REC", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgs1AM-CAU", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgs1AM-PNC", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgs1R-A1", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgs1C-A1", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgs1other", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		
		mapErrorTypeSeparated.put("srlVerbAll", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgsAll", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlNoneAll", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlMixedAll", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		
		mapErrorTypeSeparated.put("srlVerbMajority", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgsMajority", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlNoneMajority", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlMixedMajority", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		
		mapErrorTypeSeparated.put("srlVerb4+", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgs4+", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlNone4+", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlMixed4+", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		
		mapErrorTypeSeparated.put("srlVerb3+", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgs3+", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlNone3+", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlMixed3+", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		
		mapErrorTypeSeparated.put("srlVerb3", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgs3", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlNone3", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlMixed3", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		
		mapErrorTypeSeparated.put("srlArgsAllA1", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgsAllA0", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgsAllMod", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgsAllA2", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgsAllTMP", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgsAllADV", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgsAllDIS", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgsAllMNR", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgsAllNEG", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("srlArgsAllLOC", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		
		mapErrorTypeSeparated.put("err2nearDistance", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("err2farDistance", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));

		mapErrorTypeSeparated.put("err3nearDistance", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		mapErrorTypeSeparated.put("err3farDistance", new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0, 0)));
		
		
		// To collect information about type of errors in the corpus, e.g. what percent of errors in the 3 error sentences are closed class errors?
		HashMap<Integer, HashMap<String, Integer>> mapErrorStatisticsAll = new HashMap<>(); 
	
		mapErrorStatisticsAll.put(1, makeEmptyMapErrorStatisticsSeperated());
		mapErrorStatisticsAll.put(2, makeEmptyMapErrorStatisticsSeperated());
		mapErrorStatisticsAll.put(3, makeEmptyMapErrorStatisticsSeperated());
		mapErrorStatisticsAll.put(4, makeEmptyMapErrorStatisticsSeperated());
		if (errorGroupNumber.equals("7") || errorGroupNumber.equals("10")){
			mapErrorStatisticsAll.put(5, makeEmptyMapErrorStatisticsSeperated());
			mapErrorStatisticsAll.put(6, makeEmptyMapErrorStatisticsSeperated());
			mapErrorStatisticsAll.put(7, makeEmptyMapErrorStatisticsSeperated());
		}
		if (errorGroupNumber.equals("10")){
			mapErrorStatisticsAll.put(8, makeEmptyMapErrorStatisticsSeperated());
			mapErrorStatisticsAll.put(9, makeEmptyMapErrorStatisticsSeperated());
			mapErrorStatisticsAll.put(10, makeEmptyMapErrorStatisticsSeperated());
		}	

	
		int strangeSentences = 0;
		int noErrorSentences = 0;
		
		// Read Good and Bad sentences
		while(line1 != null || line2 != null ){
			
			int count_matched_labeled_one = 0;
			int count_matched_unlabeled_one = 0;
			
			//initialization
			String sentGood="", sentBad="";
			int countGood = 0, countBad = 0;
			HashMap<Integer, String> wordsGood = new HashMap<Integer, String>();
			HashMap<Integer, String> wordsBad = new HashMap<Integer, String>();
			HashMap<Integer, Integer> mapGoodBad = new HashMap<Integer, Integer>();
			HashMap<Integer, Integer> mapBadGood = new HashMap<Integer, Integer>();
			List<String> linesGood = new ArrayList<String>();
			List<String> linesBad = new ArrayList<String>();
			HashMap<String, String> mapGoodDependencies = new HashMap<String, String>();
			HashMap<String, String> mapBadDependencies = new HashMap<String, String>();
			HashMap<String, String> mapGoodDependenciesError = new HashMap<String, String>();
			HashMap<String, String> mapBadDependenciesError = new HashMap<String, String>();

			// read Good sentence
			while(line1 != null && line1.length() != 0){
				linesGood.add(line1);
				String[] words = line1.split("\t");
				sentGood = sentGood + " " + words[1];
				countGood++;
				wordsGood.put(countGood, words[1]);
				line1 = brGood.readLine();
			}
			line1 = brGood.readLine();
			
			// read Bad sentence
			while(line2 != null && line2.length() != 0){
				linesBad.add(line2);
				String[] words = line2.split("\t");
				sentBad = sentBad + " " + words[1];	
				countBad++;
				wordsBad.put(countBad, words[1]);
				line2 = brBad.readLine();
			}
			line2 = brBad.readLine();
			
			// read POS tags of Good and Bad sentences
			HashMap<String, String> posGood = readPOStags(brGoodPOS.readLine()); 
			HashMap<String, String> posBad = readPOStags(brBadPOS.readLine()); 
			
			// read SRL relations for the sentence
			HashMap<String, HashMap<String, List<Integer>>> SRLmap = readSRLrelations(brGoodSRL);
					
			// aligning Good and Bad sentences, depends whether they are from ESL or MT corpora
			List<ErrorCorrectionBean> correctionsList = null;
			if(corpusType.equals("ESL")){		
				// Use FCE error corrections to find error-related words in Good and Bad sentences, read error correction from brCorrections
				String[] lineCorrection = brCorrections.readLine().split("\t");
				System.out.println(lineCorrection[1] + "\n" +lineCorrection[3]);	 
				
				// make a list of corrections
				correctionsList = makeCorrectionsList(lineCorrection[3]);
				int good_ind = 1, bad_ind = 1;
				int noMissingWords = findNumberOfMissingWords(correctionsList);
				
				// find the alignments between Good and Bad sentences based on corrections and their types
				for(int i=0;  i < (wordsBad.size() + noMissingWords ) ; i++){
					List<ErrorCorrectionBean> specificCorrections = findCorrectionType(i, correctionsList); 
					if(specificCorrections.size() == 0){
						if (wordsBad.containsKey(bad_ind)){
							mapGoodBad.put(good_ind, bad_ind);
							mapBadGood.put(bad_ind, good_ind);
							System.out.println(good_ind + "\t" + wordsGood.get(good_ind) + "\t" + bad_ind + "\t" + wordsBad.get(bad_ind));
							good_ind++;
							bad_ind++;
						}
						continue;
					}
					if(wordsGood.containsKey(good_ind) && wordsBad.containsKey(bad_ind) && wordsGood.get(good_ind).equals(wordsBad.get(bad_ind)) && !mapGoodBad.containsKey(good_ind) && !mapBadGood.containsKey(bad_ind)){
						mapGoodBad.put(good_ind, bad_ind);
						mapBadGood.put(bad_ind, good_ind);
						System.out.println(good_ind + "\t" + wordsGood.get(good_ind) + "\t" + bad_ind + "\t" + wordsBad.get(bad_ind));
						good_ind++;
						bad_ind++;
					}
					
					for(ErrorCorrectionBean correction : specificCorrections){
						String type = correction.getType();
						if (type.startsWith("U") && correction.getCorr().length()<1){
							bad_ind++;						
						}else if (type.startsWith("M")){
							good_ind++;
						}else{
							if(correction.getStart() + 1 == bad_ind || type.startsWith("U")){
								mapGoodBad.put(good_ind, bad_ind);
								mapBadGood.put(bad_ind, good_ind);
								System.out.println(good_ind + "\t" + wordsGood.get(good_ind) + "\t" + bad_ind + "\t" + wordsBad.get(bad_ind));
								good_ind++;
								bad_ind++;
							} else
								System.out.println();
						}
					}
				}	
			} else if(corpusType.equals("MT")){ // find the corrections and the mappings using TER
				TERcost costObj = new TERcost();
				TERcalc.setCase(true);
				costObj.setShift_cost(2.0);
				costObj.setInsert_cost(0.5);
				costObj.setDelete_cost(0.5);
				costObj.setSusbtitue_cost(0.5);
				
				TERalignment out = TERcalc.TER(sentBad.trim(), sentGood.trim(), costObj);  //String hyp, String ref				
				
				System.out.println(out.toMoreString());
				double HTER = out.score();
				
				correctionsList = fillCorrectionsListWithAlignments(out.alignment, wordsGood, wordsBad);
				// TER fill alignments:
				int good_ind = 1, bad_ind = 1;
				for (int i = 0; i < out.alignment.length; i++){
					char a = out.alignment[i];
					if (a == ' ' || a == 'S'){						
						mapGoodBad.put(good_ind, bad_ind);
						mapBadGood.put(bad_ind, good_ind);
						System.out.println(good_ind + "\t" + wordsGood.get(good_ind) + "\t" + bad_ind + "\t" + wordsBad.get(bad_ind));
						good_ind++;
						bad_ind++;
					} else if (a == 'I'){
						bad_ind++;
					} else if (a == 'D'){
						good_ind++;
					}
				}

			}
			
			List<String> punctuations = Arrays.asList("\"", "''", "``", ":", ",", ".", "'");
			// Count matched dependencies between trees of good and bad sentences. First fill all dependencies in good sent and bad sent based on indexes of the good sent
			for(String line : linesGood){
				String[] words = line.split("\t");
				int wordInd = Integer.parseInt(words[0]);
				int headInd = Integer.parseInt(words[6]);
				String label = words[7];
				
				//ignore punctuation for all parsers
				if(ignorePunctuation){
					if(punctuations.contains(words[1]) || punctuations.contains(words[3])){
						System.out.println("ignore punctuation: " + words[1]);
						continue;
					}
				}
				
				if(words[6].equals("-1")) //added to handle heads in TweeboParser for punctuation (it will ignore punctuation for tweebo, because the head of punctuation is alwasys -1, does not matter what is the punc)
					continue;
				mapGoodDependencies.put(wordInd + "," + headInd, label);
				if (!mapGoodBad.containsKey(wordInd) || (!mapGoodBad.containsKey(headInd) && !words[6].equals("0") && !words[6].equals("-1") )  ){ // find error-related dependencies: the start or end of relation is not aligned so it is a missing error
					mapGoodDependenciesError.put(wordInd + "," + headInd, label);
				}
			}
			
			for(String line: linesBad){
				String[] words = line.split("\t");
				int wordInd = 0;
				int headInd = 0;
				// find error-related dependencies: the start or end of relation is not aligned so it is an unnecessary error
				if(!mapBadGood.containsKey(Integer.parseInt(words[0])) || ( !mapBadGood.containsKey(Integer.parseInt(words[6])) && !words[6].equals("0") && !words[6].equals("-1") ) ){ // if the word or its head are not aligned
					mapBadDependenciesError.put(Integer.parseInt(words[0]) + "," + Integer.parseInt(words[6]), words[7]);
					continue;
				}
				wordInd = mapBadGood.get(Integer.parseInt(words[0])); // set the index of aligned word in good sent to the word in bad sent
				
				//ignore punctuation for all parsers
				if(ignorePunctuation){
					if(punctuations.contains(words[1]) || punctuations.contains(words[3])){
						System.out.println("ignore punctuation: " + words[1]);
						continue;
					}
				}
				
				if(words[6].equals("-1")){ //added to handle heads in TweeboParser for punctuation
					headInd = -1;
					continue;
				}
				else if(!words[6].equals("0") )
					headInd = mapBadGood.get(Integer.parseInt(words[6]));
				String label = words[7];
				mapBadDependencies.put(wordInd + "," + headInd, label);
			}
			
			// find number of matched dependencies
			for(String key : mapGoodDependencies.keySet()){
				if(mapBadDependencies.containsKey(key)){
					count_matched_unlabeled++;
					count_matched_unlabeled_one++;				
 					if(mapGoodDependencies.get(key).equals(mapBadDependencies.get(key))){
						count_matched_labeled++;
						count_matched_labeled_one++;
 					}
				}
			}
			
			count++;
			
			int denominatorGood = mapGoodDependencies.size() - mapGoodDependenciesError.size();
			int denominatorBad = mapBadDependencies.size(); 
			 
			count_all_goodsent_dependencies = count_all_goodsent_dependencies + denominatorGood;
			count_all_badsent_dependencies = count_all_badsent_dependencies + denominatorBad;
			
			double precision_one = (double) count_matched_unlabeled_one/(double) denominatorBad;
			double recall_one = (double) count_matched_unlabeled_one/(double) denominatorGood;
			double unlabeled_fscore_one = 2*(precision_one * recall_one)/(precision_one + recall_one);

			precision_one = (double) count_matched_labeled_one/(double) denominatorBad;
			recall_one = (double) count_matched_labeled_one/(double) denominatorGood;
			double labeled_fscore_one = 2*(precision_one * recall_one)/(precision_one + recall_one);

			// avoid f-score to be NaN
			if(Double.isNaN(unlabeled_fscore_one))
				unlabeled_fscore_one = 0;
			
			if(Double.isNaN(labeled_fscore_one))
				labeled_fscore_one = 0;
			
			System.out.println("\nPrecision\tRecall\tF-score\n" + precision_one*100 + "\t" + recall_one*100 + "\t" + unlabeled_fscore_one*100 + "\n");
			
			labeledFscoreAll.add(labeled_fscore_one);
			unlabeledFscoreAll.add(unlabeled_fscore_one);	
				
			if (precision_one < 0.8 && mapBadDependencies.size()>3)
				System.out.println("low precision");
			
			
			//separate precision and recall based on error numbers
			int errorNumber = correctionsList.size();
						
			if (errorGroupNumber.equals("two") && errorNumber > 2)
				errorNumber = 2;
			if (errorGroupNumber.equals("four") && errorNumber>4)
				errorNumber=4;
			if (errorGroupNumber.equals("7") && errorNumber>6)
				errorNumber=7;
			if (errorGroupNumber.equals("10") && errorNumber>9)
				errorNumber=10;
			
			if(errorNumber == 0) { 
				// for example when there is a large shift in the HTER data
				// Alassane Ouattara , man of the North who denies any link with the ex-rebels , considered the delay in the announcement of the results `` unacceptable '' and asked Gbagbo to respect them , but he did not go as far as claiming the victory .
				// Alassane Ouattara , man of the North who denies any link with the ex-rebels , considered `` unacceptable '' the delay in the announcement of the results and asked Gbagbo to respect them , but he did not go as far as claiming the victory .
				System.out.println(sentGood + "\n" + sentBad);
				noErrorSentences++;
				continue;
			}
			
			ArrayList<Integer> arrayNo = mapErrorNumberSeparated.get(errorNumber);
			arrayNo.set(0, arrayNo.get(0) + count_matched_labeled_one);
			arrayNo.set(1, arrayNo.get(1) + count_matched_unlabeled_one);
			arrayNo.set(2, arrayNo.get(2) + denominatorGood);
			arrayNo.set(3, arrayNo.get(3) + denominatorBad);
			arrayNo.set(4, arrayNo.get(4) + 1);
			mapErrorNumberSeparated.put(errorNumber, arrayNo);

			// find general statistics over the type of errors in sentences with specific number of errors
			HashMap<String, Integer> map = mapErrorStatisticsAll.get(errorNumber);
			for(int j=0; j< correctionsList.size(); j++){
				String pos = "";
				String key = "";
				String type = correctionsList.get(j).getType();
				int start = correctionsList.get(j).getStart() + 1;
				String correctTo = correctionsList.get(j).getCorr();
				if(type.startsWith("M") || type.startsWith("D")) {
					pos = posGood.get(correctTo);
					map.put("allMissing", map.get("allMissing")+1);
				} else if (type.startsWith("U") || type.startsWith("I")){
					pos = posBad.get(wordsBad.get(start));
					map.put("allUnnecessary", map.get("allUnnecessary")+1);
				} else {
					pos = posGood.get(correctTo);
					map.put("allReplacement", map.get("allReplacement")+1);
				}
				// to handle Tweebo Parser tokenization when dealing with '
				if (pos == null){
					if(sentBad.contains("'") && type.startsWith("U") || type.startsWith("I")){
						start++;
						pos = posBad.get(wordsBad.get(start));
					} 
					if(pos == null){ // there is no instance right now
						strangeSentences++;
						continue;
					}
				}
				key = isOpenOrClosedPOS(pos);
				if(key.startsWith("Open"))
					map.put("allOpenPOS", map.get("allOpenPOS")+1);
				else if (key.startsWith("Close"))
					map.put("allClosedPOS", map.get("allClosedPOS")+1);
				String errorSRLposition = findErrorPositionInSRL(j, correctionsList, SRLmap);
				if (errorSRLposition.equals("Verbs")){
					map.put("allSRLVerb", map.get("allSRLVerb")+1);
				}else if (errorSRLposition.equals("Arguments")){
					map.put("allSRLArgs", map.get("allSRLArgs")+1);
				}else{
					map.put("allSRLNone", map.get("allSRLNone")+1);
				}
			}
			mapErrorStatisticsAll.put(errorNumber, map);
			
			//separate precision and recall based on error type
			if(correctionsList.size() == 1){ 
				String key = "";
				if (correctionsList.get(0).getType().startsWith("M") || correctionsList.get(0).getType().startsWith("D")){ // it has an missing error
					key = "Missing";
				}else if(correctionsList.get(0).getType().startsWith("U") || correctionsList.get(0).getType().startsWith("I")){ // it has an unnecessary error
					key = "Unnecessary";
				} else{
					key = "Replacement";
				}
				ArrayList<Integer> array = mapErrorTypeSeparated.get(key);
				array.set(0, array.get(0) + count_matched_labeled_one);
				array.set(1, array.get(1) + count_matched_unlabeled_one);
				array.set(2, array.get(2) + denominatorGood);
				array.set(3, array.get(3) + denominatorBad);
				array.set(4, array.get(4)+1);
				mapErrorTypeSeparated.put(key, array);
			}
			
			System.out.println(correctionsList);
			System.out.println(posGood);
			
			
			//find POS tags of error words and then categorize them into open or closed word class errors
			if(correctionsList.size() == 1){		
				String pos = "";
				String key = "";
				for(int j=0; j< correctionsList.size(); j++){
					String type = correctionsList.get(j).getType();
					int start = correctionsList.get(j).getStart() + 1;
					String correctTo = correctionsList.get(j).getCorr();
					if(type.startsWith("M") || type.startsWith("D")) {
						pos = posGood.get(correctTo);
					} else if (type.startsWith("U") || type.startsWith("I")){
						pos = posBad.get(wordsBad.get(start));
					} else {
						pos = posGood.get(correctTo);
					}
					// to handle Tweebo Parser tokenization when dealing with '
					if (pos == null){
						if(sentBad.contains("'") && type.startsWith("U") || type.startsWith("I")){
							start++;
							pos = posBad.get(wordsBad.get(start));
						} 
						if(pos == null){ // there is no instance right now
							strangeSentences++;
							continue;
						}
					}
					key = isOpenOrClosedPOS(pos);
					ArrayList<Integer> array = mapErrorTypeSeparated.get(key);
					array.set(0, array.get(0) + count_matched_labeled_one);
					array.set(1, array.get(1) + count_matched_unlabeled_one);
					array.set(2, array.get(2) + denominatorGood);
					array.set(3, array.get(3) + denominatorBad);
					array.set(4, array.get(4)+1);
					mapErrorTypeSeparated.put(key, array);
				}
			}
			
			//find error type then its POS tags. So there would be 6 groups of errors
			if(correctionsList.size() == 1){		
				String pos = "";
				String key = "";
				for(int j=0; j< correctionsList.size(); j++){
					String type = correctionsList.get(j).getType();
					int start = correctionsList.get(j).getStart() + 1;
					String correctTo = correctionsList.get(j).getCorr();
					if(type.startsWith("M") || type.startsWith("D")) {
						pos = posGood.get(correctTo);
						key = isOpenOrClosedPOS(pos);	
						key = "Missing" + key;
					} else if (type.startsWith("U") || type.startsWith("I")){
						pos = posBad.get(wordsBad.get(start));
						// to handle Tweebo Parser tokenization when dealing with '
						if (pos == null && sentBad.contains("'") ){
							start++;
							pos = posBad.get(wordsBad.get(start));
						}
						if(pos == null && wordsBad.get(start).equals("<num>")) //to handle MST output which changes numbers to <num> in HTER
							pos = "CD";
						if(pos == null) // to handle utf-8 characters of Yara parser for HTER
							pos = "CD";
						key = isOpenOrClosedPOS(pos);
						key = "Unnecessary" + key;
					} else {
						pos = posGood.get(correctTo);
						key = isOpenOrClosedPOS(pos);
						key = "Replacement" + key;
					}			
					ArrayList<Integer> array = mapErrorTypeSeparated.get(key);
					array.set(0, array.get(0) + count_matched_labeled_one);
					array.set(1, array.get(1) + count_matched_unlabeled_one);
					array.set(2, array.get(2) + denominatorGood);
					array.set(3, array.get(3) + denominatorBad);
					array.set(4, array.get(4)+1);
					mapErrorTypeSeparated.put(key, array);
				}
			}
			
			//find error distance - using corrections 
			double avgErrorDistance = 0.0;
			if(correctionsList.size() > 1){
				avgErrorDistance = getAverageErrorDistance(correctionsList);
				int bin = (int) Math.floor(avgErrorDistance);
				if(bin>10)
					bin=10;
				String key = "errDistance" + bin;
				ArrayList<Integer> array = mapErrorTypeSeparated.get(key);
				array.set(0, array.get(0) + count_matched_labeled_one);
				array.set(1, array.get(1) + count_matched_unlabeled_one);
				array.set(2, array.get(2) + denominatorGood);
				array.set(3, array.get(3) + denominatorBad);
				array.set(4, array.get(4)+1);
				mapErrorTypeSeparated.put(key, array);
			}
			
			// find error distance when there is only 2 errors in the sentence to compare 2 near error with 2 far errors
			if(correctionsList.size() == 2){
				double[] errorDistance = getErrorDistance(correctionsList);
				String key = "";
				if (errorDistance[1]-errorDistance[0]> 7)
					key = "err2farDistance";
				else if (errorDistance[1]-errorDistance[0]<2)
					key = "err2nearDistance";
				
				if (key.length() > 1){
					ArrayList<Integer> array = mapErrorTypeSeparated.get(key);
					array.set(0, array.get(0) + count_matched_labeled_one);
					array.set(1, array.get(1) + count_matched_unlabeled_one);
					array.set(2, array.get(2) + denominatorGood);
					array.set(3, array.get(3) + denominatorBad);
					array.set(4, array.get(4)+1);
					mapErrorTypeSeparated.put(key, array);
				}
			}
			
			// find error distance when there is only 3 errors in the sentence to compare 3 near error with 3 far errors
			if(correctionsList.size() == 3){
				double[] errorDistance = getErrorDistance(correctionsList);
				String key = "";
				if (errorDistance[1]-errorDistance[0]> 5 && errorDistance[2]-errorDistance[1] > 5)
					key = "err3farDistance";
				else if (errorDistance[1]-errorDistance[0]<2 && errorDistance[2]-errorDistance[1] < 2)
					key = "err3nearDistance";
				
				if (key.length() > 1){
					ArrayList<Integer> array = mapErrorTypeSeparated.get(key);
					array.set(0, array.get(0) + count_matched_labeled_one);
					array.set(1, array.get(1) + count_matched_unlabeled_one);
					array.set(2, array.get(2) + denominatorGood);
					array.set(3, array.get(3) + denominatorBad);
					array.set(4, array.get(4)+1);
					mapErrorTypeSeparated.put(key, array);
				}
			}
			
			//when there is only one error in the sentence, will the error in SRL verb be different from error in SRL arguments or some where else?
			if(correctionsList.size() == 1){
				String errorSRLposition = findErrorPositionInSRL(0, correctionsList, SRLmap);
				String key = "";
				String argKey = "";
				if (errorSRLposition.equals("Verbs"))
					key = "srlVerb1";
				else if (errorSRLposition.equals("Arguments")){
					key = "srlArgs1";
					// separate based on the type of argument: 10 most frequent arguments 
					argKey = findErrorPositionInSRLArgument(0, correctionsList, SRLmap);
					argKey = "srlArgs1" + argKey;
					if(!mapErrorTypeSeparated.containsKey(argKey))
						argKey = "srlArgs1other";
					ArrayList<Integer> array = mapErrorTypeSeparated.get(argKey);
					array.set(0, array.get(0) + count_matched_labeled_one);
					array.set(1, array.get(1) + count_matched_unlabeled_one);
					array.set(2, array.get(2) + denominatorGood);
					array.set(3, array.get(3) + denominatorBad);
					array.set(4, array.get(4)+1);
					mapErrorTypeSeparated.put(argKey, array);
					
				}else
					key = "srlNone1";
				ArrayList<Integer> array = mapErrorTypeSeparated.get(key);
				array.set(0, array.get(0) + count_matched_labeled_one);
				array.set(1, array.get(1) + count_matched_unlabeled_one);
				array.set(2, array.get(2) + denominatorGood);
				array.set(3, array.get(3) + denominatorBad);
				array.set(4, array.get(4)+1);
				mapErrorTypeSeparated.put(key, array);
			}
			
			// when there are more than x number of errors in the sentence, compare Replacement, Missing and Unnecessary errors
			if(correctionsList.size() >= 1){
				HashMap<String, Integer> temp = new HashMap<>(); temp.put("Replacement", 0); temp.put("Missing", 0); temp.put("Unnecessary", 0);
				String key = "";
				for(int j=0; j< correctionsList.size(); j++){	
					if (correctionsList.get(j).getType().startsWith("M") || correctionsList.get(j).getType().startsWith("D")){ // it has an missing error
						key = "Missing";
					}else if(correctionsList.get(j).getType().startsWith("U") || correctionsList.get(j).getType().startsWith("I")){ // it has an unnecessary error
						key = "Unnecessary";
					} else{
						key = "Replacement";
					}
					temp.put(key, temp.get(key)+1);
				}
				if(correctionsList.size() >= 4){
					if(temp.get("Missing") == 0 && temp.get("Unnecessary") == 0)
						key = "Replacement4+";
					else if (temp.get("Replacement") == 0 && temp.get("Unnecessary") == 0)
						key = "Missing4+";
					else if (temp.get("Replacement") == 0 && temp.get("Missing") == 0)
						key = "Unnecessary4+";
					else {
						key = "EditMixed4+";
					}
					ArrayList<Integer> array = mapErrorTypeSeparated.get(key);
					array.set(0, array.get(0) + count_matched_labeled_one);
					array.set(1, array.get(1) + count_matched_unlabeled_one);
					array.set(2, array.get(2) + denominatorGood);
					array.set(3, array.get(3) + denominatorBad);
					array.set(4, array.get(4)+1);
					mapErrorTypeSeparated.put(key, array);
					
				} 
				if (correctionsList.size() >= 3){
					if(temp.get("Missing") == 0 && temp.get("Unnecessary") == 0)
						key = "Replacement3+";
					else if (temp.get("Replacement") == 0 && temp.get("Unnecessary") == 0)
						key = "Missing3+";
					else if (temp.get("Replacement") == 0 && temp.get("Missing") == 0)
						key = "Unnecessary3+";
					else {
						key = "EditMixed3+";
					}
					ArrayList<Integer> array = mapErrorTypeSeparated.get(key);
					array.set(0, array.get(0) + count_matched_labeled_one);
					array.set(1, array.get(1) + count_matched_unlabeled_one);
					array.set(2, array.get(2) + denominatorGood);
					array.set(3, array.get(3) + denominatorBad);
					array.set(4, array.get(4)+1);
					mapErrorTypeSeparated.put(key, array);
				} 
				if (correctionsList.size() == 3){
					if(temp.get("Missing") == 0 && temp.get("Unnecessary") == 0)
						key = "Replacement3";
					else if (temp.get("Replacement") == 0 && temp.get("Unnecessary") == 0)
						key = "Missing3";
					else if (temp.get("Replacement") == 0 && temp.get("Missing") == 0)
						key = "Unnecessary3";
					else {
						key = "EditMixed3";
					}
					ArrayList<Integer> array = mapErrorTypeSeparated.get(key);
					array.set(0, array.get(0) + count_matched_labeled_one);
					array.set(1, array.get(1) + count_matched_unlabeled_one);
					array.set(2, array.get(2) + denominatorGood);
					array.set(3, array.get(3) + denominatorBad);
					array.set(4, array.get(4)+1);
					mapErrorTypeSeparated.put(key, array);
				}
			}	
			
			//when there are more than x number of errors in the sentence, compare Open and Closed errors
			if(correctionsList.size() >= 1){	
				HashMap<String, Integer> temp = new HashMap<>(); temp.put("open", 0); temp.put("closed", 0);
				String pos = "";
				String key = "";
				for(int j=0; j< correctionsList.size(); j++){
					String type = correctionsList.get(j).getType();
					int start = correctionsList.get(j).getStart() + 1;
					String correctTo = correctionsList.get(j).getCorr();
					if(type.startsWith("M") || type.startsWith("D")) {
						pos = posGood.get(correctTo);
					} else if (type.startsWith("U") || type.startsWith("I")){
						pos = posBad.get(wordsBad.get(start));
					} else {
						pos = posGood.get(correctTo);
					}
					// to handle Tweebo Parser tokenization when dealing with '
					if (pos == null){
						if(sentBad.contains("'") && type.startsWith("U") || type.startsWith("I")){
							start++;
							pos = posBad.get(wordsBad.get(start));
						} 
						if(pos == null){ // there is no instance right now
							strangeSentences++;
							continue;
						}
					}
					key = isOpenOrClosedPOS(pos);
					if(key.equals("OpenPOS"))
						temp.put("open", temp.get("open")+1);
					else
						temp.put("closed", temp.get("closed")+1);
				}
				if(correctionsList.size() >= 4){
					if(temp.get("closed") == 0)
						key = "OpenPOS4+";
					else if (temp.get("open") == 0)
						key = "ClosedPOS4+";
					else
						key = "MixedPOS4+";
					ArrayList<Integer> array = mapErrorTypeSeparated.get(key);
					array.set(0, array.get(0) + count_matched_labeled_one);
					array.set(1, array.get(1) + count_matched_unlabeled_one);
					array.set(2, array.get(2) + denominatorGood);
					array.set(3, array.get(3) + denominatorBad);
					array.set(4, array.get(4)+1);
					mapErrorTypeSeparated.put(key, array);
					
				} 
				if (correctionsList.size() >= 3){
					if(temp.get("closed") == 0)
						key = "OpenPOS3+";
					else if (temp.get("open") == 0)
						key = "ClosedPOS3+";
					else
						key = "MixedPOS3+";
					ArrayList<Integer> array = mapErrorTypeSeparated.get(key);
					array.set(0, array.get(0) + count_matched_labeled_one);
					array.set(1, array.get(1) + count_matched_unlabeled_one);
					array.set(2, array.get(2) + denominatorGood);
					array.set(3, array.get(3) + denominatorBad);
					array.set(4, array.get(4)+1);
					mapErrorTypeSeparated.put(key, array);
				} 
				if (correctionsList.size() == 3){
					if(temp.get("closed") == 0)
						key = "OpenPOS3";
					else if (temp.get("open") == 0)
						key = "ClosedPOS3";
					else
						key = "MixedPOS3";
					ArrayList<Integer> array = mapErrorTypeSeparated.get(key);
					array.set(0, array.get(0) + count_matched_labeled_one);
					array.set(1, array.get(1) + count_matched_unlabeled_one);
					array.set(2, array.get(2) + denominatorGood);
					array.set(3, array.get(3) + denominatorBad);
					array.set(4, array.get(4)+1);
					mapErrorTypeSeparated.put(key, array);
				}
			}
			
			// when there are more than x number of errors in the sentence, compare SRL role of errors
			if(correctionsList.size() >= 1){
				HashMap<String, Integer> temp = new HashMap<>(); temp.put("verb", 0); temp.put("args", 0); temp.put("none", 0);
				String key = "";
				for(int j=0; j< correctionsList.size(); j++){	
					String errorSRLposition = findErrorPositionInSRL(j, correctionsList, SRLmap);
					if (errorSRLposition.equals("Verbs")){
						temp.put("verb", temp.get("verb")+1);
					}else if (errorSRLposition.equals("Arguments")){
						temp.put("args", temp.get("args")+1);
					}else{
						temp.put("none", temp.get("none")+1);
					}
				}
				if(correctionsList.size() >= 4){
					if(temp.get("args") == 0 && temp.get("none") == 0)
						key = "srlVerb4+";
					else if (temp.get("verb") == 0 && temp.get("none") == 0)
						key = "srlArgs4+";
					else if (temp.get("verb") == 0 && temp.get("args") == 0)
						key = "srlNone4+";
					else
						key = "srlMixed4+";
					ArrayList<Integer> array = mapErrorTypeSeparated.get(key);
					array.set(0, array.get(0) + count_matched_labeled_one);
					array.set(1, array.get(1) + count_matched_unlabeled_one);
					array.set(2, array.get(2) + denominatorGood);
					array.set(3, array.get(3) + denominatorBad);
					array.set(4, array.get(4)+1);
					mapErrorTypeSeparated.put(key, array);
					
				} 
				if (correctionsList.size() >= 3){
					if(temp.get("args") == 0 && temp.get("none") == 0)
						key = "srlVerb3+";
					else if (temp.get("verb") == 0 && temp.get("none") == 0)
						key = "srlArgs3+";
					else if (temp.get("verb") == 0 && temp.get("args") == 0)
						key = "srlNone3+";
					else
						key = "srlMixed3+";
					ArrayList<Integer> array = mapErrorTypeSeparated.get(key);
					array.set(0, array.get(0) + count_matched_labeled_one);
					array.set(1, array.get(1) + count_matched_unlabeled_one);
					array.set(2, array.get(2) + denominatorGood);
					array.set(3, array.get(3) + denominatorBad);
					array.set(4, array.get(4)+1);
					mapErrorTypeSeparated.put(key, array);
				} 
				if (correctionsList.size() == 3){
					if(temp.get("args") == 0 && temp.get("none") == 0)
						key = "srlVerb3";
					else if (temp.get("verb") == 0 && temp.get("none") == 0)
						key = "srlArgs3";
					else if (temp.get("verb") == 0 && temp.get("args") == 0)
						key = "srlNone3";
					else
						key = "srlMixed3";
					ArrayList<Integer> array = mapErrorTypeSeparated.get(key);
					array.set(0, array.get(0) + count_matched_labeled_one);
					array.set(1, array.get(1) + count_matched_unlabeled_one);
					array.set(2, array.get(2) + denominatorGood);
					array.set(3, array.get(3) + denominatorBad);
					array.set(4, array.get(4)+1);
					mapErrorTypeSeparated.put(key, array);
				}
			}			

			
			//when there are more errors in the sentence, check if all the errors are the same type of error
			HashMap<String, Integer> temp = new HashMap<>(); temp.put("verb", 0); temp.put("args", 0); temp.put("none", 0);
			String key = "";
			for(int j=0; j< correctionsList.size(); j++){	
				String errorSRLposition = findErrorPositionInSRL(j, correctionsList, SRLmap);
				if (errorSRLposition.equals("Verbs")){
					temp.put("verb", temp.get("verb")+1);
				}else if (errorSRLposition.equals("Arguments")){
					temp.put("args", temp.get("args")+1);
				}else{
					temp.put("none", temp.get("none")+1);
				}
			}
			if(temp.get("args") == 0 && temp.get("none") == 0)
				key = "srlVerbAll";
			else if (temp.get("verb") == 0 && temp.get("none") == 0)
				key = "srlArgsAll";
			else if (temp.get("verb") == 0 && temp.get("args") == 0)
				key = "srlNoneAll";
			else
				key = "srlMixedAll";
			ArrayList<Integer> array = mapErrorTypeSeparated.get(key);
			array.set(0, array.get(0) + count_matched_labeled_one);
			array.set(1, array.get(1) + count_matched_unlabeled_one);
			array.set(2, array.get(2) + denominatorGood);
			array.set(3, array.get(3) + denominatorBad);
			array.set(4, array.get(4)+1);
			mapErrorTypeSeparated.put(key, array);
			
			//when there are more errors in the sentence, check if majority of type of errors
			key = "";
			if(temp.get("verb") > temp.get("args") && temp.get("verb") > temp.get("none"))
				key = "srlVerbMajority";
			else if (temp.get("args") > temp.get("verb") && temp.get("args") > temp.get("none"))
				key = "srlArgsMajority";
			else if (temp.get("none") > temp.get("verb") && temp.get("none") > temp.get("args"))
				key = "srlNoneMajority";
			else
				key = "srlMixedMajority";
			array = mapErrorTypeSeparated.get(key);
			array.set(0, array.get(0) + count_matched_labeled_one);
			array.set(1, array.get(1) + count_matched_unlabeled_one);
			array.set(2, array.get(2) + denominatorGood);
			array.set(3, array.get(3) + denominatorBad);
			array.set(4, array.get(4)+1);
			mapErrorTypeSeparated.put(key, array);
			
			
			//write to robustness result file
			outFile.write(unlabeled_fscore_one + "\t" + errorNumber + "\t" + sentBad + "\t" + sentGood + "\t" + avgErrorDistance +  "\n");
			
		}		
		System.out.println("parser: " + treeFile1);
		System.out.println("# of sentences: " + count);
		System.out.println("matched: " + count_matched_labeled);
		System.out.println("un-matched: " + count_matched_unlabeled);
		System.out.println("count_all_goodsent_dependencies: " + count_all_goodsent_dependencies);
		System.out.println("count_all_badsent_dependencies: " + count_all_badsent_dependencies);
		if(corpusType.equals("MT")){
			System.out.println("*************** Using Edit Distance to find errors ");
		}
		if(ignorePunctuation)
			System.out.println("****************** it ignored punctionas ****************");
		
		// calculate Robustness F-score both labeled and unlabeled
		double precision_labeled = (double) count_matched_labeled/(double) count_all_badsent_dependencies;
		double recall_labeled = (double) count_matched_labeled/(double) count_all_goodsent_dependencies;
		double f_score_labeled = 2*(precision_labeled * recall_labeled)/(precision_labeled + recall_labeled);
		System.out.println("Precision/Recall based on each error type: \n # of Sents\tPrecision\tRecall\tF-score\n");
		
		double precision_unlabeled = (double) count_matched_unlabeled/(double) count_all_badsent_dependencies;
		double recall_unlabeled = (double) count_matched_unlabeled/(double) count_all_goodsent_dependencies;
		double f_score_unlabeled = 2*(precision_unlabeled * recall_unlabeled)/(precision_unlabeled + recall_unlabeled);
		System.out.println("All errors\t" + count + "\t" + precision_labeled*100 + "\t" + recall_labeled*100 + "\t" + f_score_labeled*100 + "\t" + precision_unlabeled*100 + "\t" + recall_unlabeled*100 + "\t" + f_score_unlabeled*100);
		
		//print precision/recall based on each error number
		for (int i=1 ; i<=mapErrorNumberSeparated.size(); i++) { //mapErrorTypeSeparated.keySet()
			ArrayList<Integer> array = mapErrorNumberSeparated.get(i);
			precision_labeled = (double) array.get(0) /(double) array.get(3);
			recall_labeled = (double) array.get(0)/(double) array.get(2);
			f_score_labeled = 2*(precision_labeled * recall_labeled)/(precision_labeled + recall_labeled);
			
			precision_unlabeled = (double) array.get(1) /(double) array.get(3);
			recall_unlabeled = (double) array.get(1)/(double) array.get(2);
			f_score_unlabeled = 2*(precision_unlabeled * recall_unlabeled)/(precision_unlabeled + recall_unlabeled);
			
			System.out.println("# of error " + i +  "\t" + array.get(4) + "\t" + precision_labeled*100 + "\t" + recall_labeled*100 + "\t" + f_score_labeled*100 + "\t" + precision_unlabeled*100 + "\t" + recall_unlabeled*100 + "\t" + f_score_unlabeled*100);
		}
		
		System.out.println();
		//print precision/recall based on each error type
		List<String> names = Arrays.asList("Replacement", "Missing", "Unnecessary", "OpenPOS", "ClosedPOS", 
				"ReplacementOpenPOS", "ReplacementClosedPOS", "MissingOpenPOS", "MissingClosedPOS", "UnnecessaryOpenPOS", "UnnecessaryClosedPOS", 
				"errDistance0", "errDistance1", "errDistance2" , "errDistance3", "errDistance4" , "errDistance5", "errDistance6", "errDistance7", "errDistance8", "errDistance9", "errDistance10", 
				"err2nearDistance", "err2farDistance", "err3nearDistance", "err3farDistance",
				"srlVerb1", "srlArgs1", "srlNone1", 
				"srlArgs1A0",  "srlArgs1A1", "srlArgs1A2", "srlArgs1AM-Mod", "srlArgs1AM-TMP", "srlArgs1AM-ADV", "srlArgs1AM-DIS", "srlArgs1AM-MNR", "srlArgs1AM-NEG", "srlArgs1AM-LOC", "srlArgs1AM-REC", "srlArgs1AM-CAU", "srlArgs1AM-PNC", "srlArgs1R-A1", "srlArgs1C-A1", "srlArgs1other",
				"srlVerbAll", "srlArgsAll", "srlNoneAll", "srlMixedAll",
				"srlVerbMajority", "srlArgsMajority", "srlNoneMajority", "srlMixedMajority",
				"srlVerb4+", "srlArgs4+", "srlNone4+", "srlMixed4+",
				"srlVerb3+", "srlArgs3+", "srlNone3+", "srlMixed3+",
				"srlVerb3", "srlArgs3", "srlNone3", "srlMixed3",
				"OpenPOS4+", "ClosedPOS4+", "MixedPOS4+",
				"OpenPOS3+", "ClosedPOS3+", "MixedPOS3+",
				"OpenPOS3", "ClosedPOS3", "MixedPOS3",
				"Replacement4+", "Missing4+", "Unnecessary4+", "EditMixed4+",
				"Replacement3+", "Missing3+", "Unnecessary3+", "EditMixed3+",
				"Replacement3", "Missing3", "Unnecessary3", "EditMixed3"
				);
	
		for (String key : names){ //because I want them sorted
			ArrayList<Integer> array = mapErrorTypeSeparated.get(key);
			precision_labeled = (double) array.get(0) /(double) array.get(3);
			recall_labeled = (double) array.get(0)/(double) array.get(2);
			f_score_labeled = 2*(precision_labeled * recall_labeled)/(precision_labeled + recall_labeled);
			
			precision_unlabeled = (double) array.get(1) /(double) array.get(3);
			recall_unlabeled = (double) array.get(1)/(double) array.get(2);
			f_score_unlabeled = 2*(precision_unlabeled * recall_unlabeled)/(precision_unlabeled + recall_unlabeled);
			
			String out =  key +  "\t" + array.get(4) + "\t" + precision_labeled*100 + "\t" + recall_labeled*100 + "\t" + f_score_labeled*100 + "\t" + precision_unlabeled*100 + "\t" + recall_unlabeled*100 + "\t" + f_score_unlabeled*100;
			out = out.replace("NaN", "0");
			System.out.println(out);
		}
		
		
		outFile.close();
	}


	private static HashMap<String, Integer> makeEmptyMapErrorStatisticsSeperated() {
		HashMap<String, Integer> mapErrorStatisticsSeparated = new HashMap<>();
		mapErrorStatisticsSeparated.put("allOpenPOS", 0);
		mapErrorStatisticsSeparated.put("allClosedPOS", 0);
		mapErrorStatisticsSeparated.put("allReplacement", 0);
		mapErrorStatisticsSeparated.put("allMissing", 0);
		mapErrorStatisticsSeparated.put("allUnnecessary", 0);
		mapErrorStatisticsSeparated.put("allSRLVerb", 0);
		mapErrorStatisticsSeparated.put("allSRLArgs", 0);
		mapErrorStatisticsSeparated.put("allSRLNone", 0);
		return mapErrorStatisticsSeparated;
	}

	/**
	 * look at the POS and check what is its group: Closed or open
	 * @param pos
	 * @return
	 */
	private static String isOpenOrClosedPOS(String pos) {
		String key = "";
		if(pos.startsWith("N") || pos.startsWith("V") || pos.startsWith("J") || pos.startsWith("RB")){  //TODO: customize tags for twitter tagset || pos.startsWith("R") || pos.equals("^") || pos.equals("A")
			key = "OpenPOS";
		}else{
			key = "ClosedPOS";
		}
		return key;
	}
	
	public static List<ErrorCorrectionBean> makeCorrectionsList(String corrections) {
		List<ErrorCorrectionBean> correctionsList = new ArrayList<ErrorCorrectionBean>();
		String[] allCorrections = corrections.split("\'\\), \\(\'");
		for(int i=0; i<allCorrections.length; i++){
			String[] parts = allCorrections[i].split(",", 4);
			String type = parts[0].trim().replace("(", "").replace("'", "");
			int start = Integer.parseInt(parts[1].trim());
			int end = Integer.parseInt(parts[2].trim());
			String corr_to = parts[3].replace(" u'","").replace("')", "");
			ErrorCorrectionBean obj = new ErrorCorrectionBean(type, start, end, corr_to);
			correctionsList.add(obj);
		}
		return correctionsList;
	}

	/**
	 * fill correction list using TER alignments
	 * @param alignment
	 * @param wordsBad 
	 * @param wordsGood 
	 * @return
	 */
	private static List<ErrorCorrectionBean> fillCorrectionsListWithAlignments(char[] alignment, HashMap<Integer, String> wordsGood, HashMap<Integer, String> wordsBad) {
		List<ErrorCorrectionBean> correctionsList = new ArrayList<ErrorCorrectionBean>();
		int noMissing = 0;
		int noExtra = 0;
		int start = 0;
		String corr_to = "";
		for(int i=0; i<alignment.length; i++){
			corr_to = "";
			String type = String.valueOf(alignment[i]);
			if(type.equals(" "))
				continue;
			if(type.equals("D")){
				noMissing++;
				start = i;		
			} else if (type.equals("I")){ 
				noExtra++;
			}
			if(!type.equals("D"))
				start = i-noMissing;
			if(!type.equals("I"))
				corr_to = wordsGood.get(i + 1 - noExtra);
			int end = i;
			ErrorCorrectionBean obj = new ErrorCorrectionBean(type, start, end, corr_to);
			correctionsList.add(obj);
		}
		return correctionsList;
	}


	/**
	 * Find error distance in the sentence then average them
	 * for example if there are errors in the positions of 3,5,8 then the error distances are 2,3 and their average is 2.5
	 * @param correctionsList
	 * @return
	 */
	private static double getAverageErrorDistance(List<ErrorCorrectionBean> correctionsList) {
		double avg;
		double[] positionList = new double[correctionsList.size()];
		//get error positions
		for(int i=0; i<correctionsList.size(); i++){
			int position = correctionsList.get(i).getStart();
			positionList[i] = position;
		}
		//sort array 
		Arrays.sort(positionList);
		
		//find error distance
		double sum = 0.0;
		double[] errorDistanceList = new double[positionList.length-1];
		for(int i=1; i<positionList.length; i++){
			errorDistanceList[i-1] = positionList[i] - positionList[i-1];
			sum = sum + errorDistanceList[i-1];
		}
		
		//find average distance
		avg = sum/errorDistanceList.length;
		return avg;
	}


	/**
	 * Find error distance in the sentence
	 * for example if there are errors in the positions of 3,5,8 then the error distances are 2,3
	 * @param correctionsList
	 * @return
	 */
	private static double[] getErrorDistance(List<ErrorCorrectionBean> correctionsList) {
		double[] positionList = new double[correctionsList.size()];
		//get error positions
		for(int i=0; i<correctionsList.size(); i++){
			int position = correctionsList.get(i).getStart();
			positionList[i] = position;
		}
		//sort array 
		Arrays.sort(positionList);
		return positionList;
	}
	
	/**
	 * read a sentence POS file with this format:
	 * w1/pos w2/pos ...
	 * find the POS for each word and put it in a hashmap
	 * @param readLine
	 * @return
	 */
	private static HashMap<String, String> readPOStags(String line) {
		HashMap<String, String> posMap = new HashMap<String, String>();
		String[] parts = line.split(" ");
		for(int i=0; i< parts.length ; i++){
			String word = parts[i].substring(0, parts[i].lastIndexOf("/"));
			String pos = parts[i].substring(parts[i].lastIndexOf("/") + 1);
			posMap.put(word, pos);
		}			
		return posMap;
	}


	/**
	 *  load SRL relations from the file. the SRL format is like:
	 *  Are they included in the prize ?
	 *  Are	(0, 1)|||A1	they	(1, 2)
	 *  included	(2, 3)|||AM-LOC	in the prize	(3, 6)
	 * @param br
	 * @return
	 * @throws IOException 
	 */
	private static HashMap<String, HashMap<String, List<Integer>>> readSRLrelations(BufferedReader br) throws IOException {
		HashMap<String, HashMap<String, List<Integer>>> mapSRL = new HashMap<String, HashMap<String,List<Integer>>>();
		String line;
		line = br.readLine();
		line = br.readLine();
		HashMap<String, List<Integer>> mapVerbs = new HashMap<String, List<Integer>>();
		HashMap<String, List<Integer>> mapArgs = new HashMap<String, List<Integer>>();
		while(line != null && line.length() != 0){
			if(line.equals("null")){
				line = br.readLine();
				break;
			}
			String[] parts = line.split("\\|\\|\\|");
			String[] verbList = parts[0].split("\t");
			mapVerbs.put(verbList[0], getIntegerOfPairs(verbList[1])); 
			
			// read all the arguments for the given verb
			for(int i=1 ; i<parts.length ; i++){
				String[] argList = parts[i].split("\t");
				mapArgs.put(argList[0] + "\t" + argList[1], getIntegerOfPairs(argList[2])); 
			}
			// read the next line to see if it is a new verb and relations
			line = br.readLine();
		}
		mapSRL.put("Verbs", mapVerbs);
		mapSRL.put("Arguments", mapArgs);
		return mapSRL;
	}

	/***
	 * find out where is the SRL error based on the position of the error and position of SRL relations
	 * @param correctionsList
	 * @param sRLmap
	 * @return
	 */
	private static String findErrorPositionInSRL(int i, List<ErrorCorrectionBean> correctionsList, HashMap<String, HashMap<String, List<Integer>>> SRLmap) {
		String errorSRLposition = "";
		if (isErrorInSRLMap(correctionsList.get(i), SRLmap.get("Verbs")))
			errorSRLposition = "Verbs";
		else if (isErrorInSRLMap(correctionsList.get(i), SRLmap.get("Arguments")))
			errorSRLposition = "Arguments";
		else
			errorSRLposition = "None";
		return errorSRLposition;
	}
	
	private static String findErrorPositionInSRLArgument(int i, List<ErrorCorrectionBean> correctionsList, HashMap<String, HashMap<String, List<Integer>>> SRLmap) {
		String errorSRLargument = "";
		for(String key: SRLmap.get("Arguments").keySet()){
			List<Integer> positions = SRLmap.get("Arguments").get(key);
			if(correctionsList.get(i).getStart() >= positions.get(0) && correctionsList.get(i).getEnd() <= positions.get(1)){ //if the error is in the span of the SRL entity
				String[] parts = key.split("\t");
				errorSRLargument = parts[0];
				return errorSRLargument;
			}
		}
		return errorSRLargument;
	}
	
	private static boolean isErrorInSRLMap(ErrorCorrectionBean error, HashMap<String, List<Integer>> map) {
		for(String key: map.keySet()){
			List<Integer> positions = map.get(key);
			if(error.getStart() >= positions.get(0) && error.getEnd() <= positions.get(1)) //if the error is in the span of the SRL entity
				return true;
		}
		return false;
	}

	/**
	 *  get a string like (0, 1) and convert it to a list of integer numbers 0,1
	 * @param string
	 * @return
	 */
	private static List<Integer> getIntegerOfPairs(String str) {
		str = str.replace("(", "").replace(")", "");
		String[] parts = str.split(", ");
		int start = Integer.parseInt(parts[0]);
		int end = Integer.parseInt(parts[1]);
		return new ArrayList<Integer>(Arrays.asList(start, end));
	}

	/**
	 * Check whether the index of the bad sentence is in the start of a correction
	 * @param i
	 * @param correctionsList
	 * @return
	 */
	private static List<ErrorCorrectionBean> findCorrectionType(int i, List<ErrorCorrectionBean> correctionsList) {
		List<ErrorCorrectionBean> specificCorrections = new ArrayList<ErrorCorrectionBean>();
		for(ErrorCorrectionBean correction: correctionsList){
			if (correction.getStart() == i)
				specificCorrections.add(correction);
		}
		return specificCorrections;
	}
	
	/**
	 * find number of corrections that has the missing type
	 * @param correctionsList
	 * @return
	 */
	private static int findNumberOfMissingWords(List<ErrorCorrectionBean> correctionsList) {
		int missing = 0;
		for(ErrorCorrectionBean correction: correctionsList){
			if (correction.getType().startsWith("M"))
				missing++;
		}
		return missing;
	}
	
	
}

