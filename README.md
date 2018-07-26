# Hate speech identification
Problem: Hate and discrimination on the internet is very hard to catch and prevent
Solution: Trevligare
Subtitle: Your personal protector

Our solution: A service that offers an intermediate function to identify and alert on hate-speech automatically
Link to GIT repo: https://github.com/trevligare/rails_api

Model languages used: Python, Java
Service languages used: Ruby on Rails, Postgresql
Team members: Jenny Vesterlund jenny.vesterlund@mittmedia.se; Johannes Lindén johannes.linden@mittmedia.se; Pontus Ekholm pontus.ekholm@mittmedia.se; Stefan Wallin stefan.wallin@mittmedia.se; Michelle Ludovici michelle.ludovici@mittmedia.se


## Installation

### Python
```
# install requierments in python
$ pip install -r requirements.txt

# download dataset
$ cd data/raw
$ wget http://spraakbanken.gu.se/lb/resurser/meningsmangder/attasidor.xml.bz2
$ bzip2 -dk attasidor.xml.bz2
$ cd -

# Extract data from dataset
$ python xml2corenlp.py data/raw/attasidor.xml
$ python gazetteers2trainingdata.py data/tmp-output/train_corenlp.txt
```

### Java
Make sure to modify config.properties to match the models you would like to be using before running the java hate identifier application.

```
# Compile java hate identifier
$ ant jar
# Run java hate identifier test
$ java -jar ml-identifier/dist/trevligare-1.0.0/trevligare-1.0.0.jar

# Train NER tagger
$ java -Xmx8g -cp stanford-ner-2018-02-27/stanford-ner.jar edu.stanford.nlp.ie.crf.CRFClassifier -prop stanford-ner-2018-02-27/classifiers/se-ner.prop
```

## Start application
To start a server application that manages the hate identifier run
```
$ java -jar dist/trevligare-server-1.0.0/trevligare-server-1.0.0.jar
```
