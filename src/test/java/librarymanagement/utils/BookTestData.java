package librarymanagement.utils;

public final class BookTestData {
    private BookTestData() {
    }

    public static final class TestBook1 {
        public static final String ISBN = "9781234567891";
        public static final String TITLE = "1984 2";
        public static final int PUBLICATION_YEAR = 2077;
        public static final String AUTHOR_NAME = "George Orwell Jr.";
        public static final String JSON = """
                {
                    "isbn": "%s",
                    "title": "%s",
                    "publicationYear": %d,
                    "authorNames": [
                        "%s"
                    ]
                }
                """.formatted(ISBN, TITLE, PUBLICATION_YEAR, AUTHOR_NAME);
        private TestBook1() {
        }
    }

    public static class TestBook2 {
        public static final String ISBN = "123456788X";
        public static final String TITLE = "The Giant Prince";
        public static final int PUBLICATION_YEAR = 1944;
        public static final String AUTHOR_NAME = "Antoine de Saint-Exupéry Jr.";
        public static final String JSON = """
                {
                    "isbn": "%s",
                    "title": "%s",
                    "publicationYear": %d,
                    "authorNames": [
                        "%s"
                    ]
                }
                """.formatted(ISBN, TITLE, PUBLICATION_YEAR, AUTHOR_NAME);
        private TestBook2() {
        }
    }
}
