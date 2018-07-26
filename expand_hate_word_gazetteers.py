import requests
from html.parser import HTMLParser
import time
import pickle
import functools
import traceback

class SynonymWebsiteParser(HTMLParser):
  def __init__(self):
    super().__init__()
    self.synonyms = []
    self.is_a_tag = False
    self.has_synonyms_started = False
    self.exist_more_synonyms = True

  def has_class(self, attrs, value):
    return any(attr[0] == 'class' and attr[1] == value for attr in attrs)

  def handle_starttag(self, tag, attrs):
    if tag.lower() == 'div' and self.has_class(attrs, 'body'):
      self.has_synonyms_started = True
    if tag.lower() == 'a' and self.has_synonyms_started:
      self.is_a_tag = True
    if tag.lower() == 'strong' and self.has_synonyms_started:
      self.exist_more_synonyms = False

  def handle_endtag(self, tag):
    self.is_a_tag = False

  def handle_data(self, data):
    if 'Inga synonymer hittades för din sökning' in data:
      self.exist_more_synonyms = False
    if self.is_a_tag and self.exist_more_synonyms:
      self.synonyms.append(data.replace('\n', ' ').strip())

synonym_url = 'https://www.synonymer.se/sv-syn/%s'

def parse_synonym_response(text):
  parser = SynonymWebsiteParser()
  parser.feed(text)
  return parser.synonyms

@functools.lru_cache(maxsize=None)
def get_synonyms_from(word):
  r = requests.get(synonym_url % (word,))
  return parse_synonym_response(r.text)

def get_all_synonyms_from(iterable, rec=0):
  try:
    synonyms = []
    deep_synonyms = None
    for i, word in enumerate(iterable):
        word_syns = get_synonyms_from(word)
        synonyms = synonyms + word_syns + [word]
        if rec > 0:
          deep_synonyms = get_all_synonyms_from(word_syns, rec=rec-1)
          if deep_synonyms == None:
            pickle.dump(synonyms, open('synonyms_rec%i_it%i.pkl' % (rec, i), 'wb'))
            return None
          synonyms = synonyms + deep_synonyms
        #if i % 10 == 0:
        #  print('sleeping 1 sek')
        #  time.sleep(1)
    return list(set(synonyms))
  except:
    print(traceback.format_exc())
    if deep_synonyms == None:
      pickle.dump(synonyms, open('synonyms_rec%i_it%i.pkl' % (rec, i), 'wb'))
    return None

if __name__ == '__main__':
  with open('data/raw/hat-ord.csv', 'r') as f:
    synonyms = get_all_synonyms_from(f, rec=1)
    f1 = open('data/swedish-gazetteers/hat-ord.csv', 'w')
    f1.write('\n'.join(synonyms))
  f1.close()
