import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class HMMViterbi {

	private static HashMap<String, Node> transitions;
	private static HashMap<String, Node> emissions;
	private static ArrayList<String> statess;
	private static HashSet<String> wordsAll;

	private static double[][] viterbi;
	private static int[][] vitPrev;
	static int states = 0;
	static int smoothed = 0;

	public static void main(String[] args) throws IOException {

		transitions = new HashMap<String, Node>();
		emissions = new HashMap<String, Node>();
		statess = new ArrayList<String>();
		wordsAll = new HashSet<String>();
		long start = 0;
		long elapsedTime = 0;

		System.out.println("=== Building The Model");
		start = System.nanoTime();
		buildModel();
		elapsedTime = System.nanoTime() - start;
		System.out.println("=== Building The Model Completed");
		System.out.println("Total time: " + elapsedTime);
		System.out.println();

		states = statess.size();

		System.out.println("=== Tagging the text");
		start = System.nanoTime();
		tagging();
		elapsedTime = System.nanoTime() - start;
		System.out.println("=== Tagging Completed");
		System.out.println("Total time: " + elapsedTime);
	}

	private static void leplaceSmoothing(String word) {
		Iterator it = emissions.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			Node tmp = (Node) pair.getValue();

			HashMap<String, Double> tmp2 = tmp.vals;
			Iterator it2 = tmp2.entrySet().iterator();
			int countwr = tmp2.size();
			int newcount = tmp.count + countwr + 1;

			while (it2.hasNext()) {
				Map.Entry pair2 = (Map.Entry) it2.next();
				tmp2.replace((String) pair2.getKey(),
						((double) pair2.getValue() * tmp.count) / newcount);
			}
			tmp2.put(word, (1.0 / newcount));
			wordsAll.add(word);
			tmp.count = newcount;
		}
		// System.out.println("\"" + word + "\" has been smoothed");
		smoothed++;
	}

	private static void tagging() throws IOException {
		int countWord = 0;
		int correctTag = 0;
		int cs = 0;

		File[] files = new File("Testing2").listFiles();
		PrintWriter wr = new PrintWriter("Output/output.txt");
		
		for (File file : files) {
			String path = "Testing2/" + file.getName();

			BufferedReader bf = new BufferedReader(new FileReader(path));
			String sentence = bf.readLine();

			while (sentence != null) {
				cs++;
				sentence = sentence.replaceAll("\t", "");
				String[] words = sentence.split(" ");

				viterbi = new double[states][words.length];
				vitPrev = new int[states][words.length];

				for (int i = 0; i < words.length; i++) {
					if (words[i].length() > 0) {

						String word = getWord(words[i]);
						if (!wordsAll.contains(word)) {
							leplaceSmoothing(word);
						}

						for (int j = 0; j < statess.size(); j++) {
							double score = vitScore(statess.get(j), word, i, j);
							viterbi[j][i] = score;
							// wr.print(score);
						}
						// wr.println(words[i]);
					}
				}

				String tag = "";
				int idx = 0;
				double max = 0;
				int n = words.length - 1;
				for (int i = 0; i < states; i++) {
					if (viterbi[i][n] > max) {
						max = viterbi[i][n];
						idx = i;
					}
				}
				for (int i = n; i >= 0; i--) {
					if (words[i].length() > 0) {
						String cek = statess.get(idx);
						String truetag = getTag(words[i]);
						countWord++;
						if (cek.equals(truetag)) {
							correctTag++;
						}

						tag = words[i] + "/" + cek + " " + tag;
						idx = vitPrev[idx][i];
					}
				}
				wr.println(tag);

				sentence = bf.readLine();
			}
		}
		wr.close();
		System.out.println("Total evaluate sentence: " + cs);
		System.out.println("Total smoothed words: " + smoothed);
		System.out.println("Total evaluate words: " + countWord);
		System.out.println("Total correct tag: " + correctTag);
		System.out.println("Correctness persentage: " + (((double) correctTag / countWord)*100) + "%");

	}

	private static double vitScore(String tag, String word, int idxWord,
			int idxTag) {
		double max = 0.0;
		double emProb = 0.0;
		Node emtag = emissions.get(tag);
		if (emtag != null) {
			if (emtag.vals.containsKey(word)) {
				emProb = emtag.vals.get(word);
			}
		}

		if (idxWord == 0) {
			double transProb = 0.0;
			Node trans = transitions.get("start");
			if (trans.vals.containsKey(tag)) {
				transProb = trans.vals.get(tag);
			}
			max = (1 * transProb * emProb);
			vitPrev[idxTag][idxWord] = 0;
		} else {

			for (int i = 0; i < statess.size(); i++) {
				String prevTag = statess.get(i);
				double prevVitS = viterbi[i][idxWord - 1];
				double transProb = 0.0;

				Node trans = transitions.get(prevTag);
				if (trans != null) {
					if (trans.vals.containsKey(tag)) {
						transProb = trans.vals.get(tag);
					}
				}

				double score = (prevVitS * transProb * emProb);
				if (score > max) {
					max = score;
					vitPrev[idxTag][idxWord] = i;
				}
			}
		}
		return max;
	}

	private static void buildModel() throws IOException {
		int count = 0;
		int count_word = 0;
		File[] files = new File("Training").listFiles();
		for (File file : files) {
			String path = "Training/" + file.getName();

			BufferedReader bf = new BufferedReader(new FileReader(path));
			String sentence = bf.readLine();

			while (sentence != null) {
				count++;
				String[] words = sentence.split(" ");

				String tagPrev = "start";
				for (int i = 0; i < words.length; i++) {
					if (words[i].length() > 1) {
						count_word++;

						String tag = getTag(words[i]);
						String word = getWord(words[i]);

						addTag(tagPrev, tag);
						addWord(tag, word);
						tagPrev = tag;
					}
				}
				sentence = bf.readLine();
			}
		}

		System.out.println("Total sentences: " + count);
		System.out.println("Total word: " + count_word);
		System.out.println("Unique tag: " + statess.size());
		System.out.println("Unique word: " + wordsAll.size());

		PrintWriter wr = new PrintWriter("transition.txt");
		Iterator it = transitions.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			Node tmp = (Node) pair.getValue();
			wr.println(tmp.name + " " + tmp.count);
			wr.println(tmp.countProbability());
		}
		wr.close();
		System.out.println("Transistion probability ready");

		wr = new PrintWriter("emissions.txt");
		it = emissions.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			Node tmp = (Node) pair.getValue();
			wr.println(tmp.name + " " + tmp.count);
			wr.println(tmp.countProbability());
		}
		wr.close();
		System.out.println("Emission probability ready");
	}

	private static void addTag(String prev, String tag) {
		if (transitions.containsKey(prev)) {
			Node check = transitions.get(prev);
			check.addVal(tag);
		} else {
			Node check = new Node(prev, tag);
			transitions.put(prev, check);
			statess.add(prev);
		}
	}

	private static void addWord(String tag, String word) {
		word = word.replaceAll("\t", "");
		if (!wordsAll.contains(word)) {
			wordsAll.add(word);
		}
		if (emissions.containsKey(tag)) {
			Node check = emissions.get(tag);
			check.addVal(word);
		} else {
			Node check = new Node(tag, word);
			emissions.put(tag, check);
		}
	}

	private static String getTag(String str) {
		int idx = str.lastIndexOf('/') + 1;
		return str.substring(idx);
	}

	private static String getWord(String str) {
		int idx = str.lastIndexOf('/');
		return str.substring(0, idx);
	}
}

class Node {

	String name;
	HashMap<String, Double> vals;
	int count;

	public Node(String name, String firstVal) {
		this.name = name;
		this.count = 0;
		vals = new HashMap<String, Double>();
		addVal(firstVal);
	}

	public void addVal(String val) {
		if (vals.containsKey(val)) {
			double curr = vals.get(val) + 1;
			vals.replace(val, curr);
		} else {
			vals.put(val, 1.0);
		}
		count++;
	}

	public String countProbability() {
		Iterator it = vals.entrySet().iterator();
		String result = "";
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			double curr = (double) pair.getValue();
			vals.replace((String) pair.getKey(), (curr / count));
			result += "	" + pair.getKey() + " = " + pair.getValue() + "\n";
		}
		return result;
	}
}
