import jdk.nashorn.internal.runtime.regexp.joni.exception.SyntaxException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Classe de recherche simplifiée sur la BDD IMDB.
 * @author Théo Machon 32
 * @author Stephen Sakovitch 32
 * @version 0.1
 */
public class RechercheFilm {

    private class MoviePseudoRequest {
        public ArrayList<String> TITRE = new ArrayList<>();
        public ArrayList<ArrayList<String>> DE = new ArrayList<>(); // LIGNES = ET, COLONNES = OU
        public ArrayList<ArrayList<String>> AVEC = new ArrayList<>(); // LIGNES = ET, COLONNES = OU
        public ArrayList<String> PAYS = new ArrayList<>();
        public ArrayList<Integer> EN = new ArrayList<>();
        public ArrayList<Integer> AVANT = new ArrayList<>();
        public ArrayList<Integer> APRES = new ArrayList<>();
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

        /* TEST */
        MoviePseudoRequest moviePseudoRequestTest = new MoviePseudoRequest();

        moviePseudoRequestTest.TITRE.add("Avatar");

        moviePseudoRequestTest.EN.add(2009);

        moviePseudoRequestTest.PAYS.add("us");

        ArrayList<String> tabDE = new ArrayList<>();
        tabDE.add("James Cameron");
        tabDE.add("Peter Berg");
        moviePseudoRequestTest.DE.add(tabDE);

        ArrayList<String> tabAVEC = new ArrayList<>();
        tabAVEC.add("Sam Worthington");
        tabAVEC.add("Jason Statham");
        moviePseudoRequestTest.AVEC.add(tabAVEC);


        String sqlTest = convertToSQL(moviePseudoRequestTest);
        System.out.println(sqlTest);

        MoviePseudoRequest moviePseudoRequest = formatRequest(requete); //TODO en cours
        String sql = convertToSQL(moviePseudoRequest); //TODO en cours
        ResultSet set = bdd.requete(sql);
        ArrayList<InfoFilm> list = getInfoFilmArray(set); //TODO en cours
        String json = convertToJSON(list);
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
                    // LIGNES = ET, COLONNES = OU (en SQL, ce sont les OU qui sont imbriqués entre les () )
                    // NOMS ET PRENOMS EN MAJUSCULES
                    // VERIRIER LES ESPACES DANS LES NOMS/PRENOMS
                    else if (field.equals("DE"))
                    {

                    }
                    // LIGNES = ET, COLONNES = OU (en SQL, ce sont les OU qui sont imbriqués entre les () )
                    // NOMS ET PRENOMS EN MAJUSCULES
                    // VERIRIER LES ESPACES DANS LES NOMS/PRENOMS
                    else if (field.equals("AVEC"))
                    {

                    }
                    else if (field.equals("PAYS"))
                    {

                    }
                    else if (field.equals("EN")) // TODO Convertir valeurs de EN en int
                    {

                    }
                    else if (field.equals("AVANT")) // TODO Convertir valeurs AVANT en int
                    {

                    }
                    else if (field.equals("APRES")) // TODO Convertir valeurs APRES en int
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

        /* //stockage
        if (field.equals("TITRE")) {

        } else if (field.equals("DE")) {

        } else if (field.equals("AVEC")) {

        } else if (field.equals("PAYS")) {

        } else if (field.equals("EN")) {

        } else if (field.equals("AVANT")) {

        } else if (field.equals("APRES")) {

        }
        */
    }

    private String convertToSQL(MoviePseudoRequest moviePseudoRequestmap) {
        StringBuilder reqSQL = new StringBuilder();
        StringBuilder SELECT = new StringBuilder();
        SELECT.append("SELECT f.id_film, prenom, p.nom, titre, duree, annee, py.nom, role"); // Chaine du SELECT  de la requête SQL générale

        StringBuilder AVEC_SQL = new StringBuilder(); //TODO Vérifier pour les accents
        StringBuilder PAYS_SQL = new StringBuilder();
        StringBuilder TITRE_SQL = new StringBuilder();
        StringBuilder DE_SQL = new StringBuilder(); //TODO Vérifier pour les accents
        StringBuilder AVANT_SQL = new StringBuilder(); //TODO
        StringBuilder APRES_SQL = new StringBuilder(); //TODO
        StringBuilder EN_SQL = new StringBuilder();
        String ORDER_BY_SQL = "\nORDER BY annee DESC, titre";

        // Chaîne du FROM
        StringBuilder FROM = new StringBuilder();
        FROM.append("\nFROM films f NATURAL JOIN generique g NATURAL JOIN personnes p LEFT JOIN pays py ON f.pays = py.code");

        // Permet de savoir si le mot clef WHERE a déjà été ajouté à la requête (avant le(s) 'AND')
        boolean where_created = false;

        // AVEC (acteur(s))
        if (!moviePseudoRequestmap.AVEC.isEmpty()){

            String nom, prenom;
            // NOM PRENOM
            for (int i = 0; i < moviePseudoRequestmap.AVEC.size(); i++) {

                if (i == 0){
                    AVEC_SQL.append("\nWHERE (");
                    where_created = true;
                }
                else AVEC_SQL.append("\nAND (");

                for (int j = 0; j < moviePseudoRequestmap.AVEC.get(i).size(); j++) {
                    String[] prenom_nom = moviePseudoRequestmap.AVEC.get(i).get(j).split(" "); // On sépare nom et prénom

                    if (j > 0) AVEC_SQL.append("\nOR");

                    AVEC_SQL.append(" id_film IN (SELECT id_film FROM personnes NATURAL JOIN generique WHERE UPPER(nom) LIKE UPPER('%").append(prenom_nom[1]).append("%') AND UPPER(prenom) LIKE UPPER('%").append(prenom_nom[0]).append("%') AND role = 'A')");
                }
                AVEC_SQL.append(")");
            }
        }

        // DE (réalisateur(s))
        if (!moviePseudoRequestmap.DE.isEmpty()){

            String nom, prenom;
            // NOM PRENOM
            for (int i = 0; i < moviePseudoRequestmap.DE.size(); i++) {
                if (!where_created){
                    DE_SQL.append("\nWHERE (");
                    where_created = true;
                }
                else DE_SQL.append("\nAND (");

                for (int j = 0; j < moviePseudoRequestmap.DE.get(i).size(); j++) {

                    String[] prenom_nom = moviePseudoRequestmap.DE.get(i).get(j).split(" "); // On sépare nom et prénom
                    if (j > 0) DE_SQL.append("\nOR");

                    DE_SQL.append(" id_film IN (SELECT id_film FROM personnes NATURAL JOIN generique WHERE UPPER(nom) LIKE UPPER('%").append(prenom_nom[1]).append("%') AND UPPER(prenom) LIKE UPPER('%").append(prenom_nom[0]).append("%') AND role = 'R')");
                }
                DE_SQL.append(")");
            }
        }

        // TITRE
        if (!moviePseudoRequestmap.TITRE.isEmpty()){
            if (!where_created){
                where_created = true;
                TITRE_SQL.append("\nWHERE (");
            }
            else TITRE_SQL.append("\nAND (");

            for (int i = 0; i < moviePseudoRequestmap.TITRE.size(); i++) {
                if (i > 0) TITRE_SQL.append(" OR");
                TITRE_SQL.append(" UPPER(titre) LIKE UPPER('%").append(moviePseudoRequestmap.TITRE.get(i)).append("%')");
            }
            TITRE_SQL.append(")");
        }

        // EN AVANT APRES
        // EN
        if (!moviePseudoRequestmap.EN.isEmpty()){
            if (!where_created){
                EN_SQL.append("\nWHERE (");
                where_created = true;
            }
            else EN_SQL.append("\nAND (");

            for (int i = 0; i < moviePseudoRequestmap.EN.size(); i++) {
                if (i > 0) EN_SQL.append("\nOR");
                EN_SQL.append(" annee = ").append(moviePseudoRequestmap.EN.get(i));
            }
            EN_SQL.append(")");
        }
        /*else {

            // AVANT //TODO Steph verifier pour une liste pour plusieurs 'OU'
            if (!moviePseudoRequestmap.AVANT.isEmpty()){
                if (where_created) AVANT_SQL.append("\nAND annee < ").append(Collections.max(moviePseudoRequestmap.AVANT));
                else{
                    AVANT_SQL.append("\nWHERE annee < ").append(Collections.max(moviePseudoRequestmap.AVANT));
                    where_created = true;
                }

                for (int i = 0; i < moviePseudoRequestmap.AVANT.size(); i++) {
                    if (where_created = (i == 0)) AVEC_SQL.append(" WHERE (");
                    else AVEC_SQL.append(" AND (");

                    for (int j = 0; j < moviePseudoRequestmap.AVANT.get(i).size(); j++) {

                        if (j > 0) AVEC_SQL.append(" OR");

                        AVEC_SQL.append(" id_film IN (SELECT id_film FROM personnes NATURAL JOIN generique WHERE nom = '").append(nom).append("' AND prenom = '").append(prenom).append("' AND role = 'A')");
                    }
                    AVEC_SQL.append(")");
            }
            // APRES TODO Steph verifier pour une liste pour plusieurs 'OU'
            else if (!moviePseudoRequestmap.APRES.isEmpty()){
                if (where_created) APRES_SQL.append(" annee > ").append(Collections.min(moviePseudoRequestmap.APRES));
                else{
                    APRES_SQL.append("\nWHERE annee > ").append(Collections.min(moviePseudoRequestmap.APRES));
                    where_created = true;
                }
            }
        }*/

        // PAYS
        if (!moviePseudoRequestmap.PAYS.isEmpty()){
            if (!where_created) PAYS_SQL.append("\nWHERE (");
            else PAYS_SQL.append("\nAND (");

            for (int i = 0; i < moviePseudoRequestmap.PAYS.size(); i++) {
                if (i > 0) PAYS_SQL.append("\nOR");

                PAYS_SQL.append(" py.code LIKE '%").append(moviePseudoRequestmap.PAYS.get(i)).append("%' OR py.nom LIKE '%").append(moviePseudoRequestmap.PAYS.get(i)).append("%'");
            }
            PAYS_SQL.append(")");
        }

        // Ajout de chaque clause
        reqSQL.append(SELECT).append(FROM).append(AVEC_SQL).append(DE_SQL).append(TITRE_SQL).append(EN_SQL).append(APRES_SQL).append(AVANT_SQL).append(PAYS_SQL).append(ORDER_BY_SQL);
        return reqSQL.toString();
    }

    /**
     * Ordre des colonnes dans le resultSet passé en paramètre :
     * [1] f.id_film (ID du film)
     * [2] prenom [3] p.nom (prénom/nom d'une personne)
     * [4] titre (titre du film)
     * [5] duree (duree du film en minutes)
     * [6] annee (annee de sortie du film)
     * [7] py.nom (nom du pays en entier)
     * [8] role (role de la personne => 'A' : acteur, 'R' : réalisateur)
     * @param set resultSet de la requête SQL construite à partir du pseudo-langage
     * @return ArrayList<InfoFilm> liste des films
     */
    private ArrayList<InfoFilm> getInfoFilmArray(ResultSet set) {
        ArrayList<InfoFilm> filmsList = new ArrayList<>();

        try {
            int size = set.getMetaData().getColumnCount();

            // Champs de la classe InfoFilm
            int id_film = -1;
            ArrayList<NomPersonne> realisateurs = new ArrayList<>();
            ArrayList<NomPersonne> acteurs = new ArrayList<>();
            ArrayList<String> autres_titres = new ArrayList<>(); // A faire dans une deuxième requête

            while (set.next()) { // Lecture des lignes
                for (int i = 1; i <= size; i++) { // Lecture des colonnes

                    // On remplit chaque champ
                    if (set.getString(8).equals("A")) acteurs.add(new NomPersonne(set.getString(2), set.getString(3)));
                    else if (set.getString(8).equals("R")) realisateurs.add(new NomPersonne(set.getString(2), set.getString(3)));

                    String titre = set.getString(4);
                    int duree = set.getInt(5);
                    int annee = set.getInt(6);
                    String pays = set.getString(7);

                    // Nouveau film lu : on créé et ajoute une instance d'InfoFilm dans l'ArrayList
                    if (set.getInt(1) != id_film){
                        filmsList.add(new InfoFilm(titre, realisateurs, acteurs, pays, annee, duree, autres_titres));

                        // On vide les tableaux pour passer au film suivant
                        realisateurs.clear();
                        acteurs.clear();
                        autres_titres.clear();
                    }

                    id_film = set.getInt(1);
                }
            }
            set.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return filmsList;
    }

    private String convertToJSON(ArrayList<InfoFilm> list) {
        StringBuilder result = new StringBuilder();
        for (InfoFilm movie : list) {
            result.append(movie.toString());
            result.append("\n");
        }
        return result.toString();
    }

}
