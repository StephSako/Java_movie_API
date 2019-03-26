import com.sun.xml.internal.ws.api.message.ExceptionHasMessage;
import jdk.nashorn.internal.runtime.regexp.joni.exception.SyntaxException;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe de recherche simplifiée sur la BDD IMDB.
 * @author Machon
 * @author Sakovitch
 * @version 0.1
 */
public class RechercheFilm {

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
        Map map = formatRequest(requete); //TODO en cours
        String sql = convertToSQL(map); //TODO
        ResultSet set = bdd.requete(sql); //DONE
        ArrayList<InfoFilm> list = getInfoFilmArray(set); //TODO
        String json = convertToJSON(list); //DONE
        return json;
    }

    private Map<String, ArrayList<String>> formatRequest(String requete) {
        String term = "", value = "";
        boolean isTerm = true;
        String[] possibleTerms = {"TITRE", "DE", "AVEC", "PAYS", "EN", "AVANT", "APRES"};

        for (int i=0; i<requete.length(); i++) {
            char c = requete.charAt(i);

            if (isTerm) //si on s'occupe du terme
            {
                if (c != ' ') //si le char actuel n'est pas un espace
                {
                    term += Character.toUpperCase(c);
                }
                else //si le char actuel est un espace = la fin du terme
                {
                    if (Arrays.asList(possibleTerms).contains(term)) //si le terme est valide
                    {
                        isTerm = false;
                    }
                    else //si le terme est invalide
                    {
                        throw new SyntaxException("Invalid term: "+term);
                    }
                }
            }

            else //s'il s'agit de la valeur
            {
                if (c==',') //si on tombe sur une virgule
                {
                    term
                }
                else if (c=='O' && requete.charAt(i+1)=='U') //si on tombe sur un OU
                {

                }
                else //si on tombe sur un char quelconque
                {
                    value += c;
                }
            }
        }
    }

    private String convertToSQL(Map<String, ArrayList<String>> map) {
        //TODO Sakovitch
        StringBuilder reqSQL = new StringBuilder();
        StringBuilder AVEC_SQL = new StringBuilder();
        StringBuilder TITRE_SQL = new StringBuilder();
        StringBuilder DE_SQL = new StringBuilder(); //TODO DE_SQL
        StringBuilder AVANT_SQL = new StringBuilder();
        StringBuilder APRES_SQL = new StringBuilder();
        StringBuilder EN_SQL = new StringBuilder();

        // Chaîne du FROM  qui pourraît être modifié par la recherche d'acteurs ou réalisateurs
        StringBuilder FROM_WITH_JOIN = new StringBuilder();

        boolean where_created = false;

        reqSQL.append("SELECT titre, nom, prenom, pays, annee FROM films"); // Requête SQl générale

        if (map.containsKey("AVEC")){ // TODO Refaire nom / prenom SQL
            reqSQL.append(" NATURAL JOIN (personnes NATURAL JOIN generique)");
            ArrayList<String> personnes_array = map.get("AVEC");

            String nom, prenom;
            // Ajout de la condition pour les noms et prenoms
            for (int i = 0; i < personnes_array.size(); i++) {

                // On ajoute un table avec jointure car il s'agit d'un nouvel acteur
                FROM_WITH_JOIN.append(" LEFT JOIN (personnes NATURAL JOIN generique) A").append(i).append(" ON films.id_film = A").append(i).append(".id_film");

                String[] parts = personnes_array.get(i).split(" "); // On sépare nom et prénom de la personne
                nom = parts[0];
                prenom = parts[0];

                if (i == 0) {
                    AVEC_SQL.append(" WHERE");
                    where_created = true;
                }
                else AVEC_SQL.append(" AND");

                AVEC_SQL.append(" A").append(i).append(".nom = '").append(nom).append("' AND A").append(i).append(".prenom = '").append(prenom).append("' AND A").append(i).append(".role = 'A'");
            }
        }

        // Ajout de la condition liée au titre du film
        if (map.containsKey("TITRE")){
            if (where_created) TITRE_SQL.append(" AND titre LIKE '%").append(map.get("TITRE")).append("%'");
            else TITRE_SQL.append(" WHERE titre LIKE '%").append(map.get("TITRE")).append("%'");
        }

        // Ajout de la condition liée aux années passées
        if (map.containsKey("AVANT")){
            if (where_created) AVANT_SQL.append(" AND annee < ").append(map.get("AVANT"));
            else AVANT_SQL.append(" WHERE annee < ").append(map.get("AVANT"));
        }

        // Ajout de la condition liée aux années futures
        if (map.containsKey("APRES")){
            if (where_created) APRES_SQL.append(" AND annee > ").append(map.get("APRES"));
            else APRES_SQL.append(" WHERE annee > ").append(map.get("APRES"));
        }
        else if (map.containsKey("APRÈS")){
            if (where_created) APRES_SQL.append(" AND annee > ").append(map.get("APRÈS"));
            else  APRES_SQL.append(" WHERE annee > ").append(map.get("APRÈS"));
        }

        if (map.containsKey("EN")){
            if (where_created) EN_SQL.append(" AND annee = ").append(map.get("EN"));
            else EN_SQL.append(" WHERE annee = ").append(map.get("EN"));
        }

        //reqSQL.append(); // Ajout de la clause FROM  et de chaque clause du WHERE
        return reqSQL.toString();
    }

    private ArrayList<InfoFilm> getInfoFilmArray(ResultSet set) {
        //TODO
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
        r.retrouve("TITRE blues, AVEC John Belushi");
        System.out.println(r.bdd.requete("SELECT * FROM films").toString());
    }

}