import jdk.nashorn.internal.runtime.regexp.joni.exception.SyntaxException;

import java.sql.Array;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Classe de recherche simplifiée sur la BDD IMDB.
 * @author Machon
 * @author Sakovitch
 * @version 0.1
 */
public class RechercheFilm {

    //TODO Isn't MoviePseudoRequest better as Enum ?
    private class MoviePseudoRequest {
        public ArrayList<String> TITRE = new ArrayList<>();
        public ArrayList<ArrayList<String>> DE = new ArrayList<>();
        public ArrayList<ArrayList<String>> AVEC = new ArrayList<>();
        public ArrayList<String> PAYS = new ArrayList<>();
        public ArrayList<String> EN = new ArrayList<>();
        public String AVANT = "";
        public String APRES = "";
    }

    //TODO change bdd to private
    public BDDManager bdd;

    /**
     * Constructeur, ouvre la BDD.
     * @param nomFichierSQLite Nom du ficher BDD.
     */
    public RechercheFilm(String nomFichierSQLite) {
        bdd = new BDDManager(nomFichierSQLite);
    }

    /**
     * Ferme la BDD.
     */
    public void fermeBase() {
        //TODO
    }

    /**
     * Effectue une recherche dans la BDD.
     * @param requete Langage de recherce simplifee:<br>
     * TITRE suivi d'un titre de film;<br>
     * DE suivi d'un nom de réalisateur;<br>
     * AVEC suivi d'un nom d'acteur ou d'actrice;<br>
     * PAYS suivi d'un code (ISO sur deux lettres) ou nom de pays;<br>
     * EN suivi d'une annee de sortie;<br>
     * AVANT suivi d'une annee de sortie (correspond a <, on ne traite pas <=);<br>
     * APRES (ou APRÈS) suivi d'une annee de sortie (correspond a >, on ne traite pas >=).<br>
     * Les conditions ainsi exprimees peuvent être combinees soit en les separant par une virgule ("et" implicite), soit avec OU.<br>
     * On peut omettre le mot-clef apres une virgule ou OU, dans ce cas c'est implicitement le meme type de critere que precedemment qui s'applique.
     * @return Reponse de la recherche au format JSON.
     */
    public String retrouve(String requete) throws SyntaxException {
        MoviePseudoRequest moviePseudoRequest = formatRequest(requete); //TODO en cours
        String sql = convertToSQL(moviePseudoRequest); //TODO
        ResultSet set = bdd.requete(sql); //DONE
        ArrayList<InfoFilm> list = getInfoFilmArray(set); //TODO
        String json = convertToJSON(list); //DONE
        return json;
    }

    private MoviePseudoRequest formatRequest(String requete) {

        MoviePseudoRequest infos = new MoviePseudoRequest();

        String field="", value="";
        ArrayList<String> tmpStorage = new ArrayList<>();
        boolean newField = true;
        String[] possibleTerms = {"TITRE", "DE", "AVEC", "PAYS", "EN", "AVANT", "APRES"};

        String[] list = requete.split(" |((?<=,)|(?=,))");
        for (int i=0; i<list.length; i++) //pour chaque mot de la recherche
        {
            String str = list[i];
            if (newField) //si on commence un nouveau champ
            {
                field = "";
                if (Arrays.asList(possibleTerms).contains(str)) //si le champ fait parti des champs valides
                {
                    field = str;
                    newField = false;
                }
                else //si le champ n'est pas reconnu
                {
                    throw new SyntaxException("Invalid field name: "+str);
                }
            }
            else //si on regarde la valeur d'un champ
            {
                if (str.equals("OU"))
                {

                }
                else if (str.equals(","))
                {
                    if (field.equals("TITRE"))
                    {
                        if (tmpStorage.isEmpty())
                        {

                        }
                        else
                        {

                        }
                        newField = Arrays.asList(possibleTerms).contains(list[i+1]);
                    }
                    else if (field.equals("DE"))
                    {

                    }
                    else if (field.equals("AVEC"))
                    {

                    }
                    else if (field.equals("PAYS"))
                    {

                    }
                    else if (field.equals("EN"))
                    {

                    }
                    else if (field.equals("AVANT"))
                    {

                    }
                    else if (field.equals("APRES"))
                    {

                    }
                }
                else
                {
                    value += str+" ";
                }
            }

        }

        return infos;

        /*** //stockage
        if (field.equals("TITRE")) {

        } else if (field.equals("DE")) {

        } else if (field.equals("AVEC")) {

        } else if (field.equals("PAYS")) {

        } else if (field.equals("EN")) {

        } else if (field.equals("AVANT")) {

        } else if (field.equals("APRES")) {

        }
        /***/
    }

    private String convertToSQL(MoviePseudoRequest moviePseudoRequestmap) {
        //TODO Sakovitch
        StringBuilder reqSQL = new StringBuilder();
        reqSQL.append("SELECT titre, nom, prenom, pays, annee"); // Chaine du SELECT  de la requête SQL générale

        StringBuilder AVEC_SQL = new StringBuilder();
        StringBuilder PAYS_SQL = new StringBuilder();
        StringBuilder TITRE_SQL = new StringBuilder();
        StringBuilder DE_SQL = new StringBuilder(); //TODO DE_SQL
        StringBuilder AVANT_SQL = new StringBuilder();
        StringBuilder APRES_SQL = new StringBuilder();
        StringBuilder EN_SQL = new StringBuilder();

        // Chaîne du FROM
        StringBuilder FROM = new StringBuilder();
        FROM.append(" FROM films f NATURAL JOIN generique g NATURAL JOIN personnes p LEFT JOIN pays py ON f.pays = py.code");

        // Permet de savoir si le mot clef WHERE a déjà été ajouté à la requête (avant le(s) 'AND')
        boolean where_created = false;

        if (!moviePseudoRequestmap.AVEC.isEmpty()){
            ArrayList<ArrayList<String>> personnes_array = moviePseudoRequestmap.AVEC;

            String nom, prenom;
            // Ajout de la condition pour les noms et prenoms
            for (int i = 0; i < personnes_array.size(); i++) {
                for (int j = 0; j < personnes_array.get(i).size(); j++) {

                    String[] parts = personnes_array.get(i).get(j).split(" "); // On sépare nom et prénom
                    nom = parts[0];
                    prenom = parts[0];

                    if (where_created = (i == 0 && j == 0)) AVEC_SQL.append(" WHERE");
                    else AVEC_SQL.append(" AND");

                    AVEC_SQL.append(" id_film IN (select id_film from personnes natural join generique where nom = '").append(nom).append("' and prenom = '").append(prenom).append("' and role = 'A')");
                }
                // TODO GESTION DU 'OU'
            }
        }

        // Ajout de la condition liée au titre du film
        if (!moviePseudoRequestmap.TITRE.isEmpty()){
            if (where_created) TITRE_SQL.append(" AND titre LIKE '%").append(moviePseudoRequestmap.TITRE.get(0)).append("%'");
            else TITRE_SQL.append(" WHERE titre LIKE '%").append(moviePseudoRequestmap.TITRE.get(0)).append("%'");
        }

        // Conditions liées aux années
        if (!moviePseudoRequestmap.EN.isEmpty()){
            if (where_created) EN_SQL.append(" AND annee = ").append(moviePseudoRequestmap.EN);
            else EN_SQL.append(" WHERE annee = ").append(moviePseudoRequestmap.EN);
        }
        else {
            // ENCADREMENT
            if (!moviePseudoRequestmap.AVANT.isEmpty() && !moviePseudoRequestmap.APRES.isEmpty()){
                if (where_created) AVANT_SQL.append(" AND annee < ").append(moviePseudoRequestmap.AVANT);
                else AVANT_SQL.append(" WHERE annee BETWEEN ").append(moviePseudoRequestmap.AVANT).append(" AND ").append(moviePseudoRequestmap.APRES);
            }
            else{
                if (!moviePseudoRequestmap.AVANT.isEmpty()){
                    if (where_created) AVANT_SQL.append(" AND annee < ").append(moviePseudoRequestmap.AVANT);
                    else AVANT_SQL.append(" WHERE annee < ").append(moviePseudoRequestmap.AVANT);
                }
                else if (!moviePseudoRequestmap.APRES.isEmpty()){
                    if (where_created) APRES_SQL.append(" AND annee > ").append(moviePseudoRequestmap.APRES);
                    else APRES_SQL.append(" WHERE annee > ").append(moviePseudoRequestmap.APRES);
                }
            }
        }

        // Pays d'origine du film
        if (!moviePseudoRequestmap.PAYS.isEmpty()){
            if (where_created) AVANT_SQL.append(" AND f.pays = '").append(moviePseudoRequestmap.PAYS).append("'");
            else AVANT_SQL.append(" WHERE f.pays = '").append(moviePseudoRequestmap.PAYS).append("'");
        }

        //reqSQL.append(); // Ajout de chaque clause FROM WHERE AND
        return reqSQL.toString();
    }

    private ArrayList<InfoFilm> getInfoFilmArray(ResultSet set) {
        //TODO
        ArrayList<InfoFilm> FilmsList = new ArrayList<>();
        return FilmsList;
    }

    private String convertToJSON(ArrayList<InfoFilm> list) {
        String result = "";
        for (InfoFilm movie : list) {
            result += movie.toString();
            result += "\n";
        }
        return result;
    }

    //TODO to remove when a proper testing class is created
    public static void main(String[] args) {

        // testing
        RechercheFilm r = new RechercheFilm("bdd/bdfilm.sqlite");
        //r.retrouve("TITRE blues, AVEC John Belushi");
        System.out.println(r.bdd.requete("SELECT * FROM films").toString());
    }

}