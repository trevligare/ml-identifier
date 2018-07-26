import sys

class Entity(object):
  """Representation of a named entity in a sentence"""
  def __init__(self, entity_name, entity_word, entity_probability):
    super(Entity, self).__init__()
    self.entity_name = entity_name
    self.entity_word = entity_word
    self.entity_probability = entity_probability
  def is_person():
    return self.entity_name == '<PERSON>'
  def get_entity_probability():
    return self.entity_probability

class NotImplemented(Exception):
  pass

def identify_objects(sentence):
  raise NotImplemented('TODO: Implement a NER Tagger to identify entities in sentence')


def construct_dependency_tree(sentence):
  raise NotImplemented('TODO: Implement a POS Tagger and dependency parser to identify what entitities the words referes to')


def identify_hate_threat_objects(sentence):
  raise NotImplemented('TODO: Implement word mathcer to look for hate words in sentence to determine if this sentence could be a hate/threat sentence')


def remove_negations(sentence, dep_tree, hate_threats):
  raise NotImplemented('TODO: Implement removal of negations concerning hate_threat words in sentence')


def is_sibling_of(ent1, ent2, dep_tree):
  raise NotImplemented('TODO: Implement sibling of check for given two entities')


def compute_hate_threat_probability(entity, dep_tree):
  raise NotImplemented('TODO: Implement hate/threat probability')

def is_hate_sentence(sentence):
  entitities = identify_objects(sentence)
  if all(not ent.is_person() for ent in entitities):
    return False

  hate_threats = identify_hate_threat_objects(sentence)
  if not hate_threats:
    return False

  dep_tree = construct_dependency_tree(sentence)
  non_neg_sentence = remove_negations(sentence, dep_tree, hate_threats)

  hate_prob = 0
  hate_instance_count = 0
  person_entities = [ent for ent in entitities if ent.is_person()]
  for hate_threat in hate_threats:
    for entity in person_entities:
      if is_sibling_of(hate_threat, entity, dep_tree):
        hate_prob += compute_hate_threat_probability(entity, dep_tree)
        hate_instance_count += 1
  return hate_prob / hate_instance_count

if __name__ == '__main__':
  if is_hate_sentence(sys.argv[1]):
    print(sys.argv[1], "is a hate/threat sentence")
  else:
    print(sys.argv[1], "is not a hate/threat sentence")