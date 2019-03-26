SELECT *
FROM (personnes NATURAL JOIN generique) AS GA NATURAL JOIN films,
	(personnes NATURAL JOIN generique) AS GR NATURAL JOIN films
	WHERE titre = '13 Hours'
	AND GA.nom IN ('Stephens')
	AND GA.prenom IN ('Toby')
	AND GR.nom IN ('Beahan')