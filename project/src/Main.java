public class Main {

    public static void main(String[] args) {

        // testing
        RechercheFilm r = new RechercheFilm("bdd/bdfilm.sqlite");
        System.out.println(r.retrouve("TITRE Avatar"));
    }

}
