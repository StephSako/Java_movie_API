import java.sql.*;

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
    public String retrouve(String requete) {
        //TODO
        return null;
    }

    //TODO to remove when a proper testing class is created
    public static void main(String[] args) {

        // testing
        RechercheFilm r = new RechercheFilm("bdd/bdfilm.sqlite");
        System.out.println(r.bdd.requete("SELECT * FROM films").toString());
    }

}