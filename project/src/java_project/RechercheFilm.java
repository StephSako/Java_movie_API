package java_project;

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
class RechercheFilm {

    private class MoviePseudoRequest {
        ArrayList<String> TITRE = new ArrayList<>();
        ArrayList<ArrayList<String>> DE = new ArrayList<>(); // LIGNES = ET, COLONNES = OU
        ArrayList<ArrayList<String>> AVEC = new ArrayList<>(); // LIGNES = ET, COLONNES = OU
        ArrayList<String> PAYS = new ArrayList<>();
        ArrayList<Integer> EN = new ArrayList<>();
        ArrayList<Integer> AVANT = new ArrayList<>();
        ArrayList<Integer> APRES = new ArrayList<>();

        boolean erreur = false;
        String message_erreur = "";

        @Override
        public String toString() {
            String str = "";
            str+="TITRE "+TITRE.toString()+"\n";
            str+="DE "+DE.toString()+"\n";
            str+="AVEC "+AVEC.toString()+"\n";
            str+="PAYS "+PAYS.toString()+"\n";
            str+="EN "+EN.toString()+"\n";
            str+="AVANT "+AVANT.toString()+"\n";
            str+="APRES "+APRES.toString()+"\n";
            return str;
        }
    }

    private BDDManager bdd;

    /**
     * Constructeur, ouvre la BDD.
     * @param nomFichierSQLite Nom du ficher BDD.
     */
    RechercheFilm(String nomFichierSQLite) {
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
    String retrouve(String requete) {
        /**
        MoviePseudoRequest moviePseudoRequestTest = formatRequest("TITRE intouchables, DE moi, AVEC Audrey Hepburn OU Marilyn Monroe, Kirk Douglas, ladydi, PAYS ukraine, EN 1950, AVANT 1960, APRES 1720");
        String sqlTest = convertToSQL(moviePseudoRequestTest);
        /**/

        MoviePseudoRequest moviePseudoRequest = formatRequest(requete);
        if (!moviePseudoRequest.erreur){
            String sql = convertToSQL(moviePseudoRequest);

            ArrayList<InfoFilm> list = getInfoFilmArray(sql);
            return convertToJSON(list);
        } else return "{\"erreur\":\"" + moviePseudoRequest.message_erreur + "\"}"; // Envoi de l'erreur

    }

    private MoviePseudoRequest formatRequest(String requete) { // TODO En cours

        MoviePseudoRequest infos = new MoviePseudoRequest();
        requete += ",END";

        String field="";
        String value="";
        ArrayList<String> tmpStorage = new ArrayList<>();
        boolean newField = true;
        String[] possibleTerms = {"TITRE", "DE", "AVEC", "PAYS", "EN", "AVANT", "APRES"};

        String[] list = requete.split(" |((?<=,)|(?=,))");
        for (int i=0; i<list.length; i++) //pour chaque mot de la recherche
        {
            String str = list[i];
            if (newField) //si oregarde un mot-clef
            {
                if (Arrays.asList(possibleTerms).contains(str)) //si le mot-clef fait partie des mots-clefs valables
                {
                    //s'il s'agit d'un champ qui ne peut pas prendre de ET, et que le champ est deja pris
                    if (str.equals("TITRE") && !infos.TITRE.isEmpty()) {
                        infos.erreur = true;
                        infos.message_erreur = "multiples champs TITRE";
                        break;
                    }
                    else if (str.equals("PAYS") && !infos.PAYS.isEmpty()) {
                        infos.erreur = true;
                        infos.message_erreur = "multiples champs PAYS";
                        break;
                    }
                    else if (str.equals("EN") && !infos.EN.isEmpty()) {
                        infos.erreur = true;
                        infos.message_erreur = "multiples champs EN";
                        break;
                    }

                    else //si tout va bien pour le mot clef
                    {
                        field = str;
                        newField = false;
                    }
                }
                else //si le mot n'est pas un mot-clef valable
                {
                    if (str.equals("END")) {
                        System.out.println(i+" OH");
                    }
                    else if (i==0) //si c'est le 1er mot-clef de la requete
                    {
                        infos.erreur = true;
                        infos.message_erreur = "1er champ invalide";
                        break;
                    }
                }
            }

            else //si on regarde la valeur d'un champ
            {
                if (str.equals("OU")) //si le mot actuel est "OU"
                {
                    tmpStorage.add(value.trim());
                    value="";
                }
                else if (list[i].equals(","))  //si le " mot " actuel est une virgule
                {
                    tmpStorage.add(value.trim());
                    value="";

                    if (field.equals("TITRE")) {
                        infos.TITRE = new ArrayList<>(tmpStorage);
                    }

                    // LIGNES = ET, COLONNES = OU
                    // PEUT IMPORTE 1, 2 OU PLUS DE 3 MOTS POUR LE NOM ET PRENOM : SQL FAIT TOUT
                    // NE PAS INSERER D'ESPACE DANS LE NOM COMPLET : TOUT COLLER !!
                    else if (field.equals("DE")) {
                        ArrayList<String> tmpStorage2 = new ArrayList<>();
                        for (String tmpVal : tmpStorage) {
                            tmpStorage2.add(tmpVal.replaceAll("\\s+",""));
                        }
                        infos.DE.add(tmpStorage2);
                    }

                    // LIGNES = ET, COLONNES = OU
                    // PEUT IMPORTE 1, 2 OU PLUS DE 3 MOTS POUR LE NOM ET PRENOM : SQL FAIT TOUT
                    // NE PAS INSERER D'ESPACE DANS LE NOM COMPLET : TOUT COLLER !!
                    else if (field.equals("AVEC")) {
                        ArrayList<String> tmpStorage2 = new ArrayList<>();
                        for (String tmpVal : tmpStorage) {
                            tmpStorage2.add(tmpVal.replaceAll("\\s+",""));
                        }
                        infos.AVEC.add(tmpStorage2);
                    }

                    else if (field.equals("PAYS")) {
                        infos.PAYS = new ArrayList<>(tmpStorage);
                    }

                    else if (field.equals("EN")) {
                        ArrayList<Integer> tmpStorage2 = new ArrayList<>();
                        for (String tmpVal : tmpStorage) {
                            try {
                                tmpStorage2.add(Integer.valueOf(tmpVal));
                            } catch(NumberFormatException err) {
                                infos.erreur = true;
                                infos.message_erreur = "champ 'EN' non numerique ["+err.getMessage()+"]";
                                break;
                            }
                        }
                        infos.EN = tmpStorage2;
                    }

                    else if (field.equals("AVANT")) {
                        for (String tmpVal : tmpStorage) {
                            try {
                                infos.AVANT.add(Integer.valueOf(tmpVal));
                            } catch(NumberFormatException err) {
                                infos.erreur = true;
                                infos.message_erreur = "champ 'AVANT' non numerique ["+err.getMessage()+"]";
                                break;
                            }
                        }
                    }

                    else if (field.equals("APRES")) {
                        for (String tmpVal : tmpStorage) {
                            try {
                                infos.APRES.add(Integer.valueOf(tmpVal));
                            } catch(NumberFormatException err) {
                                infos.erreur = true;
                                infos.message_erreur = "champ 'APRES' non numerique ["+err.getMessage()+"]";
                                break;
                            }
                        }
                    }

                    newField = Arrays.asList(possibleTerms).contains(list[i+1]);
                    tmpStorage.clear();

                }
                else //si le mot actuel fait partie de la valeur du champ
                {
                    value=value+str+" ";
                }

            }
        }
        
        // Afficher objet MoviePseudoRequest
        System.out.println(infos);

        return infos;
    }

    private String convertToSQL(MoviePseudoRequest moviePseudoRequestmap) {
        StringBuilder reqSQL = new StringBuilder();
        String SELECT = "SELECT f.id_film as id_film_f, prenom, p.nom as nom_p, f.titre as titre_f, duree, annee, py.nom as nom_py, role, (select group_concat(a_t.titre, '#') from autres_titres a_t where a_t.id_film=f.id_film) as liste_autres_titres";

        // Chaîne du FROM
        String FROM = "\nFROM films f NATURAL JOIN generique g NATURAL JOIN personnes p LEFT JOIN pays py ON f.pays = py.code";

        String ORDER_BY_SQL = "\nORDER BY annee DESC, f.titre";

        StringBuilder AVEC_SQL = new StringBuilder();
        StringBuilder PAYS_SQL = new StringBuilder();
        StringBuilder TITRE_SQL = new StringBuilder();
        StringBuilder DE_SQL = new StringBuilder();
        StringBuilder AVANT_SQL = new StringBuilder();
        StringBuilder APRES_SQL = new StringBuilder();
        StringBuilder EN_SQL = new StringBuilder();

        // Permet de savoir si le mot clef WHERE a déjà été ajouté à la requête (avant le(s) 'AND')
        boolean where_created = false;

        // AVEC (acteur(s))
        if (!moviePseudoRequestmap.AVEC.isEmpty()){
            for (int i = 0; i < moviePseudoRequestmap.AVEC.size(); i++) {

                if (i == 0){
                    AVEC_SQL.append("\nWHERE (");
                    where_created = true;
                }
                else AVEC_SQL.append("\nAND (");

                for (int j = 0; j < moviePseudoRequestmap.AVEC.get(i).size(); j++) {

                    if (j > 0) AVEC_SQL.append("\nOR");

                    AVEC_SQL.append(" f.id_film IN (SELECT id_film FROM personnes NATURAL JOIN generique");
                    // Permet de ne pas s'embêter avec l'ordre du nom et prénoms, et si la personne n'a que son nom de renseigné
                    AVEC_SQL.append(" WHERE REPLACE(prenom_sans_accent || nom_sans_accent,' ','') LIKE '%").append(moviePseudoRequestmap.AVEC.get(i).get(j)).append("%' OR REPLACE(nom_sans_accent || prenom_sans_accent,' ','') LIKE '%").append(moviePseudoRequestmap.AVEC.get(i).get(j)).append("%' OR REPLACE(nom_sans_accent,' ','') LIKE '%").append(moviePseudoRequestmap.AVEC.get(i).get(j)).append("%'");
                    AVEC_SQL.append(" AND role = 'A')");
                }
                AVEC_SQL.append(")");
            }
        }

        // DE (réalisateur(s))
        if (!moviePseudoRequestmap.DE.isEmpty()){
            for (int i = 0; i < moviePseudoRequestmap.DE.size(); i++) {
                if (!where_created){
                    DE_SQL.append("\nWHERE (");
                    where_created = true;
                }
                else DE_SQL.append("\nAND (");

                for (int j = 0; j < moviePseudoRequestmap.DE.get(i).size(); j++) {

                    if (j > 0) DE_SQL.append("\nOR");

                    // 3 lignes supplémentaires au cas où l'utilisateur saisie des accent, un l'un et/ou à l'autre, ou pas du tout
                    DE_SQL.append(" f.id_film IN (SELECT id_film FROM personnes NATURAL JOIN generique");
                    DE_SQL.append(" WHERE REPLACE(prenom_sans_accent || nom_sans_accent,' ','') LIKE '%").append(moviePseudoRequestmap.DE.get(i).get(j)).append("%' OR REPLACE(nom_sans_accent || prenom_sans_accent,' ','') LIKE '%").append(moviePseudoRequestmap.DE.get(i).get(j)).append("%' OR REPLACE(nom_sans_accent,' ','') LIKE '%").append(moviePseudoRequestmap.DE.get(i).get(j)).append("%'");
                    DE_SQL.append(" AND role = 'R')");
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
                // Permet une recherche sur les autres titres
                TITRE_SQL.append(" f.id_film IN (SELECT id_film FROM recherche_titre rt WHERE rt.titre LIKE '%").append(moviePseudoRequestmap.TITRE.get(i)).append("%')");
                TITRE_SQL.append(" OR f.titre LIKE '%").append(moviePseudoRequestmap.TITRE.get(i)).append("%'");
            }
            TITRE_SQL.append(")");
        }

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
        else {
            // AVANT
            if (!moviePseudoRequestmap.AVANT.isEmpty()){
                if (where_created) AVANT_SQL.append("\nAND annee < ").append(Collections.max(moviePseudoRequestmap.AVANT));
                else {
                    APRES_SQL.append("\nWHERE annee < ").append(Collections.max(moviePseudoRequestmap.AVANT));
                    where_created = true;
                }
            }
            // APRES
            if(!moviePseudoRequestmap.APRES.isEmpty()) {
                if (where_created) APRES_SQL.append("\nAND annee > ").append(Collections.min(moviePseudoRequestmap.APRES));
                else {
                    APRES_SQL.append("\nWHERE annee > ").append(Collections.min(moviePseudoRequestmap.APRES));
                    where_created = true;
                }
            }
        }

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
     * Ordre des colonnes dans le resultSet (-1 pour l'ArrayList) :
     * [1] f.id_film (ID du film)
     * [2] prenom [3] p.nom (prénom/nom d'une personne)
     * [4] titre (titre du film)
     * [5] duree (duree du film en minutes)
     * [6] annee (annee de sortie du film)
     * [7] py.nom (nom du pays en entier)
     * [8] role (role de la personne => 'A' : acteur, 'R' : réalisateur)
     * [9] liste des autres titres sur une ligne
     * @param sql resultSet de la requête SQL construite à partir du pseudo-langage
     * @return ArrayList<java_project.InfoFilm> liste des films
     */
    private ArrayList<InfoFilm> getInfoFilmArray(String sql) {
        ArrayList<InfoFilm> filmsList = new ArrayList<>();

        try (ResultSet set = bdd.getCo().createStatement().executeQuery(sql)) {

            if (set.next()) { // Verifie si le ResultSet contient au moins un résultat
                ArrayList<ArrayList<String>> liste = convertRStoAL(set);

                // Champs de la classe java_project.InfoFilm
                ArrayList<NomPersonne> realisateurs = new ArrayList<>();
                ArrayList<NomPersonne> acteurs = new ArrayList<>();
                ArrayList<String> autres_titres = new ArrayList<>();
                int duree, annee;
                String pays, titre;

                for( int i = 0; i < liste.size(); i++) {

                    if (liste.get(i).get(7).equals("A")) {
                        String prenom_act = "";
                        if (liste.get(i).get(1) != null) prenom_act = liste.get(i).get(1);
                        acteurs.add(new NomPersonne(prenom_act, liste.get(i).get(2)));
                    } else if (liste.get(i).get(7).equals("R")) {
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

                    // Nouveau film lu ou fin de la liste : on créé et ajoute une nouvelle instance d'java_project.InfoFilm dans l'ArrayList
                    if (i == (liste.size()-1) || !Integer.valueOf(liste.get(i).get(0)).equals(Integer.valueOf(liste.get(i + 1).get(0)))) {
                        filmsList.add(new InfoFilm(titre, realisateurs, acteurs, pays, annee, duree, autres_titres));

                        // On vide les tableaux pour passer au film suivant
                        acteurs = new ArrayList<>();
                        realisateurs = new ArrayList<>();
                        autres_titres = new ArrayList<>();
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        bdd.fermeBase();

        return filmsList;
    }

    // Le driver JDBC SQLite ne supporte pas la fonction isLast() et ne permet pas de connaître les valeurs de set.next()
    // en étant à la ligne "actuelle" ;nous convertissons alors le ResultSet retourné en ArrayList<ArrayList<String>>
    // pour un traitement plus facile d'un point de vue algorithmique
    private ArrayList<ArrayList<String>> convertRStoAL(ResultSet set) throws SQLException {
        ArrayList<ArrayList<String>> set_to_at = new ArrayList<>();

        do {
            ArrayList<String> liste_simple = new ArrayList<>();
            for (int i = 1; i <= 9; i++) liste_simple.add(set.getString(i));
            set_to_at.add(liste_simple);
        } while (set.next());

        set.close();
        return set_to_at;
    }

    private String convertToJSON(ArrayList<InfoFilm> list) {
        StringBuilder result = new StringBuilder();

        int n;
        if (list.size() >= 100){
            n = 100;
            result.append("{\"info\":\"Résultat limité à 100 films\", \"resultat\":[ ");
        }
        else{
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
