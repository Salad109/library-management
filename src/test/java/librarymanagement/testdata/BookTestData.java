package librarymanagement.testdata;

public class BookTestData {

    private static final BookData[] BOOKS = {
            new BookData("The Goober Lore", "9781234567890", 2025, "The Goober", "Joe Mama"),
            new BookData("The Mama Family", "123456789X", 2026, "Joe Mama", "Jane Mama"),
            new BookData("Junior's Junipers", "9791234567890", 1984, "Joe Mama Jr.", "The Goober"),
            new BookData("Charlie's Cherries", "9781234567891", 1999, "Charlie Charles", "Charlie Charles Jr."),
            new BookData("Builder's Buildings", "9791234567891", 2027, "Bob Builder", "Alice Allison")
    };
    private static int bookIndex = 0;

    public static BookData getNextBook() {
        return BOOKS[++bookIndex % BOOKS.length];
    }

    public static BookData getCurrentBook() {
        return BOOKS[bookIndex % BOOKS.length];
    }

    public static class BookData {
        public final String TITLE;
        public final String ISBN;
        public final Integer PUBLICATION_YEAR;
        public final String AUTHOR1;
        public final String AUTHOR2;
        public final String JSON;

        public BookData(String title, String isbn, Integer publicationYear, String author1, String author2) {
            this.TITLE = title;
            this.ISBN = isbn;
            this.PUBLICATION_YEAR = publicationYear;
            this.AUTHOR1 = author1;
            this.AUTHOR2 = author2;
            this.JSON = """
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
                    """.formatted(title, publicationYear, isbn, author1, author2);
        }
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