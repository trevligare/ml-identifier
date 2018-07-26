#!/usr/bin/python
import sys
import glob
from nltk.stem.snowball import SnowballStemmer

# tried stemming but didn't work
stemmer = SnowballStemmer("swedish")


gazetteers_folder = 'data/swedish-gazetteers/'
gazetteers_files = glob.glob(gazetteers_folder + '*.csv')
gazetteers_mapping = {
  'cities': 'LOCATION',
  'countries': 'LOCATION',
  'geonames_cities1000': 'LOCATION',
  'location': 'LOCATION',
  'streetnames': 'LOCATION',
  'swedish-cities': 'LOCATION',
  'swedish-countries': 'LOCATION',
  'swedish-provinces': 'LOCATION',
  'world-capitals': 'LOCATION',
  'organization': 'ORGANIZATION',
  'dbpedia_companies': 'ORGANIZATION',
  'ngos': 'ORGANIZATION',
  'swedish-sportsclub': 'ORGANIZATION',
  'surname': 'PERSON',
  'firstname': 'PERSON'
  # ,
  # 'hat-ord': 'MEAN'

}
gazetteers = {
  # stemmer.stem(word): gazetteers_mapping[file]
  word: gazetteers_mapping[file]
  for file, gaz_map in gazetteers_mapping.items()
  if gazetteers_folder + file + '.csv' in gazetteers_files
  for word in open(gazetteers_folder + file + '.csv', 'r').read().split('\n')
}

def get_gazetteers(word, default):
  if word in gazetteers:
    return gazetteers[word].upper()
  if word[:-1] in gazetteers:
    return gazetteers[word[:-1]].upper()
  return default

def update_old_arrs(old_arrs, arr):
  for i in range(len(old_arrs)-1):
    old_arrs[i] = old_arrs[i + 1]
  old_arrs[-1] = arr

def update_linked_words(old_arrs, arr, doc):
  for i in range(len(old_arrs)-1, 1, -1):
    if all(a != '0\n' for a in old_arrs[i:]):
      label = get_gazetteers(' '.join(a[0] for a in old_arrs[i:]) + ' ' + arr[0], arr[1][:-1]) + '\n'
      if label != arr[1]: #'LABEL\n':
        for j in range(1, len(old_arrs) + 1 - i, 1):
          doc[-j] = doc[-j].split('\t')[0] + '\t' + label
        arr[1] = label
        break

def gazetteers2trainingdata(sentence_csv_file, output_file_name, unkown_label_file_name):
  f1 = open(output_file_name % (0,), 'w')
  f2 = open(unkown_label_file_name, 'w')
  doc = []
  old_arrs = [['', '']] * 5
  unkown_labels = []
  with open(sentence_csv_file, 'r') as f:
    for idx, line in enumerate(f):
      arr = [a for a in line.split('\t') if a != ""]
      if len(arr) < 2:
        continue

      if arr[1] == 'LABEL\n':
        temp_word = arr[0].strip()  # stemmer.stem(arr[0])
        arr[0] = temp_word
        arr[1] = get_gazetteers(arr[0], arr[1][:-1]) + '\n'

      if len(doc) > len(old_arrs) and arr[1] != '0\n':
        update_linked_words(old_arrs, arr, doc)

      if old_arrs[-1][1] != '0\n' and arr[1] == 'LABEL\n':
        arr[1] = old_arrs[-1][1]

      if 'LABEL\n' == arr[1] and '\t'.join(arr) not in unkown_labels:
        unkown_labels.append('\t'.join(arr))

      doc.append('\t'.join(arr))
      if idx % 500 == 0:
        f1.write(''.join(doc[:-len(old_arrs)]))
        doc = ['\t'.join(a) for a in old_arrs]
        if idx % (900 * 1000) == 0:
          f1.close()
          output_file = output_file_name % (int(idx / (900*1000)),)
          print('Starting new file', output_file)
          f1 = open(output_file, 'w')

      update_old_arrs(old_arrs, arr)

  f2.write(''.join(unkown_labels))
  f1.write(''.join(doc))
  f1.close()
  f2.close()

if __name__ == '__main__':
  output_file_name = "data/tmp-output/training_gazetteers_%i.txt"
  gazetteers2trainingdata(sys.argv[1], output_file_name, 'data/tmp-output/unkown_LABEL_file.txt')
