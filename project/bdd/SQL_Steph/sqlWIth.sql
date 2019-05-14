WITH neuf_acteurs AS (
tSELECT titre
tFROM films natural join generique
tWHERE role = 'A'
tgroup by titre
thaving count(role) >= 9
 ) 
SELECT titre
FROM films
WHERE titre IN (SELECT titre FROM neuf_acteurs)
and titre like '%vie%'
