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

        reqSQL.append("SELECT titre, nom, prenom, pays, annee FROM films"); // Requête SQl générale

        if (map.containsKey("AVEC")){ // TODO Refaire nom / prenom SQL
            reqSQL.append(" NATURAL JOIN (personnes NATURAL JOIN generique)");
            ArrayList<String> personnes_array = map.get("AVEC");

            String nom;
            // Ajout de la condition pour les noms
            for (int i = 0; i < personnes_array.size(); i++) {
                String[] parts = personnes_array.get(i).split(" ");

                nom = parts[0];

                if (i == 0) AVEC_SQL.append(" WHERE nom IN ('").append(nom).append("'");
                else AVEC_SQL.append(" ,'").append(nom).append("'");

                AVEC_SQL.append(")");
            }

            String prenom;
            // Ajout de la condition pour les prenoms
            for (int i = 0; i < personnes_array.size(); i++) {
                String[] parts = personnes_array.get(i).split(" ");

                prenom = parts[1];

                if (i == 0) AVEC_SQL.append(" WHERE prenom IN ('").append(prenom).append("'");
                else AVEC_SQL.append(" ,'").append(prenom).append("'");

                AVEC_SQL.append(")");
            }
        }

        // Ajout de la condition liée au titre du film
        if (map.containsKey("TITRE")) TITRE_SQL.append(" AND titre LIKE '%").append(map.get("TITRE")).append("%'");

        // Ajout de la condition liée aux années passées
        if (map.containsKey("AVANT")) AVANT_SQL.append(" AND annee < " + map.get("AVANT"));

        // Ajout de la condition liée aux années futures
        if (map.containsKey("APRES")) APRES_SQL.append(" AND annee > ").append(map.get("APRES"));
        else if (map.containsKey("APRÈS")) APRES_SQL.append(" AND annee > ").append(map.get("APRÈS"));

        if (map.containsKey("EN")) EN_SQL.append(" AND annee = ").append(map.get("EN"));

        //reqSQL.append(); // Ajout de chaque clause du WHERE
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