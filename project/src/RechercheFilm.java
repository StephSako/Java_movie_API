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
        HashMap<String, String>
    }

}