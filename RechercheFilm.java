/**
 * Classe de recherche simplifiée sur la BDD IMDB
 * @author Machon
 * @author Sakovitch
 * @version 0.1
 */
public class RechercheFilm {

    /**
     * Contructeur, ouvre la BDD
     * @param nomFichierSQLite
     *  Nom du ficher BDD
     */
    public RechercheFilm(String nomFichierSQLite) {

    }

    /**
     * Ferme la BDD
     */
    public void fermeBase() {

    }

    /**
     * Effectue une recherche
     * @param requete
     *  Langage de recherce simplifée :
     *      TITRE suivi d'un titre de film
     *      DE suivi d'un nom de réalisateur
     *      AVEC suivi d'un nom d'acteur ou d'actrice
     *      PAYS suivi d'un code (ISO sur deux lettres) ou nom de pays
     *      EN suivi d'une année de sortieAVANTsuivi d'une année de sortie (correspond à <, on ne traite pas <=)
     *      APRES (ou APRÈS) suivi d'une année de sortie (correspond à >, on ne traite pas >=)
     *  Les conditions ainsi exprimées peuvent être combinées soit en les séparant par une virgule ("et" implicite), soit avec OU. On peut omettre le mot-clef après une virgule ou OU, dans ce cas c'est implicitement le même type de critère que précédemment qui s'applique.
     * @return
     *  Réponse de la recherche au format JSON
     */
    public String retrouve(String requete) {

    }

}