package java_project;

public class Main {

    public static void main(String[] args) {

        RechercheFilm r = new RechercheFilm("bdd/bdfilm.sqlite");
        System.out.println(r.retrouve("EN 2009, AVEC John, Jim"));
    }
}
