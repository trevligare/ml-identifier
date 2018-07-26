package org.trevligare;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import se.su.ling.stagger.CTBTagger;
import se.su.ling.stagger.EnglishTokenizer;
import se.su.ling.stagger.GenericTagger;
import se.su.ling.stagger.LatinTokenizer;
import se.su.ling.stagger.PTBTagger;
import se.su.ling.stagger.SUCTagger;
import se.su.ling.stagger.SwedishTokenizer;
import se.su.ling.stagger.TagNameException;
import se.su.ling.stagger.TaggedData;
import se.su.ling.stagger.Tagger;
import se.su.ling.stagger.Tokenizer;

import org.maltparser.MaltParserService;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.symbol.SymbolTable;

public class DependencyTree {
    public static boolean plainOutput = false;
    public static boolean extendLexicon = true;
    public static boolean hasNE = true;

    protected Tagger tagger = null;
    protected MaltParserService maltParser = null;
    protected Tokenizer tokenizer = null;
    List<String> positiveAdjAdv = null;
    List<String> negativeAdjAdv = null;

    List<String> positiveVerb = null;
    List<String> negativeVerb = null;

    public class MaltTaggedToken extends se.su.ling.stagger.TaggedToken {
        Integer treeDepth = null;
        DependencyStructure graph;
        public MaltTaggedToken(se.su.ling.stagger.Token tok, String id) {
            super(tok, id);
        }
        public MaltTaggedToken(se.su.ling.stagger.TaggedToken tok) {
            super(tok);
        }
        public String getLemma() {
            return this.lf;
        }
        public String getWord() {
            return this.textLower;
        }
        public String getRawWord() {
            return this.token.value;
        }
        public boolean isRightChildOf(MaltTaggedToken token) {
            try {
                SortedSet<org.maltparser.core.syntaxgraph.node.DependencyNode> stack = new TreeSet<>();
                stack.addAll(token.getDependencyNode(false).getRightDependents());
                org.maltparser.core.syntaxgraph.node.DependencyNode thisDependencyNode = this.getDependencyNode(false);
                while(!stack.isEmpty()) {
                    org.maltparser.core.syntaxgraph.node.DependencyNode el = stack.first();
                    if (el == thisDependencyNode) {
                        return true;
                    }
                    stack.remove(el);
                    stack.addAll(el.getRightDependents());
                }
                return false;
            } catch(MaltChainedException ex) {
                System.err.println("Error: " + ex.toString());
            }
            return false;
        }
        public boolean isLeftChildOf(MaltTaggedToken token) {
            try {
                SortedSet<org.maltparser.core.syntaxgraph.node.DependencyNode> stack = new TreeSet<>();
                stack.addAll(token.getDependencyNode(false).getLeftDependents());
                org.maltparser.core.syntaxgraph.node.DependencyNode thisDependencyNode = this.getDependencyNode(false);
                while(!stack.isEmpty()) {
                    org.maltparser.core.syntaxgraph.node.DependencyNode el = stack.first();
                    if (el == thisDependencyNode) {
                        return true;
                    }
                    stack.remove(el);
                    stack.addAll(el.getLeftDependents());
                }
                return false;
            } catch(MaltChainedException ex) {
                System.err.println("Error: " + ex.toString());
            }
            return false;
        }
        public void setGraph(DependencyStructure graph) throws MaltChainedException {
            this.graph = graph;
        }
        public Integer getRank() {
            if (graph != null) {
                try {
                    return getDependencyNode(true).getRank();
                } catch(MaltChainedException ex) {}
            }
            return null;
        }
        public Integer getTreeDepth() {
            if (graph != null) {
                try {
                    return getDependencyNode(true).getDependencyNodeDepth();
                } catch(MaltChainedException ex) {}
            }
            return null;
        }
        private org.maltparser.core.syntaxgraph.node.DependencyNode getDependencyNode(boolean validate) throws MaltChainedException {
            // "LEMMA" is a symbol of the word lemma
            // "FORM" is a symbol of the word itself
            SymbolTable sym = graph.getSymbolTables().getSymbolTable("ID");
            String tokenSymbol = this.id.split(":")[2];
            org.maltparser.core.syntaxgraph.node.DependencyNode token = graph.getDependencyNode(Integer.parseInt(tokenSymbol));
            if (!validate || (token.isLabeled() && token.getLabelSymbol(sym).equals(tokenSymbol))) {
                return token;
            }
            return null;
        }
    }
    public DependencyTree() {}
    public DependencyTree(String modelPath) {
        this(modelPath, "swemalt-1.7.2.mco");
    }
    public DependencyTree(String posModelPath, String parserModelPath) {
        this.loadMaltParserModel(parserModelPath);
        this.loadStaggerModel(posModelPath);
    }
    /**
     * Creates and returns a tokenizer for the given language.
     */
    private static Tokenizer getTokenizer(Reader reader, String lang) {
        Tokenizer tokenizer;
        switch (lang) {
            case "sv":
                tokenizer = new SwedishTokenizer(reader);
                break;
            case "en":
                tokenizer = new EnglishTokenizer(reader);
                break;
            case "any":
                tokenizer = new LatinTokenizer(reader);
                break;
            default:
                throw new IllegalArgumentException();
        }
        return tokenizer;
    }
    /**
     * Creates and returns a tagger for the given language.
     */
    private static Tagger getTagger(String lang, TaggedData td, int posBeamSize, int neBeamSize) {
        Tagger tagger = null;
        switch (lang) {
            case "sv":
                tagger = new SUCTagger(
                        td, posBeamSize, neBeamSize);
                break;
            case "en":
                tagger = new PTBTagger(
                        td, posBeamSize, neBeamSize);
                break;
            case "any":
                tagger = new GenericTagger(
                        td, posBeamSize, neBeamSize);
                break;
            case "zh":
                tagger = new CTBTagger(
                        td, posBeamSize, neBeamSize);
                break;
            default:
                System.err.println("Invalid language: "+lang);
                break;
        }
        return tagger;
    }

    /**
     * Open a buffered file reader for a file with UTF-8 encoding
     * @param name - path/name of the file
     * @return A BufferedReader for the given file path
     * @throws IOException
     */
    private static BufferedReader openUTF8File(String name) throws IOException {
        if(name.equals("-"))
            return new BufferedReader(
                new InputStreamReader(System.in, "UTF-8"));
        else if(name.endsWith(".gz"))
            return new BufferedReader(new InputStreamReader(
                        new GZIPInputStream(
                            new FileInputStream(name)), "UTF-8"));
        return new BufferedReader(new InputStreamReader(
                    new FileInputStream(name), "UTF-8"));
    }

    /**
     * Returns a textual based tableview of the tagged words and their relation
     * in a sentence
     * @param sentence - the sentence to analys and tag
     * @param sentIdx - the index that we will start at in the sentence
     * @return  - table format textual representation of the tagged sentence
     * @throws IOException
     */
    protected String sentenceToPOSTree(ArrayList<se.su.ling.stagger.Token> sentence, int sentIdx) throws IOException {
        if(tagger == null || tokenizer == null) return null;
        BufferedWriter writer;
        StringWriter sb = new StringWriter();
        writer = new BufferedWriter(sb);//
                //new OutputStreamWriter(System.out, "UTF-8"));
        se.su.ling.stagger.TaggedToken[] sent = new se.su.ling.stagger.TaggedToken[sentence.size()];
        String fileID = "Hej";
        if(tokenizer.sentID != null) {
            if(!fileID.equals(tokenizer.sentID)) {
                fileID = tokenizer.sentID;
                sentIdx = 0;
            }
        }
        for(int j=0; j<sentence.size(); j++) {
            se.su.ling.stagger.Token tok = sentence.get(j);
            String id;
            id = fileID + ":" + sentIdx + ":" + (1 + j) + ":" + tok.offset;
            sent[j] = new se.su.ling.stagger.TaggedToken(tok, id);
        }
        se.su.ling.stagger.TaggedToken[] taggedSent =
                tagger.tagSentence(sent, true, false);
        try {
            tagger.getTaggedData().writeConllSentence(
                    writer,
                    taggedSent,
                    plainOutput);

        } catch (TagNameException ex) {
            Logger.getLogger(DependencyTree.class.getName()).log(Level.SEVERE, null, ex);
        }
        writer.close();
        return sb.toString();
    }
    /**
     * Tagg an entire sentence with Part of speech tags
     * @param sentence - the sentence to tag
     * @param sentIdx - The index to start from in word indicies
     * @return A new tagged sentence
     * @throws IOException
     */
    public List<MaltTaggedToken> tagSentenceWithPOS(List<se.su.ling.stagger.Token> sentence, int sentIdx) throws IOException {
        if(tagger == null || tokenizer == null) return null;

        MaltTaggedToken[] sent = new MaltTaggedToken[sentence.size()];
        String fileID = "TaggedWord";
        if(tokenizer.sentID != null) {
            if(!fileID.equals(tokenizer.sentID)) {
                fileID = tokenizer.sentID;
                sentIdx = 0;
            }
        }
        for(int j = 0; j < sent.length; j++) {
            se.su.ling.stagger.Token tok = sentence.get(j);
            String id;
            id = fileID + ":" + sentIdx + ":" + (1 + j) + ":" + tok.offset;
            sent[j] = new MaltTaggedToken(tok, id);
        }
        se.su.ling.stagger.TaggedToken[] tokens = tagger.tagSentence(sent, true, false);
        for (int j = 0; j < tokens.length; j++) {
            sent[j] = new MaltTaggedToken(tokens[j]);
        }
        return Arrays.asList(sent);
    }
    /**
     * Parses multiple sentences in multiple files and converts the sentence to language specific rules
     * (swedish rules) @see SwedishRules.java
     * @param inputFiles - the files to read sentences from
     * @return an array of modified sentences based on language specific rules
     * @throws IOException
     */
    public List<List<MaltTaggedToken>> sentences2POSTaggs(String sentences) throws IOException {
        String lang = "sv";//tagger.getTaggedData().getLanguage();

        // TODO: experimental feature, might remove later
        tagger.setExtendLexicon(extendLexicon);
        if(!hasNE) tagger.setHasNE(false);

        int indexResult = 0;
        // BufferedReader reader = openUTF8File(inputFile);
        InputStream is = new ByteArrayInputStream(sentences.getBytes());
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        tokenizer = getTokenizer(reader, lang);
        ArrayList<se.su.ling.stagger.Token> sentence;
        int sentIdx = 0;
        List<List<MaltTaggedToken>> sentenceTree = new ArrayList<>();
        while((sentence=tokenizer.readSentence())!=null) {
            List<MaltTaggedToken> taggedSentence = tagSentenceWithPOS(sentence, sentIdx);
            sentenceTree.add(taggedSentence);
            sentIdx++;
        }
        tokenizer.yyclose();
        return sentenceTree;
    }
    public List<List<MaltTaggedToken>> POSTaggs2DependencyTree(List<List<MaltTaggedToken>> sentenceTree) {
        List<List<MaltTaggedToken>> depTree = new ArrayList<>();
        for (List<MaltTaggedToken> sentence : sentenceTree) {
            try {
                StringWriter writer = new StringWriter();
                tagger.getTaggedData().writeConllSentence(writer, sentence.toArray(new MaltTaggedToken[sentence.size()]), false);
                String[] tokens = writer.toString().split("\n");
                DependencyStructure graph = maltParser.parse(tokens);

                List<MaltTaggedToken> sentenceDepTree = new ArrayList<>();
                for (int i = 0; i < tokens.length; i++) {
                    MaltTaggedToken token = new MaltTaggedToken(sentence.get(i));
                    token.setGraph(graph);
                    sentenceDepTree.add(token);
                }
                depTree.add(sentenceDepTree);
            } catch (MaltChainedException | TagNameException | IOException ex) {
                depTree.add(null);
            }
        }
        return depTree;
    }

    public List<List<MaltTaggedToken>> parseSentences(String sentences) throws IOException {
        return POSTaggs2DependencyTree(sentences2POSTaggs(sentences));
    }

    /**
     * Loads a tagger from file this will mostly take long time to process so
     * do it once to make it performance friendly!
     * @param modelFile - the file that we want to load as a tagger model
     */
    public void loadStaggerModel(String modelFile) {
        try {
            try (ObjectInputStream modelReader = new ObjectInputStream(
                    new FileInputStream(modelFile))) {
                System.out.println( "Loading Stagger model ...");
                tagger = (Tagger)modelReader.readObject();
                System.out.println("Stagger model loaded");
            }
        }
        catch(ClassNotFoundException | IOException ex) {
            tagger = null;
            System.out.println(ex);
            System.err.println("Unable to load Stagger model " + modelFile);
        }
    }
    public void loadMaltParserModel(String modelFile) {
        try {
            if (maltParser != null) {
                maltParser.terminateParserModel();
                maltParser = null;
            }
            File f = new File(modelFile.replace(" ", ""));
            maltParser = new MaltParserService();
            // Inititalize the parser model modelFile and sets the working directory to '.' and sets the logging file to 'parser.log'
            String cwd = System.getProperty("user.dir");
            System.setProperty("user.dir", f.getAbsoluteFile().getParentFile().getAbsolutePath());
            maltParser.initializeParserModel("-c " + f.getName() /*+ " -w " + f.getParentFile().getAbsolutePath()*/ + " -m parse");
            System.setProperty("user.dir", cwd);
            System.out.println("Maltparser model loaded");

        } catch(MaltChainedException ex) {
            System.out.println(ex);
            System.err.println("Unable to load malt model " + modelFile);
            maltParser = null;
        }
    }

    public static void main(String[] args) throws IOException {
        DependencyTree mp = new DependencyTree();
        mp.loadStaggerModel("stagger/swedish.bin");

        Scanner sc = new Scanner(System.in);
        String line = "";
        while(!line.equals("exit"))  {
            System.out.print("> ");
            line = sc.nextLine();

            // Parse sentences from the file stagger/tst2.txt
            mp.parseSentences(line);
        }


//        ConcurrentDesendencyGraph outputGraph = null;
//        // Loading the Swedish model swemalt-mini
//        ConcurrentMaltParserModel model = null;
//        try {
//            URL swemaltMiniModelURL = new File(base + "maltparser-1.8.1/example1.mco").toURI().toURL();
//            model = ConcurrentMaltParserService.initializeParserModel(swemaltMiniModelURL);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        for(String tokensStr : res) {
//            System.out.println("Token from stagger:");
//            System.out.println(tokensStr);
//            System.out.println("\nToken generated with maltparser");
//
//            // Creates an array of tokens, which contains the Swedish sentence 'Samtidigt får du högsta sparränta plus en skattefri sparpremie.'
//            // in the CoNLL data format.
//            String[] tokens = tokensStr.split("\n");
//            try {
//                outputGraph = model.parse(tokens);
//            } catch (Exception e) {
//               e.printStackTrace();
//            }
//            System.out.println(outputGraph);
//        }
    }
}
