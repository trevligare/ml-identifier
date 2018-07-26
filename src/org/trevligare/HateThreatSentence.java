package org.trevligare;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.*;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.sequences.DocumentReaderAndWriter;
import edu.stanford.nlp.sequences.SeqClassifierFlags;
import edu.stanford.nlp.util.Triple;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;

import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Iterator;
import java.util.Collections;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.io.IOException;

/** This is a demo of calling CRFClassifier programmatically.
 *  <p>
 *  Usage: {@code java -mx400m -cp "*" NERDemo [serializedClassifier [fileName]] }
 *  <p>
 *  If arguments aren't specified, they default to
 *  classifiers/english.all.3class.distsim.crf.ser.gz and some hardcoded sample text.
 *  If run with arguments, it shows some of the ways to get k-best labelings and
 *  probabilities out with CRFClassifier. If run without arguments, it shows some of
 *  the alternative output formats that you can get.
 *  <p>
 *  To use CRFClassifier from the command line:
 *  </p><blockquote>
 *  {@code java -mx400m edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier [classifier] -textFile [file] }
 *  </blockquote><p>
 *  Or if the file is already tokenized and one word per line, perhaps in
 *  a tab-separated value format with extra columns for part-of-speech tag,
 *  etc., use the version below (note the 's' instead of the 'x'):
 *  </p><blockquote>
 *  {@code java -mx400m edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier [classifier] -testFile [file] }
 *  </blockquote>
 *
 *  @author Jenny Finkel
 *  @author Christopher Manning
 */

public class HateThreatSentence {
  public Properties props = new Properties();
  NERTagger ner = null;
  DependencyTree depTree = null;
  List<String> words = null;

  public List<String> identifyHateThreatWords(List<DependencyTree.MaltTaggedToken> depTree, String sentence) {
    List<String> existingHateWords = new ArrayList<>();
    for (String word : words) {
      if (findWordInDepTree(depTree, word) != null) existingHateWords.add(word);
    }
    return existingHateWords;
  }
  public List<Pair<String, Double>> identifyPersons(String sentence) {
    return ner.getHighestProbabilityOfTag(sentence, "PERSON");
  }

  public List<DependencyTree.MaltTaggedToken> constructDependencyTree(String sentence) {
    try {
      return depTree.parseSentences(sentence).get(0);
    } catch(IOException ex) {
      System.err.println(ex);
      System.err.println("IOException trying to construct dependency tree of sentence: " + sentence);
    }
    return null;
  }
  public void removeNegations(String sentence, List<DependencyTree.MaltTaggedToken> depTree, List<String> hateThreats) {
  }

  public DependencyTree.MaltTaggedToken findWordInDepTree(List<DependencyTree.MaltTaggedToken> depTree, String word) {
    for(DependencyTree.MaltTaggedToken t : depTree) {
      if (word.equals(t.getLemma()) || word.equals(t.getWord()) || word.equals(t.getRawWord())) {
        // Might be more than one match of the word?
        return t;
      }
    }
    return null;
  }

  public double isHateThreatSentence(String sentence)  {
    List<Pair<String, Double>> persons = identifyPersons(sentence);
    List<DependencyTree.MaltTaggedToken> sentenceDepTree = constructDependencyTree(sentence);
    List<String> hateThreats = identifyHateThreatWords(sentenceDepTree, sentence);
    for(Pair<String, Double> word : persons) {
      DependencyTree.MaltTaggedToken personToken = findWordInDepTree(sentenceDepTree, word.first());
      if (personToken == null) continue;
      for(String hateThreat : hateThreats) {
        DependencyTree.MaltTaggedToken hateToken = findWordInDepTree(sentenceDepTree, hateThreat);
        if (hateToken == null) continue;
        // Use right child of to avoid sentences about hate/threats towards objects (like "I hate strawberries"")
        if (personToken.isRightChildOf(hateToken)) {
          // System.out.println("Hate sentence found on hateword:" + hateThreat + ", person:" + word.first());
          return word.second();
        }
      }
    }
    return 0;
  }

  public HateThreatSentence() throws ClassNotFoundException, IOException {
    this("config.properties");
  }

  public HateThreatSentence(String config) throws ClassNotFoundException, IOException {
    try {
      props.load(new FileInputStream(config));
    } catch(IOException e) {}

    final String serializedNERModel = props.getProperty("model.ner", "stanford-ner-2018-02-27/classifiers/se-ner-model.ser.file-list-test.gz");
    final String serializedStaggerModel = props.getProperty("model.pos", "stagger/swedish.bin");
    final String serializedMCOModel = props.getProperty("model.mco", "swemalt-1.7.2.mco");

    ner = new NERTagger(serializedNERModel);
    depTree = new DependencyTree(serializedStaggerModel, serializedMCOModel);
    words = Files.readAllLines(Paths.get(props.getProperty("hate_dictionary", "data/tmp-output/training_hat.txt")));
  }

  public static void main(String[] args) throws ClassNotFoundException, IOException {
    String[] example = {"Good afternoon Rajat Raina, how are you today?",
                        "Bob hatar Alice!",
                        "I go to school at Stanford University, which is located in California. Johannes likes Swedish icecream in the summer." };
    HateThreatSentence hts = new HateThreatSentence();
    for(String sentence : example) {
      if (hts.isHateThreatSentence(sentence) > 0) {
        System.out.println("Hate sentence found:" + sentence);
      }
    }
  }
}
