-- AVEC Ulrich Tukur DE Florian Henckel von Donnersmarck APRES 2005 PAYS de

select prenom, nom, f.titre, pays, duree, annee, a_t.titre
from (films f natural join generique natural join personnes) agp left join autres_titres a_t on agp.id_film = a_t.id_film
where f.id_film IN (select id_film from personnes natural join generique where nom = 'Henckel von Donnersmarck' and prenom = 'Florian' and role = 'R')
and f.id_film IN (select id_film from personnes natural join generique where nom = 'Tukur' and prenom = 'Ulrich' and role = 'A')
and pays = 'de'
and annee > 2005