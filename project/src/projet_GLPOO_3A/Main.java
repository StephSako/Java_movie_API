package projet_GLPOO_3A;

public class Main {

    public static void main(String[] args) {

        RechercheFilm r = new RechercheFilm("../bdd/bdfilm.sqlite");
        System.out.println(r.retrouve(String.join(" ", args)));
    }
}