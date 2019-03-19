/**
 * Gestion des noms (noms de famille + prenom) des personnes.
 * <p>
 * La classe NomPersonne permet de gerer les noms et tient en particulier compte des prefixes des noms ('de', 'von', 'van') dans le tri.
 */
public class NomPersonne implements Comparable<NomPersonne>{
    private String _nom;
    private String _prenom;
    private int    _debutComp;

    /**
     * Creation d'un nouveau NomPersonne, le prenom est passe en deuxieme.
     *  @param nom Nom de famille ou nom d'artiste.
     *  @param prenom Prenom (peut etre "null").
     */
    public NomPersonne(String nom, String prenom) {
        _nom = new String(nom);
        _prenom = new String(prenom);
        _debutComp = 0;
        // On regarde quel est le premier caractère en majuscules pour trier.
        // 'von Stroheim' avec les S, 'de la Huerta' avec les H et 'de Funès' avec les F.
        // 'De Niro' sera en revanche à D.
        while ((_debutComp < _nom.length())
                && (_nom.charAt(_debutComp)
                == Character.toLowerCase(_nom.charAt(_debutComp)))) {
            _debutComp++;
        }
    }

    /**
     * Comparateur qui tient compte des prefixes de noms.
     *  @param autre NomPersonne qui est compare a l'objet courant.
     *  @return Un entier inferieur, egal ou superieur a zero suivant le cas.
     */
    @Override
    public int compareTo(NomPersonne autre) {
        if (autre == null) {
            return 1;
        }
        int cmp = this._nom.substring(this._debutComp)
                .compareTo(autre._nom.substring(autre._debutComp));
        if (cmp == 0) {
            return this._prenom.compareTo(autre._prenom);
        } else {
            return cmp;
        }
    }

    /**
     * Retourne un nom affichable.
     * <p>
     * S'il y a une mention telle que (Jr.) qui dans la base est dans la colonne du prenom, elle est reportee a la fin.
     *  @return La combinaison du prenom et du nom, dans cet ordre.
     */
    @Override
    public String toString() {
        int pos = -1;

        if (this._prenom != null) {
            // Les mentions entre parentheses seront renvoyees a la fin
            pos = this._prenom.indexOf('(');
        }
        if (pos == -1) {
            if (this._prenom == null) {
                return this._nom;
            } else {
                return this._prenom + " " + this._nom;
            }
        } else {
            return this._prenom.substring(0, pos-1).trim()
                    + " " + this._nom
                    + " " + this._prenom.substring(pos).trim();
        }
    }
}
