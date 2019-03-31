-- AVEC Kate Beahan, Pablo Schreiber OU Toby Stephens

SELECT f.id_film, prenom, p.nom, titre, duree, annee, py.nom, role
FROM films f natural join generique g natural join personnes p left join pays py on f.pays = py.code
WHERE 
	(	id_film IN (select id_film from personnes natural join generique where nom = 'Schreiber' and prenom = 'Pablo' and role = 'A')
	OR id_film IN (select id_film from personnes natural join generique where nom = 'Stephens' and prenom = 'Toby' and role = 'A')		)
AND	(	id_film IN (select id_film from personnes natural join generique where nom = 'Beahan' and prenom = 'Kate' and role = 'A')		)