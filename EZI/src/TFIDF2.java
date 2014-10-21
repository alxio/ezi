import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

public class TFIDF2 {
	double INF = Double.MAX_VALUE;

	private ArrayList<TreeMap<String, Integer>> documents = new ArrayList<TreeMap<String, Integer>>();
	private ArrayList<TreeMap<String, Double>> TFIDF = new ArrayList<TreeMap<String, Double>>();
	// private ArrayList<TreeMap<String, Double>> TFIDF = new
	// ArrayList<TreeMap<String, Double>>();
	private TreeSet<String> terms = new TreeSet<String>();
	private TreeMap<String, Integer> query = new TreeMap<String, Integer>();
	private TreeMap<String, Double> IDF = new TreeMap<String, Double>();

	public static void main(String[] args) throws IOException {
		TFIDF2 t = new TFIDF2();
//		t.readTerms(args[0]);
//		t.printTerms();
		t.readDocuments("/home/alxio/workspace2/EZI/documents.txt");
		t.printDocuments();
//		t.readQuery("Algorithms Theory Implementation Application");
//		for (Entry<String, Integer> e : t.query.entrySet()) {
//			System.out.print(e.getKey() + " " + e.getValue() + "; ");
//		}
//		t.solveQuery();
	}

	// public void computeTFIDF() {
	// if (IDF.size() == 0) {
	// computeIDF();
	// }
	// computeTF();
	// TFIDF.clear();
	// for (int i = 0; i < documents.size(); i++) {
	// TreeMap<String, Double> tfs = TF.get(i);
	// TreeMap<String, Double> tfids = new TreeMap<String, Double>();
	//
	// for (Entry<String, Double> e : tfs.entrySet()) {
	// String term = e.getKey();
	// double tf = e.getValue();
	// double idf = IDF.get(term);
	// tfids.put(term, tf * idf);
	// }
	// TFIDF.add(tfs);
	// }
	// }

	private TreeMap<String, Double> computeTFIDF(TreeMap<String, Integer> document) {
		int max = 1;
		// get max
		for (Entry<String, Integer> e : document.entrySet()) {
			int count = e.getValue();
			if (max < count)
				max = count;
		}
		TreeMap<String, Double> tf = new TreeMap<String, Double>();
		// count TFs using max
		for (Entry<String, Integer> e : document.entrySet()) {
			String term = e.getKey();
			int count = e.getValue();
			double idf = IDF.get(term);
			tf.put(term, idf * count / max);
		}
		return tf;
	}

	private void computeTFIDFs() {
		TFIDF.clear();
		for (int i = 0; i < documents.size(); i++) {
			TFIDF.add(computeTFIDF(documents.get(i)));
		}
	}

	public void computeIDF() {
		IDF.clear();
		int n = documents.size();
		for (String term : terms) {
			int counter = 0;
			for (TreeMap<String, Integer> doc : documents) {
				if (doc.containsKey(term))
					counter++;
			}
			double value = counter == 0 ? INF : Math.log(((double) n) / counter);
			IDF.put(term, value);
		}
	}

	public void readTerms(String filename) throws IOException {
		ArrayList<ArrayList<String>> stemmedData = Stemmer.readFile(filename, false);
		if (stemmedData.size() != 1)
			throw new IllegalArgumentException("Bad file format!");
		documents.clear();
		terms.clear();
		IDF.clear();
		for (String term : stemmedData.get(0)) {
			terms.add(term);
		}
	}

	public void printTerms() {
		System.out.println("Keywords:");
		for (String term : terms) {
			System.out.print(term + " ");
		}
		System.out.println();
	}

	private void readTitles(String filename) throws IOException {
		docTitles.clear();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
		String line;
		boolean empty = true;
		while ((line = br.readLine()) != null) {
			if (empty)
				docTitles.add(line);
			empty = line.isEmpty();
		}
		br.close();
	}
	
	private ArrayList<String> docTitles = new ArrayList<String>();
	
	public void readDocuments(String filename) throws IOException {
		readTitles(filename);
		ArrayList<ArrayList<String>> stemmedDocs = Stemmer.readFile(filename, false);
		documents.clear();
		TFIDF.clear();
		IDF.clear();
		for (ArrayList<String> stemmed : stemmedDocs) {
			TreeMap<String, Integer> doc = new TreeMap<String, Integer>();
			for (String word : stemmed) {
				if (!doc.containsKey(word)) {
					doc.put(word, 1);
				} else {
					doc.put(word, doc.get(word) + 1);
				}
			}
			documents.add(doc);
		}
	}

	public void printDocuments() {
		System.out.println("Documents:");
		for (int i = 0; i < documents.size(); i++) {
			System.out.println("Document #" + i);
			System.out.println(docTitles.get(i));
			for (Entry<String, Integer> e : documents.get(i).entrySet()) {
				System.out.print(e.getKey() + " " + e.getValue() + "; ");
			}
			System.out.println();
		}
	}

	private void solveQuery() {
		TreeMap<String, Double> queryTFIDF = computeTFIDF(query);
		for (int i = 0; i < documents.size(); i++) {
			TreeMap<String, Double> documentTFIDF = TFIDF.get(i);
			
		}
	}

	public void handleQueries(String filename) throws IOException {
		ArrayList<ArrayList<String>> stemmedQueries = Stemmer.readFile(filename, false);
		for (ArrayList<String> basicQuery : stemmedQueries) {
			readQuery(basicQuery);
			solveQuery();
		}
	}

	public void handleQuery(String data) {
		readQuery(data);
		solveQuery();
	}

	private void readQuery(String data) {
		readQuery(Stemmer.read(data));
	}

	private void readQuery(ArrayList<String> basicQuery) {
		query.clear();
		for (String word : basicQuery) {
			if (!terms.contains(word))
				continue;
			if (!query.containsKey(word)) {
				query.put(word, 1);
			} else {
				query.put(word, query.get(word) + 1);
			}
		}
	}
}
