-- AVEC Pablo Schreiber, Toby Stephens DE Kate Beahan APRES 2014 PAYS us

select prenom, p.nom, titre, pays, duree, annee, titre
from films f natural join generique g natural join personnes p left join pays py on f.pays = py.code
where id_film IN (select id_film from personnes natural join generique where nom = 'Schreiber' and prenom = 'Pablo' and role = 'A')
and id_film IN (select id_film from personnes natural join generique where nom = 'Stephens' and prenom = 'Toby' and role = 'A')
and id_film IN (select id_film from personnes natural join generique where nom = 'Beahan' and prenom = 'Kate' and role = 'R')
and f.pays = 'us'
and annee BETWEEN 2014 and 2016
and titre = '13 Hours'