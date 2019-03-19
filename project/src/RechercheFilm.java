/**
 * Classe de recherche simplifiée sur la BDD IMDB.
 *  @author Machon
 *  @author Sakovitch
 *  @version 0.1
 */
public class RechercheFilm {

    /**
     * Constructeur, ouvre la BDD.
     *  @param nomFichierSQLite Nom du ficher BDD.
     */
    public RechercheFilm(String nomFichierSQLite) {
        //TODO connexion BDD
    }

    /**
     * Ferme la BDD.
     */
    public void fermeBase() {
        //TODO
    }

    /**
     * Effectue une recherche dans la BDD.
     *  @param requete Langage de recherce simplifée:
     *      TITRE suivi d'un titre de film;
     *      DE suivi d'un nom de réalisateur;
     *      AVEC suivi d'un nom d'acteur ou d'actrice;
     *      PAYS suivi d'un code (ISO sur deux lettres) ou nom de pays;
     *      EN suivi d'une annee de sortie;
     *      AVANT suivi d'une annee de sortie (correspond a <, on ne traite pas <=);
     *      APRES (ou APRÈS) suivi d'une annee de sortie (correspond a >, on ne traite pas >=).
     *  Les conditions ainsi exprimees peuvent être combinees soit en les separant par une virgule ("et" implicite), soit avec OU.
     *  On peut omettre le mot-clef apres une virgule ou OU, dans ce cas c'est implicitement le meme type de critere que precedemment qui s'applique.
     *  @return Reponse de la recherche au format JSON.
     */
    public String retrouve(String requete) {
        //TODO
        return null;
    }

    public static void main(String[] args) {
        new RechercheFilm(null);
    }

}