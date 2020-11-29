
This directory will contain the libraries that could not be added as a Gradle dependency.
Some of these libraries are not available as Maven artifact and some are too large to
checkin to github due to free account limitations. 

Download following libraries:
-   stanford-english-corenlp-models-current.jar (or stanford-english-corenlp-2018-02-27-models.jar and rename it)
-   ws4j-1.0.1.jar

curl 'https://nlp.stanford.edu/software/stanford-english-corenlp-2018-02-27-models.jar' --output stanford-english-c
orenlp-models-current.jar
curl https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/ws4j/ws4j-1.0.1.jar --output ws4j-1.0.1.jar

* If we add KBP annotator at a later point, download the model from:
https://nlp.stanford.edu/software/corenlp-backup-download.html
stanford-english-kbp-corenlp-2018-02-27-models.jar

* If we decide to use JWNL instead of WS4J for WordNet, the following libraries will need to be added as Gradle dependencies: 
'net.sf.extjwnl:extjwnl:2.0.2', 'net.sf.extjwnl:extjwnl-data-wn31:1.2'
