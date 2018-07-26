from bs4 import BeautifulSoup, Tag
import os, sys

def get_label(word):
  return word.getText() + '\t'+ 'LABEL'

def get_blank(word):
  return word.getText() +'\t'+'0'

def collect_words(soup):
  train_sentence = []
  for word in soup.find_all('w'):
    if word['pos'] == 'PM':
      train_sentence.append(get_label(word))
    else:
      train_sentence.append(get_blank(word))
  return train_sentence

def xml2corenlp(xml_file, output_file):
  f1 = open(output_file, 'w')
  sentence = []
  train_sentence = []

  doc = []
  with open(xml_file, 'r') as f:
    for line in f:
      soup = BeautifulSoup(line, 'html.parser')
      train_sentence = train_sentence + collect_words(soup)

      if '</sentence>' in line:
        doc.append('\n'.join(train_sentence) + '\n')
        train_sentence = []

      if len(doc) == 500:
        f1.write(''.join(doc))
        doc = []

  f1.write(''.join(doc))
  f1.close()

if __name__ == '__main__':
  collect_sentences(sys.argv[1], 'data/tmp-output/training_corenlp.txt')
