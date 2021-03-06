import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;

public class TFIDFSol {
    Vector<String> db = new Vector<String>(); // the document collection
    TreeMap<String, Double> idfs = new TreeMap<String, Double>(); // idf value for each term in the vocabulary
    TreeMap<String, Set<Integer>> invertedFile = new TreeMap<String, Set<Integer>>(); // term -> docIds of docs containing the term
    Vector<TreeMap<String, Double>> tf = new Vector<TreeMap<String, Double>>(); // term x docId matrix with term frequencies

    public static void main(String [] args) {
    	TFIDFSol tfidf = new TFIDFSol();
    	tfidf.go();
    }

    private void go() {
        // init the database
        initDB("db.txt");

        // init global variables: tf, invertedFile, and idfs
        init();

        // print the database
        printDB();

        // idfs and tfs
        System.out.println("IDFs:");
        // print the vocabulary
        printVoc();
        
        System.out.println("\nTFs for Equations:");
        for (int i = 0; i < db.size(); i++)
        {
        	    System.out.println("Equations: doc " + i + " : " + getTF("Equations", i));
        }
    
        // similarities for different queries
        rank("Differential Equations");
    }

    // inits database from textfile
    private void initDB(String filename) {
        db.clear();
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            while (br.ready()) {
                String doc = br.readLine().trim();
                db.add(doc);
            }
        } catch (FileNotFoundException e) {
            System.out.println("No database available.");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // lists the vocabulary
    private void printVoc() {
        System.out.println("Vocabulary:");
        for (Map.Entry<String, Double> entry : idfs.entrySet()) {
            System.out.println(entry.getKey() + ", idf = " + entry.getValue());
        }
    }

    // lists the database
    private void printDB() {
        System.out.println("size of the database: " + db.size());
        for (int i = 0; i < db.size(); i++) {
            System.out.println("doc " + i + ": " + db.elementAt(i));
        }
        System.out.println("");
    }

    // calculates the similarity between two vectors
    // each vector is a term -> weight map
    private double similarity(TreeMap<String, Double> v1, TreeMap<String, Double> v2) {
        double sum = 0;
        // iterate through one vector
        for (Map.Entry<String, Double> entry : v1.entrySet()) {
            String term = entry.getKey();
            Double w1 = entry.getValue();
            // multiply weights if contained in second vector
            Double w2 = v2.get(term);
            if (w2 != null)            	
                sum += w1 * w2;
        }
        // TODO write the formula for computation of cosinus
        // note that v.values() is Collection<Double> that you may need to calculate length of the vector
        // take advantage of vecLength() function
        return 1;
    }

    // returns the length of a vector
    private double vecLength(Collection<Double> vec) {
        double sum = 0;
        for (Double d : vec) {
            sum += Math.pow(d, 2);
        }
        return Math.sqrt(sum);
    }

    // ranks a query to the documents of the database
    private void rank(String query) {
        System.out.println("");
        System.out.println("query = " + query);

        // get term frequencies for the query terms
        TreeMap<String, Double> termFreqs = getTF(query);

        // construct the query vector
        // the query vector
        TreeMap<String, Double> queryVec = new TreeMap<String, Double>();

        // iterate through all query terms
        for (Map.Entry<String, Double> entry : termFreqs.entrySet()) {
            String term = entry.getKey();
            //TODO compute tfidf value for terms of query
            double tfidf = 0;
            queryVec.put(term, tfidf);
        }

        // helper class to store a docId and its score
        class DocScore implements Comparable<DocScore> {
            double score;
            int docId;

            public DocScore(double score, int docId) {
                this.score = score;
                this.docId = docId;
            }

            public int compareTo(DocScore docScore) {
                if (score > docScore.score) return -1;
                if (score < docScore.score) return 1;
                return 0;
            }
        }


        Set<Integer> union;
        TreeSet<String> queryTerms = new TreeSet<String>(termFreqs.keySet());

        // from the inverted file get the union of all docIDs that contain any query term
        union = invertedFile.get(queryTerms.first());
        for (String term : queryTerms) {
            union.addAll(invertedFile.get(term));
        }

        // calculate the scores of documents in the union
        Vector<DocScore> scores = new Vector<DocScore>();
        for (Integer i : union) {
            scores.add(new DocScore(similarity(queryVec, getDocVec(i)), i));
        }

        // sort and print the scores
        Collections.sort(scores);
        for (DocScore docScore : scores) {
            System.out.println("score of doc " + docScore.docId + " = " + docScore.score);
        }
    }

    // returns the idf of a term
    private double idf(String term) {
        return idfs.get(term);
    }

    // calculates the document vector for a given docID
    private TreeMap<String, Double> getDocVec(int docId) {
        TreeMap<String, Double> vec = new TreeMap<String, Double>();

        // get all term frequencies
        TreeMap<String, Double> termFreqs = tf.elementAt(docId);

        // for each term, tf * idf
        for (Map.Entry<String, Double> entry : termFreqs.entrySet()) {
            String term = entry.getKey();
            //TODO compute tfidf value for a given term
            //take advantage of idf() function
            double tfidf = 0;
            vec.put(term, tfidf);
        }
        return vec;
    }

    // returns the term frequency for a term and a docID
    private double getTF(String term, int docId) {
        Double freq = tf.elementAt(docId).get(term);
        if (freq == null) return 0;
        else return freq;
    }

    // calculates the term frequencies for a document
    private TreeMap<String, Double> getTF(String doc) {
        TreeMap<String, Double> termFreqs = new TreeMap<String, Double>();
        double max = 0;

        // tokenize document
        StringTokenizer st = new StringTokenizer(doc, " ");

        // for all tokens
        while (st.hasMoreTokens()) {
            String term = st.nextToken();

            // count the max term frequency
            Double count = termFreqs.get(term);
            if (count == null) {
                count = new Double(0);
            }
            count++;
            termFreqs.put(term, count);
            if (count > max) max = count;
        }

        // normalize tf
        for (Double tf : termFreqs.values()) {
        	//TODO write the formula for normalization of TF
        	tf = 0.0;
        }
        return termFreqs;
    }

    // init tf, invertedFile, and idfs
    private void init() {
        int docId = 0;
        // for all docs in the database
        for (String doc : db) {
            // get the tfs for a doc
            TreeMap<String, Double> termFreqs = getTF(doc);

            // add to global tf vector
            tf.add(termFreqs);

            // for all terms
            for (String term : termFreqs.keySet()) {
                // add the current docID to the posting list
                Set<Integer> docIds = invertedFile.get(term);
                if (docIds == null) docIds = new TreeSet<Integer>();
                docIds.add(docId);
                invertedFile.put(term, docIds);
            }
            docId++;
        }

        // calculate idfs
        int dbSize = db.size();
        // for all terms
        for (Map.Entry<String, Set<Integer>> entry : invertedFile.entrySet()) {
            String term = entry.getKey();
            // get the size of the posting list, i.e. the document frequency
            int df = entry.getValue().size();
            //TODO write the formula for calculation of IDF 
            idfs.put(term, 0.0);
        }
    }
}

