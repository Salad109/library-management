package librarymanagement.testdata;

public class BookTestData {

    public static class ValidBook1 {
        public static final String TITLE = "Test Book";
        public static final String ISBN = "9781234567890";
        public static final Integer PUBLICATION_YEAR = 2025;
        public static final String AUTHOR_1 = "Joe Mama";
        public static final String AUTHOR_2 = "Jane Mama";

        public static final String JSON = """
            {
                "title": "%s",
                "publicationYear": %d,
                "isbn": "%s",
                "authors": [
                    {
                        "name": "%s"
                    },
                    {
                        "name": "%s"
                    }
                ]
            }
            """.formatted(TITLE, PUBLICATION_YEAR, ISBN, AUTHOR_1, AUTHOR_2);
    }

    public static class ValidBook2 {
        public static final String TITLE = "Test Book 2";
        public static final String ISBN = "9791234567890";
        public static final Integer PUBLICATION_YEAR = 1984;
        public static final String AUTHOR_1 = "Joe Mama";

        public static final String JSON = """
            {
                "title": "%s",
                "publicationYear": %d,
                "isbn": "%s",
                "authors": [
                    {
                        "name": "%s"
                    }
                ]
            }
            """.formatted(TITLE, PUBLICATION_YEAR, ISBN, AUTHOR_1);
    }

    public static class InvalidBookNoTitleInvalidIsbn {
        public static final String ISBN = "no";
        public static final Integer PUBLICATION_YEAR = 2021;
        public static final String AUTHOR_1 = "Jane Mama";

        public static final String JSON = """
            {
                "publicationYear": %d,
                "authors": [
                    {
                        "name": "%s"
                    }
                ],
                "isbn": "%s"
            }
            """.formatted(PUBLICATION_YEAR, AUTHOR_1, ISBN);
    }
}