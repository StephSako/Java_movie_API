SELECT films.id_film, titre
FROM films
	LEFT JOIN (personnes NATURAL JOIN generique) A1 ON films.id_film=A1.id_film
 	LEFT JOIN (personnes NATURAL JOIN generique) A2 ON films.id_film=A2.id_film
	LEFT JOIN (personnes NATURAL JOIN generique) A3 ON films.id_film=A3.id_film
 	LEFT JOIN (personnes NATURAL JOIN generique) R1 ON films.id_film=R1.id_film
WHERE A1.nom = 'Stephens' AND A1.role = 'A' AND A1.prenom = 'Toby'
AND A2.nom = 'Schreiber' AND A2.role = 'A' AND A2.prenom = 'Pablo'
AND A3.nom = 'Martini' AND A3.role = 'A' AND A3.prenom = 'Max'
AND R1.nom = 'Beahan' AND R1.role = 'R' AND R1.prenom = 'Kate';
