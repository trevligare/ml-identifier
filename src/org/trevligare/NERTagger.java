package org.trevligare;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map.Entry;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ie.crf.CRFCliqueTree;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.util.Pair;

public class NERTagger {
  CRFClassifier<CoreLabel> classifier;
  public NERTagger(String serializedClassifier) throws IOException, ClassNotFoundException {
      classifier = CRFClassifier.getClassifier(serializedClassifier);
  }
  public List<Pair<String, Set<Entry<String, Double>>>> labelSentence(String sentence) {
    int globalPos = 0;
    List<Pair<String, Set<Entry<String, Double>>>> document = new ArrayList<>();
    // String fileContents = IOUtils.slurpFile(args[1]);
    List<List<CoreLabel>> out = classifier.classify(sentence);
    for (List<CoreLabel> labeledSentence : out) {
      CRFCliqueTree<String> cqt = classifier.getCliqueTree(labeledSentence);
      for (int pos = 0; pos < cqt.length(); pos++) {
        List<String> attrs = new ArrayList<>();
        document.add(Pair.makePair(labeledSentence.get(pos).originalText(), cqt.probs(pos).entrySet()));
        // document.add(Pair.makePair(labeledSentence.get(pos).lemma(), cqt.probs(pos).entrySet()));
      }
      break; // We only want one sentence so lets ignore the rest
    }
    return document;
  }
  public List<Pair<String, Double>> getHighestProbabilityOfTag(String sentence, String tag) {
    List<Pair<String, Set<Entry<String, Double>>>> document = this.labelSentence(sentence);
    List<Pair<String, Double>> probTag = new ArrayList<>();
    for (Pair<String, Set<Entry<String, Double>>> word : document) {
      double tagProb = -1;
      double maxProb = -1;
      for (Entry<String, Double> tagEntry : word.second()) {
        if (tagEntry.getKey().equals(tag) && tagProb < tagEntry.getValue()) tagProb = tagEntry.getValue();
        if (maxProb < tagEntry.getValue()) maxProb = tagEntry.getValue();
      }
      if (tagProb >= maxProb) {
        probTag.add(Pair.makePair(word.first(), tagProb));
      }
    }
    return probTag;
  }
}
