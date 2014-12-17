import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;

public class TFIDF {
	private Scanner sc;

	double INF = Double.MAX_VALUE;

	private ArrayList<String> terms = new ArrayList<String>();
	private HashMap<String, Integer> termsIndex = new HashMap<String, Integer>();

	private ArrayList<String> docTitles = new ArrayList<String>();
	private ArrayList<float[]> documents = new ArrayList<float[]>();
	private ArrayList<double[]> TFIDF = new ArrayList<double[]>();
	private ArrayList<Double> TFIDFlen = new ArrayList<Double>();

	private double[] IDF = null;

	private int N = 0;

	public static void main(String[] args) throws Exception {
		TFIDF t = new TFIDF();
		if (args.length == 4) {
			t.readTerms(args[0]);
			t.readDocuments(args[1]);
			t.handleQueries(args[3]);
		} else if (args.length == 3) {
			t.readTerms(args[0]);
			t.readDocuments(args[1]);
			t.kMeans(Integer.parseInt(args[2]));
		} else if (args.length == 2) {
			System.out.println("Trying to read terms from " + args[0]);
			t.readTerms(args[0]);
			System.out.println("Done.");
			System.out.println("Trying to read documents from " + args[1]);
			t.readDocuments(args[1]);
			System.out.println("Done.");
			System.out.println("Entering interactive mode.");
			t.showHelp();
			t.loop();
		} else {
			System.out
					.println("Entering interactive mode. To use program in batch mode, enter paths to TERMS, DOCUMENTS 'q' and QUERIES in arguments.\n"
							+ "You can also enter TERMS and DOCUMENTS for pre-loading and futher interactive work."
							+ "NEW FEATURE!!!! You can also enter TERMS, DOCUMENTS and K to group into K groups.");
			t.showHelp();
			t.loop();
		}
	}

	private void loop() {
		sc = new Scanner(System.in);
		while (true) {
			String cmd = sc.next();
			String file;
			try {
				switch (cmd.charAt(0)) {
				case 'd':
					file = sc.next();
					readDocuments(file);
					break;
				case 't':
					file = sc.next();
					readTerms(file);
					break;
				case 's':
					printDocuments();
					break;
				case 'q':
					file = sc.next();
					handleQueries(file);
					break;
				case 'e':
					file = sc.nextLine();
					handleQuery(file);
					break;
				case 'r':
					file = sc.nextLine();
					handleQueryWithRevelant(file);
					break;
				case 'g':
					int k = Integer.parseInt(sc.nextLine().substring(1));
					kMeans(k);
					break;
				case 'x':
					return;
				default:
					showHelp();
				}
				System.out.println("Done.");
			} catch (Exception e) {
				System.out.println(e.toString());
			}
		}
	}

	private int[] getRandoms(int k, int max) {
		Random R = new Random();
		HashSet<Integer> used = new HashSet<Integer>();
		int[] data = new int[k];
		for (int i = 0; i < k; ++i) {
			int n;
			do
				n = R.nextInt(max);
			while (used.contains(n));
			used.add(n);
			data[i] = n;
		}
		Arrays.sort(data);
		return data;
	}

	private void kMeans(int k) throws Exception {
		if (k > documents.size())
			throw new Exception("K is larger then number of documents!");
		double[][] means = new double[k][terms.size()];
		double[] lens = new double[k];
		ArrayList<Integer> groups[] = null;
		{
			int[] start = getRandoms(k, documents.size());
			for (int i = 0; i < k; ++i) {
				means[i] = TFIDF.get(start[i]).clone();
				lens[i] = TFIDFlen.get(start[i]);
			}
		}
		int it = 0;
		while (true) {
			@SuppressWarnings("unchecked")
			ArrayList<Integer> newGroups[] = new ArrayList[k];
			for (int i = 0; i < k; ++i)
				newGroups[i] = new ArrayList<Integer>();

			for (int i = 0; i < documents.size(); ++i) {// foreach doc
				double[] doc = TFIDF.get(i);
				double len = TFIDFlen.get(i);
				int id = 0;
				double max = 0;
				for (int j = 0; j < k; ++j) {// foreach mean
					double sim = cosineSimilarity(doc, means[j], len, lens[j]);
					if (sim > max) {
						max = sim;
						id = j;
					}
				}
				newGroups[id].add(i);
			}
			boolean same = true;
			if (groups == null)
				same = false;
			else
				for (int i = 0; i < k; ++i) {
					if (groups[i].size() != newGroups[i].size()) {
						same = false;
						break;
					}
					for (int j = 0; j < groups[i].size(); ++j) {
						if (groups[i].get(j) != newGroups[i].get(j)) {
							same = false;
							break;
						}
					}
				}
			if (same)
				break;
			groups = newGroups;
			double J = 0;
			for (int i = 0; i < k; ++i) {
				double[] v = new double[N];
				for (Integer docid : groups[i]) {
					for(int j=0;j<N;++j){
						v[j] += TFIDF.get(docid)[j];
					}
				}
				if(groups[i].size() > 0)
					for(int j=0;j<N;++j){
						v[j] /= groups[i].size();
					}
				lens[i] = vectorLen(v);
				means[i] = v;
				for (Integer docid : groups[i]) {
					J += cosineSimilarity(TFIDF.get(docid), v, TFIDFlen.get(docid), lens[i]);
				}
			}
			++it;
			System.out.println("iteration " + it + " J = " + J);
		}
		System.out.println("k-means ended after " + it + " iterations");
		printGroups(groups);
	}

	private void printGroups(ArrayList<Integer>[] groups) {
		System.out.println("Total docs: " + documents.size());
		for (int i = 0; i < groups.length; ++i) {
			System.out.println("Group " + (i + 1) + " size: "
					+ groups[i].size());
			for (Integer docid : groups[i]) {
				System.out.println(docid + ": " + docTitles.get(docid));
			}
			System.out.println();
		}
	}

	private static final String help = "Available commands:\n"
			+ "t [file] : load terms\n"
			+ "d [file] : load documents\n"
			+ "s        : show stemmed documents\n"
			+ "q [file] : execute queries from file\n"
			+ "e [term1, term2, ...] : execute single query from stdin\n"
			+ "x        : exit\n"
			+ "r [term1, term2, ...] : execute sigle query from stdin with REVELANT FEEEDBACK support\n"
			+ "\nNEW COMMAND AVAIBLE!!!\n"
			+ "g [k]    : group documents into k groups";

	private void showHelp() {
		System.out.println(help);
	}

	private double[] computeTFIDF(float[] document) {
		float max = 1;
		// get max
		for (int i = 0; i < N; i++) {
			float count = document[i];
			if (max < count)
				max = count;
		}
		double[] tf = new double[N];
		// count TFs using max
		for (int i = 0; i < N; i++) {
			tf[i] = IDF[i] * document[i] / max;
		}
		return tf;
	}

	private void computeTFIDFs() {
		TFIDF.clear();
		for (int i = 0; i < documents.size(); i++) {
			double[] tfidf = computeTFIDF(documents.get(i));
			TFIDF.add(tfidf);
			TFIDFlen.add(vectorLen(tfidf));
		}
	}

	private void computeIDF() {
		IDF = new double[N];
		int n = documents.size();
		for (int i = 0; i < N; i++) {
			int counter = 0;
			for (float[] doc : documents) {
				if (doc[i] > 0)
					counter++;
			}
			IDF[i] = counter == 0 ? INF : Math.log(((double) n) / counter);
		}
	}

	public void readTerms(String filename) throws IOException {
		ArrayList<ArrayList<String>> stemmedData = Stemmer.readFile(filename,
				false);
		if (stemmedData.size() != 1)
			throw new IllegalArgumentException("Bad file format!");
		terms.clear();
		termsIndex.clear();
		documents.clear();
		docTitles.clear();
		int i = 0;
		for (String term : stemmedData.get(0)) {
			terms.add(term);
			termsIndex.put(term, i++);
		}
		N = terms.size();
	}

	public void printTerms() {
		System.out.println("Keywords:");
		for (String term : terms) {
			System.out.print(term + " ");
		}
		System.out.println();
		System.out.println();
	}

	private void readTitles(String filename) throws IOException {
		docTitles.clear();
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(filename)));
		String line;
		boolean empty = true;
		while ((line = br.readLine()) != null) {
			if (empty)
				docTitles.add(line);
			empty = line.isEmpty();
		}
		br.close();
	}

	public void readDocuments(String filename) throws IOException {
		if (terms.size() == 0) {
			throw new IllegalStateException("Load terms first.");
		}
		readTitles(filename);
		ArrayList<ArrayList<String>> stemmedDocs = Stemmer.readFile(filename,
				false);
		documents.clear();
		for (ArrayList<String> stemmed : stemmedDocs) {
			float[] doc = new float[N];
			for (String word : stemmed) {
				Integer index = termsIndex.get(word);
				if (index == null)
					continue;
				doc[index]++;
			}
			documents.add(doc);
		}
		computeIDF();
		computeTFIDFs();
	}

	public void printDocuments() {
		System.out.println("Documents:");
		for (int i = 0; i < documents.size(); i++) {
			System.out.println("#" + i + " " + docTitles.get(i));
			for (int j = 0; j < N; j++) {
				if (documents.get(i)[j] == 0)
					continue;
				System.out.print(terms.get(j));
				System.out.print(":  ");
				System.out.print(documents.get(i)[j]);
				System.out.print(",  ");
			}
			System.out.println();
			System.out.println();
		}
	}

	private static double vectorLen(double[] v) {
		double l = 0;
		for (int i = 0; i < v.length; i++) {
			l += v[i] * v[i];
		}
		return Math.sqrt(l);
	}

	private void solveAndPrint(float[] query) {
		TreeMap<Double, Integer> sorted = solveQuery(query);
		for (Entry<Double, Integer> e : sorted.entrySet()) {
			int id = e.getValue();
			double d = -e.getKey();
			if (d <= minValue)
				break;
			String name = docTitles.get(id);
			System.out.printf("%.4f", d);
			System.out.println(" \t" + name);
		}
		System.out.println();
	}

	private double cosineSimilarity(double[] v1, double[] v2, double len1,
			double len2) {
		double div = len1 * len2;
		if (div == 0) {
			return 0;
		} else {
			double[] sim = v1.clone();
			for (int i = 0; i < N; i++) {
				sim[i] *= v2[i];
			}
			return vectorLen(sim) / div;
		}
	}

	private TreeMap<Double, Integer> solveQuery(float[] query) {
		if (documents.size() == 0) {
			throw new IllegalStateException("Load documents first.");
		}
		double[] queryTFIDF = computeTFIDF(query);
		double queryLen = vectorLen(queryTFIDF);
		TreeMap<Double, Integer> sorted = new TreeMap<Double, Integer>();
		for (int i = 0; i < documents.size(); i++) {
			double s = cosineSimilarity(queryTFIDF, TFIDF.get(i), queryLen,
					TFIDFlen.get(i));
			sorted.put(-s, i);
		}
		return sorted;
	}

	public void handleQueries(String filename) throws IOException {
		ArrayList<ArrayList<String>> stemmedQueries = Stemmer.readFile(
				filename, true);
		for (ArrayList<String> basicQuery : stemmedQueries) {
			float[] query = readQuery(basicQuery);
			printQuery(query);
			solveAndPrint(query);
		}
	}

	public void handleQuery(String data) {
		float[] query = readQuery(data);
		printQuery(query);
		solveAndPrint(query);
	}

	private float[] readQuery(String data) {
		return readQuery(Stemmer.read(data));
	}

	private float[] readQuery(ArrayList<String> basicQuery) {
		float[] query = new float[N];
		for (String word : basicQuery) {
			Integer index = termsIndex.get(word);
			if (index == null)
				continue;
			query[index]++;
		}
		return query;
	}

	private void printQuery(float[] query) {
		System.out.print("Query: ");
		for (int i = 0; i < N; i++) {
			if (query[i] > 0)
				System.out.print(terms.get(i) + " ");
			if (query[i] > 0) {
				System.out.print("(" + query[i] + ") ");
			}
		}
		System.out.println();
	}

	static final float minValue = 0.1f;
	static final float a = 1;
	static final float b = 0.75f;
	static final float c = 0.15f;
	static final float maxPercent = 0.5f;

	private void handleQueryWithRevelant(String data) {
		float[] query = readQuery(data);
		printQuery(query);
		TreeMap<Double, Integer> sorted = solveQuery(query);
		int i = 0;

		float[] good = new float[N];
		float[] bad = new float[N];
		int badCount = 0;
		int goodCount = 0;

		for (Entry<Double, Integer> e : sorted.entrySet()) {
			if (-e.getKey() <= minValue)
				break;
			if (++i > sorted.size() * maxPercent)
				break;
			int id = e.getValue();
			double d = -e.getKey();
			String name = docTitles.get(id);
			System.out.printf("%.4f", d);
			System.out.println(" \t" + name);
			boolean ok = false;
			while (true) {
				System.out.println("Is it revelant? (y/n)");
				String cmd = sc.next();
				if (cmd.charAt(0) == 'y' || cmd.charAt(0) == 'Y') {
					ok = true;
					break;
				}
				if (cmd.charAt(0) == 'n' || cmd.charAt(0) == 'N') {
					ok = false;
					break;
				}
			}
			double[] doc = TFIDF.get(id);

			float[] tab = ok ? good : bad;
			if (ok)
				goodCount++;
			else
				badCount++;

			for (int j = 0; j < N; j++) {
				tab[j] += doc[j];
			}
		}
		for (int j = 0; j < N; j++) {
			query[j] = Math.max(0,
					a * query[j] + b * good[j] / Math.max(goodCount, 1) - c
							* bad[j] / Math.max(badCount, 1));
		}
		printQuery(query);
		solveAndPrint(query);
	}
}
