SELECT films.id_film, titre
FROM (personnes NATURAL JOIN generique) AS A1, (personnes NATURAL JOIN generique) AS A2, films
WHERE A1.nom = 'Stephens'
AND A2.nom = 'Schreiber'
AND A1.id_film = A2.id_film
AND A1.id_film = films.id_film