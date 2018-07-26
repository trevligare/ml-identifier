from bs4 import BeautifulSoup
import sys


def collect_words(soup, hateWords):
    train_sentence = []
    for word in soup.find_all('w'):
        blingbring = word['blingbring'].split('|')
        if any((bb in hateWords) for bb in blingbring):
            train_sentence += [word for word in blingbring if word != '']
    return train_sentence


def xml2hate_words(xml_file, output_file):
    f1 = open(output_file, 'w')
    hateWords = open('data/raw/hat-ord.csv', 'r').read().split('\n')
    train_sentence = []

    doc = []
    with open(xml_file, 'r') as f:
        for line in f:
            soup = BeautifulSoup(line, 'html.parser')
            train_sentence = train_sentence + collect_words(soup, hateWords)

            if '</sentence>' in line:
                doc.append('\n'.join(train_sentence) + '\n')
                train_sentence = []

            if len(doc) == 500:
                f1.write(''.join(doc))
                doc = []

    f1.write(''.join(doc))
    f1.close()
    unique_words = set(open(output_file, 'r').read().split('\n'))
    open(output_file, 'w').write('\n'.join(unique_words))


if __name__ == '__main__':
    xml2hate_words(sys.argv[1], 'data/tmp-output/training_hat.txt')
