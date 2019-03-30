import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {

        // testing
        RechercheFilm r = new RechercheFilm("bdd/bdfilm.sqlite");
        //r.retrouve("TITRE blues, AVEC John Belushi");
        ArrayList<ArrayList<String>> resultSetRequest = r.bdd.requeteArray("select f.id_film, prenom, p.nom, titre, duree, annee, titre, py.nom\n" +
                "from films f natural join generique g natural join personnes p left join pays py on f.pays = py.code\n" +
                "where id_film IN (select id_film from personnes natural join generique where nom = 'Schreiber' and prenom = 'Pablo' and role = 'A')\n" +
                "and id_film IN (select id_film from personnes natural join generique where nom = 'Stephens' and prenom = 'Toby' and role = 'A')\n" +
                "and id_film IN (select id_film from personnes natural join generique where nom = 'Beahan' and prenom = 'Kate' and role = 'R')\n" +
                "and f.pays = 'us'\n" +
                "and annee BETWEEN 2014 and 2016\n" +
                "and titre = '13 Hours'");

        System.out.println("\n");
        for (int i = 0; i < resultSetRequest.size(); i++){
            System.out.println(resultSetRequest.get(i).toString());
            if (i == 0) System.out.println("-------------------------------------------------------------------");
        }
    }

}
