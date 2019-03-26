SELECT *
FROM ((personnes NATURAL JOIN generique) AS GA, (personnes NATURAL JOIN generique) AS GR), films
WHERE titre = '13 Hours'
AND GA.id_film = films.id_film
AND GR.id_film = films.id_film
AND (
	GA.nom IN ('Stephens', 'Schreiber')
	AND GA.prenom IN ('Toby', 'Pablo')
	AND GA.role = 'A')
AND (
	GR.nom IN ('Beahan')
	AND GR.prenom IN ('Kate')
	AND GR.role = 'R')
;