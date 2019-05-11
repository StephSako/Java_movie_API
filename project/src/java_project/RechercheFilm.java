package java_project;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Classe de recherche dans la BDD SQLite d'IMDB.
 * @author Theo Machon 3A-32
 * @author Stephen Sakovitch 3A-32
 * @version 0.2
 */
public class RechercheFilm {

    /**
     * Accesseur a la BDD SQLite des films
     */
    public class BDDManager {
        private Connection co;

        private ArrayList<ArrayList<String>> ArrayResultSQL(String sql){
            PreparedStatement ps;
            ResultSet set;
            ArrayList<ArrayList<String>> liste = new ArrayList<>();
            try {
                ps = this.co.prepareStatement(sql);
                set = ps.executeQuery();
                if (set.next()) liste = convertRStoAL(set); // Si le ResultSet contient au moins une ligne
            }
            catch (SQLException e) { e.printStackTrace(); }
            return liste;
        }

        /**
         * Ferme la BDD
         */
        public void fermeBase() {
            try { this.co.close(); }
            catch (SQLException e) { System.out.println("{\"erreur\":\"Fermeture impossible\"}"); }
        }

        /**
         * Constructeur de l'accesseur de BDD SQLite
         *  @param file Chemin d'acces au fichier BDD
         */
        public BDDManager(String file) {
            String url = "jdbc:sqlite:";
            url += file;
            try { co = DriverManager.getConnection(url); }
            catch(Exception err) { System.out.println("{\"erreur\":\"Impossible d'accéder à la BDD Sqlite\"}"); }
        }
    }

    private boolean erreur = false;
    private String message_erreur = "";
    private BDDManager bdd;

    /**
     * Constructeur, ouvre la BDD.
     * @param nomFichierSQLite Chemin et nom du ficher BDD.
     */
    public RechercheFilm(String nomFichierSQLite) {
        bdd = new BDDManager(nomFichierSQLite);
    }

    /**
     * Traduit la pseudo-requete en requete SQL, effectue la recherche dans la BDD SQLite et renvoie les films en format JSON.
     * @param requete Langage de recherce simplifee:<br>
     * TITRE suivi d'un titre de film;<br>
     * DE suivi d'un nom de realisateur;<br>
     * AVEC suivi d'un nom d'acteur ou d'actrice;<br>
     * PAYS suivi d'un code (ISO sur deux lettres) ou nom de pays;<br>
     * EN suivi d'une annee de sortie;<br>
     * AVANT suivi d'une annee de sortie (correspond a <, on ne traite pas <=);<br>
     * APRES (ou APReS) suivi d'une annee de sortie (correspond a >, on ne traite pas >=).<br>
     * Les conditions ainsi exprimees peuvent etre combinees soit en les separant par une virgule ("et" implicite), soit avec OU.<br>
     * On peut omettre le mot-clef apres une virgule ou OU, dans ce cas c'est implicitement le meme type de critere que precedemment qui s'applique.
     * @return Reponse de la recherche au format JSON.
     */
    public String retrouve(String requete) {
        String sql = formatRequest(requete);
        if (!this.erreur) return convertToJSON(getInfoFilmArray(sql));
        else return "{\"erreur\":\"" + this.message_erreur + "\"}";
    }

    /**
     * Convertit la pseudo-requete en requete SQL exploitable
     * @param requete Pseudo-requete
     * @return String Requete SQL cree
     */
    public String formatRequest(String requete) {
        StringBuilder sql = new StringBuilder(), value= new StringBuilder();
        String SELECT = "SELECT f.id_film as id_film_f, prenom, p.nom as nom_p, f.titre as titre_f, duree, annee, py.nom as nom_py, role, (select group_concat(a_t.titre, '#') from autres_titres a_t where a_t.id_film=f.id_film) as liste_autres_titres";
        String FROM = "\nFROM films f NATURAL JOIN generique g NATURAL JOIN personnes p LEFT JOIN pays py ON f.pays = py.code";
        String ORDER_BY_SQL = "\nORDER BY annee DESC, f.titre";
        sql.append(SELECT).append(FROM);
        boolean where_created = false, TITRE_filled = false, PAYS_filled = false, EN_filled = false, newField = true, or_btwn_kw = false;
        requete += ",END";
        String field = "";
        ArrayList<String> tmpStorage = new ArrayList<>();
        String[] possibleTerms = {"TITRE", "DE", "AVEC", "PAYS", "EN", "AVANT", "APRES"};
        ArrayList<ArrayList<String>> array2D = new ArrayList<>();

        String[] list = requete.split(" |((?<=,)|(?=,))");
        label:
        for (int i = 0; i<list.length; i++) { // Parcourt de la pseudo-requete
            String str = list[i];
            if (newField) { // Si on lit un mot-clef
                if (Arrays.asList(possibleTerms).contains(str)) { // Si le mot-clef fait partie des mots-clefs valides
                    // S'il s'agit d'un champ qui ne peut pas prendre de ET et que le champ a deja ete saisie
                    if (str.matches("TITRE|PAYS|EN") && (TITRE_filled && PAYS_filled && EN_filled)) {
                        this.erreur = true;
                        this.message_erreur = "Le mot-clef " + str + " n'accepte qu'une seule valeur. Utilisez des'OU'.";
                        break;
                    }
                    else if (!field.equals(str)){// Si tout va bien pour le mot clef
                        field = str;
                        newField = false;
                    }
                }
                else { // Si le mot n'est pas un mot-clef valide
                    if (str.equals("END")) break; // Si on lit le mot-clef de la fin
                    else if (!field.matches("DE|AVEC") && field.matches("TITRE|PAYS|EN")) { // S'il ne s'agit ni de "DE", ni de "AVEC" et que plusieurs valeurs sont saisies
                        this.erreur = true;
                        this.message_erreur = "Le mot-clef '" + str + "' est invalide ou plusieurs valeurs ont ete saisies pour le mot-clef " + field + ". Utilisez des 'OU'.";
                        break;
                    }
                    else { // Si on lit une autre valeur apres un "DE" ou un "AVEC"
                        newField=false;
                        i--;
                    }
                }
            }
            else { // Si on lit la valeur d'un champ
                if (str.equals("OU")) { // Si le mot actuel lu est un "OU"
                    if (list[i+1] == null || list[i+1].equals(",")) { // S'il n'y a pas de valeur qui suit
                        this.erreur = true;
                        this.message_erreur = "Une valeur est attendue apres le mot-clef 'OU'.";
                        break;
                    }
                    else if (value.length() == 0) { // S'il n'y a pas de valeur avant un 'OU'
                        this.erreur = true;
                        this.message_erreur = "Une valeur prealable est requise pour le mot-clef 'OU'.";
                        break;
                    }
                    else if(list[i+1] != null && Arrays.asList(possibleTerms).contains(list[i+1]) && !field.equals(list[i+1])) { // Si un mot-clef est lu apres un 'OU', on concatene le SQL avec les valeurs du mot-clef precedent
                        tmpStorage.add(value.toString().trim());
                        value = new StringBuilder();
                        or_btwn_kw = true;

                        switch (field) {
                            case "TITRE":
                                if (!where_created) {
                                    where_created = true;
                                    sql.append("\nWHERE (");
                                }
                                else sql.append("\nOR (");

                                for (int j = 0; j < tmpStorage.size(); j++) {
                                    if (j > 0) sql.append(" OR");
                                    sql.append(" f.id_film IN (SELECT id_film FROM recherche_titre rt WHERE rt.titre LIKE '%' || replace('").append(tmpStorage.get(j)).append("', ' ', '%') || '%')");
                                }
                                sql.append(")");
                                TITRE_filled = true;
                                break;
                            case "DE": {
                                ArrayList<String> tmpStorage2 = new ArrayList<>();
                                for (String tmpVal : tmpStorage) {
                                    if (tmpVal.matches(".*\\d.*")) { //si le valeur contient un nombre
                                        this.erreur = true;
                                        this.message_erreur = "Une valeur numerique a ete saisie pour le mot-clef DE";
                                        break;
                                    }
                                    else tmpStorage2.add(tmpVal);
                                }
                                array2D.add(tmpStorage2);

                                for (ArrayList<String> strings : array2D) {
                                    if (!where_created) {
                                        where_created = true;
                                        sql.append("\nWHERE (");
                                    }
                                    else sql.append("\nOR (");

                                    for (int k = 0; k < strings.size(); k++) {
                                        if (k > 0) sql.append("\nOR");
                                        sql.append(" f.id_film IN (SELECT id_film FROM personnes NATURAL JOIN generique");
                                        sql.append(" WHERE (prenom_sans_accent || ' ' || nom_sans_accent LIKE '%").append(strings.get(k)).append("%' OR nom_sans_accent || ' ' || prenom_sans_accent LIKE '%").append(strings.get(k)).append("%' OR nom_sans_accent LIKE '%").append(strings.get(k)).append("%')");
                                        sql.append(" AND role = 'R')");
                                    }
                                    sql.append(")");
                                }
                                array2D.clear();
                                break;
                            }
                            case "AVEC": {
                                ArrayList<String> tmpStorage2 = new ArrayList<>();
                                for (String tmpVal : tmpStorage) {
                                    if (tmpVal.matches(".*\\d.*")) { //si la valeur contient un nombre
                                        this.erreur = true;
                                        this.message_erreur = "Une valeur numerique a ete saisie pour le mot-clef AVEC";
                                        break;
                                    }
                                    else tmpStorage2.add(tmpVal);
                                }

                                array2D.add(tmpStorage2);
                                for (ArrayList<String> strings : array2D) {
                                    if (!where_created) {
                                        where_created = true;
                                        sql.append("\nWHERE (");
                                    }
                                    else sql.append("\nOR (");

                                    for (int k = 0; k < strings.size(); k++) {
                                        if (k > 0) sql.append("\nOR");
                                        sql.append(" f.id_film IN (SELECT id_film FROM personnes NATURAL JOIN generique");
                                        sql.append(" WHERE (prenom_sans_accent || ' ' || nom_sans_accent LIKE '%").append(strings.get(k)).append("%' OR nom_sans_accent || ' ' || prenom_sans_accent LIKE '%").append(strings.get(k)).append("%' OR nom_sans_accent LIKE '%").append(strings.get(k)).append("%')");
                                        sql.append(" AND role = 'A')");
                                    }
                                    sql.append(")");
                                }
                                array2D.clear();
                                break;
                            }
                            case "PAYS":
                                if (tmpStorage.get(0).matches(".*\\d.*")) { //si le valeur contient un nombre
                                    this.erreur = true;
                                    this.message_erreur = "Une valeur numerique a ete saisie pour le mot-clef PAYS";
                                    break label;
                                }
                                else {
                                    if (!where_created) {
                                        where_created = true;
                                        sql.append("\nWHERE (");
                                    }
                                    else sql.append("\nOR (");

                                    for (int j = 0; j < tmpStorage.size(); j++) {
                                        if (j > 0) sql.append("\nOR");
                                        sql.append(" py.code LIKE '%").append(tmpStorage.get(j)).append("%' OR py.nom LIKE '%").append(tmpStorage.get(j)).append("%'");
                                    }
                                    sql.append(")");
                                }
                                PAYS_filled = true;
                                break;
                            case "EN": {
                                ArrayList<Integer> tmpStorage2 = new ArrayList<>();
                                for (String tmpVal : tmpStorage) {
                                    try {
                                        tmpStorage2.add(Integer.valueOf(tmpVal));
                                    }
                                    catch (NumberFormatException err) {
                                        this.erreur = true;
                                        this.message_erreur = "Une valeur non-numerique a ete saisie pour le mot-clef EN : [" + err.getMessage().replace('\"', '\'') + "]";
                                        break;
                                    }
                                }

                                if (!where_created) {
                                    where_created = true;
                                    sql.append("\nWHERE (");
                                }
                                else sql.append("\nOR (");

                                for (int j = 0; j < tmpStorage2.size(); j++) {
                                    if (j > 0) sql.append("\nOR");
                                    sql.append(" annee = ").append(tmpStorage2.get(j));
                                }
                                sql.append(")");
                                EN_filled = true;
                                break;
                            }
                            case "AVANT":
                                ArrayList<Integer> tmpAvant = new ArrayList<>();
                                for (String tmpVal : tmpStorage) {
                                    try {
                                        tmpAvant.add(Integer.valueOf(tmpVal));
                                    }
                                    catch (NumberFormatException err) {
                                        this.erreur = true;
                                        this.message_erreur = "Une valeur non-numerique a ete saisie pour le mot-clef AVANT : [" + err.getMessage().replace('\"', '\'') + "]";
                                        break;
                                    }
                                }

                                if (!where_created) {
                                    where_created = true;
                                    sql.append("\nWHERE (");
                                }
                                else sql.append("\nOR (");

                                if (tmpAvant.size() > 0) sql.append(" annee < ").append(Collections.max(tmpAvant)).append(")");
                                break;
                            case "APRES":
                                ArrayList<Integer> tmpApres = new ArrayList<>();
                                for (String tmpVal : tmpStorage) {
                                    try {
                                        tmpApres.add(Integer.valueOf(tmpVal));
                                    }
                                    catch (NumberFormatException err) {
                                        this.erreur = true;
                                        this.message_erreur = "Une valeur non-numerique a ete saisie pour le mot-clef APRES : [" + err.getMessage().replace('\"', '\'') + "]";
                                        break;
                                    }
                                }
                                if (!where_created) {
                                    where_created = true;
                                    sql.append("\nWHERE (");
                                }
                                else sql.append("\nOR (");

                                if (tmpApres.size() > 0) sql.append(" annee > ").append(Collections.min(tmpApres)).append(")");
                                break;
                        }
                        newField = true;
                        tmpStorage.clear();
                    }
                    else {
                        tmpStorage.add(value.toString().trim());
                        value = new StringBuilder();
                    }
                }
                else if (str.equals(",")) { // Si le mot actuel lu est une virgule
                    if (list[i+1] == null || list[i+1].equals(",")) {
                        this.erreur = true;
                        this.message_erreur = "Surplus d'une virgule avant " + list[i-1] + ". Sinon, une valeur est attendue.";
                        break;
                    }

                    tmpStorage.add(value.toString().trim());
                    value = new StringBuilder();

                    switch (field) {
                        case "TITRE":
                            if (!where_created) {
                                where_created = true;
                                sql.append("\nWHERE (");
                            }
                            else sql.append("\nAND (");

                            for (int j = 0; j < tmpStorage.size(); j++) {
                                if (j > 0) sql.append(" OR");
                                sql.append(" f.id_film IN (SELECT id_film FROM recherche_titre rt WHERE rt.titre LIKE '%' || replace('").append(tmpStorage.get(j)).append("', ' ', '%') || '%')");
                            }
                            sql.append(")");
                            TITRE_filled = true;
                            break;
                        case "DE": {
                            ArrayList<String> tmpStorage2 = new ArrayList<>();
                            for (String tmpVal : tmpStorage) {
                                if (tmpVal.matches(".*\\d.*")) { // Si le valeur contient un nombre
                                    this.erreur = true;
                                    this.message_erreur = "Une valeur numerique a ete saisie pour le mot-clef DE";
                                    break;
                                }
                                else tmpStorage2.add(tmpVal);
                            }
                            array2D.add(tmpStorage2);

                            for (ArrayList<String> strings : array2D) {
                                if (!where_created) {
                                    sql.append("\nWHERE (");
                                    where_created = true;
                                }
                                else if (or_btwn_kw) {
                                    or_btwn_kw = false;
                                    sql.append("\n OR (");
                                }
                                else sql.append("\nAND (");

                                for (int k = 0; k < strings.size(); k++) {
                                    if (k > 0) sql.append("\nOR");
                                    sql.append(" f.id_film IN (SELECT id_film FROM personnes NATURAL JOIN generique");
                                    sql.append(" WHERE (prenom_sans_accent || ' ' || nom_sans_accent LIKE '%").append(strings.get(k)).append("%' OR nom_sans_accent || ' ' || prenom_sans_accent LIKE '%").append(strings.get(k)).append("%' OR nom_sans_accent LIKE '%").append(strings.get(k)).append("%')");
                                    sql.append(" AND role = 'R')");
                                }
                                sql.append(")");
                            }
                            array2D.clear();
                            break;
                        }
                        case "AVEC": {
                            ArrayList<String> tmpStorage2 = new ArrayList<>();
                            for (String tmpVal : tmpStorage) {
                                if (tmpVal.matches(".*\\d.*")) { //si la valeur contient un nombre
                                    this.erreur = true;
                                    this.message_erreur = "Une valeur numerique a ete saisie pour le mot-clef AVEC";
                                    break;
                                }
                                else tmpStorage2.add(tmpVal);
                            }

                            array2D.add(tmpStorage2);
                            for (ArrayList<String> strings : array2D) {
                                if (!where_created) {
                                    sql.append("\nWHERE (");
                                    where_created = true;
                                }
                                else if (or_btwn_kw){
                                    or_btwn_kw = false;
                                    sql.append("\n OR (");
                                }
                                else sql.append("\nAND (");

                                for (int k = 0; k < strings.size(); k++) {
                                    if (k > 0) sql.append("\nOR");
                                    sql.append(" f.id_film IN (SELECT id_film FROM personnes NATURAL JOIN generique");
                                    sql.append(" WHERE (prenom_sans_accent || ' ' || nom_sans_accent LIKE '%").append(strings.get(k)).append("%' OR nom_sans_accent || ' ' || prenom_sans_accent LIKE '%").append(strings.get(k)).append("%' OR nom_sans_accent LIKE '%").append(strings.get(k)).append("%')");
                                    sql.append(" AND role = 'A')");
                                }
                                sql.append(")");
                            }
                            array2D.clear();
                            break;
                        }
                        case "PAYS":
                            if (tmpStorage.get(0).matches(".*\\d.*")) { //si le valeur contient un nombre
                                this.erreur = true;
                                this.message_erreur = "Une valeur numerique a ete saisie pour le mot-clef PAYS";
                                break label;
                            }
                            else {
                                if (!where_created) {
                                    sql.append("\nWHERE (");
                                    where_created = true;
                                }
                                else if (or_btwn_kw){
                                    or_btwn_kw = false;
                                    sql.append("\n OR (");
                                }
                                else sql.append("\nAND (");

                                for (int j = 0; j < tmpStorage.size(); j++) {
                                    if (j > 0) sql.append("\nOR");
                                    sql.append(" py.code LIKE '%").append(tmpStorage.get(j)).append("%' OR py.nom LIKE '%").append(tmpStorage.get(j)).append("%'");
                                }
                                sql.append(")");
                            }
                            PAYS_filled = true;
                            break;
                        case "EN": {
                            ArrayList<Integer> tmpStorage2 = new ArrayList<>();
                            for (String tmpVal : tmpStorage) {
                                try {
                                    tmpStorage2.add(Integer.valueOf(tmpVal));
                                }
                                catch (NumberFormatException err) {
                                    this.erreur = true;
                                    this.message_erreur = "Une valeur non-numerique a ete saisie pour le mot-clef EN : [" + err.getMessage().replace('\"', '\'') + "]";
                                    break;
                                }
                            }

                            if (!where_created) {
                                where_created = true;
                                sql.append("\nWHERE (");
                            }
                            else if (or_btwn_kw){
                                or_btwn_kw = false;
                                sql.append("\n OR (");
                            }
                            else sql.append("\nAND (");

                            for (int j = 0; j < tmpStorage2.size(); j++) {
                                if (j > 0) sql.append("\nOR");
                                sql.append(" annee = ").append(tmpStorage2.get(j));
                            }
                            sql.append(")");
                            EN_filled = true;
                            break;
                        }
                        case "AVANT":
                            ArrayList<Integer> tmpAvant = new ArrayList<>();
                            for (String tmpVal : tmpStorage) {
                                try {
                                    tmpAvant.add(Integer.valueOf(tmpVal));
                                }
                                catch (NumberFormatException err) {
                                    this.erreur = true;
                                    this.message_erreur = "Une valeur non-numerique a ete saisie pour le mot-clef AVANT : [" + err.getMessage().replace('\"', '\'') + "]";
                                    break;
                                }
                            }

                            if (!where_created) {
                                where_created = true;
                                sql.append("\nWHERE (");
                            }
                            else if (or_btwn_kw){
                                or_btwn_kw = false;
                                sql.append("\n OR (");
                            }
                            else sql.append("\nAND (");

                            if (tmpAvant.size() > 0) sql.append(" annee < ").append(Collections.max(tmpAvant)).append(")");
                            break;
                        case "APRES":
                            ArrayList<Integer> tmpApres = new ArrayList<>();
                            for (String tmpVal : tmpStorage) {
                                try {
                                    tmpApres.add(Integer.valueOf(tmpVal));
                                }
                                catch (NumberFormatException err) {
                                    this.erreur = true;
                                    this.message_erreur = "Une valeur non-numerique a ete saisie pour le mot-clef APRES : [" + err.getMessage().replace('\"', '\'') + "]";
                                    break;
                                }
                            }
                            if (!where_created) {
                                where_created = true;
                                sql.append("\nWHERE (");
                            }
                            else if (or_btwn_kw){
                                or_btwn_kw = false;
                                sql.append("\n OR (");
                            }
                            else sql.append("\nAND (");

                            if (tmpApres.size() > 0) sql.append(" annee > ").append(Collections.min(tmpApres)).append(")");
                            break;
                    }
                    newField = true;
                    tmpStorage.clear();
                }
                else value.append(str).append(" ");// Si le mot actuel lu fait partie de la valeur du champ comme un nom compose
            }
        }
        sql.append(ORDER_BY_SQL);
        return sql.toString();
    }

    /**
     * Ordre des colonnes dans le resultSet (-1 pour l'ArrayList) :
     * [1] f.id_film (ID du film)
     * [2] prenom [3] p.nom (prenom/nom d'une personne)
     * [4] titre (titre du film)
     * [5] duree (duree du film en minutes)
     * [6] annee (annee de sortie du film)
     * [7] py.nom (nom du pays en entier)
     * [8] role (role de la personne => 'A' : acteur, 'R' : realisateur)
     * [9] liste des autres titres sur une ligne
     * @param sql resultSet de la requete SQL construite a partir du pseudo-langage
     * @return ArrayList<java_project.InfoFilm> liste des films
     */
    public ArrayList<InfoFilm> getInfoFilmArray(String sql){
        ArrayList<InfoFilm> filmsList = new ArrayList<>();
        ArrayList<ArrayList<String>> liste = bdd.ArrayResultSQL(sql); // ResultSet converti
        ArrayList<NomPersonne> realisateurs = new ArrayList<>(), acteurs = new ArrayList<>();
        ArrayList<String> autres_titres = new ArrayList<>();
        int duree, annee;
        String pays, titre;

        for (int i = 0; i < liste.size(); i++) {
            if (liste.get(i).get(7).equals("A")) {
                String prenom_act = "";
                if (liste.get(i).get(1) != null) prenom_act = liste.get(i).get(1);
                acteurs.add(new NomPersonne(prenom_act, liste.get(i).get(2)));
            }
            else if (liste.get(i).get(7).equals("R")) {
                String prenom_real = "";
                if (liste.get(i).get(1) != null) prenom_real = liste.get(i).get(1);
                realisateurs.add(new NomPersonne(prenom_real, liste.get(i).get(2)));
            }

            titre = liste.get(i).get(3);
            duree = (liste.get(i).get(4) != null) ? Integer.valueOf(liste.get(i).get(4)) : 0;
            annee = (liste.get(i).get(5) != null) ? Integer.valueOf(liste.get(i).get(5)) : 0;
            pays = (liste.get(i).get(5) != null) ? liste.get(i).get(6) : "";

            if (liste.get(i).get(8) != null && autres_titres.isEmpty()) {
                String[] autres_titres_list_splited = liste.get(i).get(8).split("#");
                Collections.addAll(autres_titres, autres_titres_list_splited);
            }

            // Nouveau film lu ou fin de la liste : on cree et ajoute une nouvelle instance d'java_project.InfoFilm dans l'ArrayList
            if (i == (liste.size()-1) || !Integer.valueOf(liste.get(i).get(0)).equals(Integer.valueOf(liste.get(i + 1).get(0)))) {
                filmsList.add(new InfoFilm(titre, realisateurs, acteurs, pays, annee, duree, autres_titres));
                acteurs = new ArrayList<>();
                realisateurs = new ArrayList<>();
                autres_titres = new ArrayList<>();
            }
        }

        bdd.fermeBase();
        return filmsList;
    }

    /**
     * Permet de convertir le ResultSet en ArrayList<ArrayList<String>> car le driver jdbc ne permet pas de changer les parametres de lecture du ResultSet et il n'est pas possible d'acceder a n+1 avec isNext() pour savoir si le prochain film est une nouvelle entree.
     * @param set ResultSet obtenu
     * @return ArrayList<ArrayList<String>> ResultSet converti
     * @throws SQLException Se lance si le ResultSet est vide
     */
    public ArrayList<ArrayList<String>> convertRStoAL(ResultSet set) throws SQLException {
        ArrayList<ArrayList<String>> set_to_at = new ArrayList<>();
        do {
            ArrayList<String> liste_simple = new ArrayList<>();
            for (int i = 1; i <= 9; i++) liste_simple.add(set.getString(i));
            set_to_at.add(liste_simple);
        } while (set.next());
        set.close();
        return set_to_at;
    }

    /**
     * Permet de formatter le json final lisible par un lecteur de json, comme jq dans le terminal
     * @param list Tableau d'InfoFilm
     * @return String json enfin retourne
     */
    public String convertToJSON(ArrayList<InfoFilm> list) {
        StringBuilder result = new StringBuilder();
        int n;
        if (list.size() >= 100) {
            n = 100;
            result.append("{\"info\":\"Resultat limite a 100 films\", \"resultat\":[ ");
        }
        else {
            n = list.size();
            result.append("{\"resultat\":[ ");
        }

        for (int i = 0; i < n; i++) {
            if (i > 0) result.append(",\n");
            result.append(list.get(i).toString());
        }
        result.append("]}");
        return result.toString();
    }
}