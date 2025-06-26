package librarymanagement.testdata;

public class CopyTestData {

    public static class ValidCopy1 {
        public static final String ISBN = BookTestData.ValidBook1.ISBN;
        public static final String STATUS = "AVAILABLE";

        public static final String JSON = """
                {
                    "book": {
                        "isbn": "%s"
                    },
                    "status": "%s"
                }
                """.formatted(ISBN, STATUS);
    }

    public static class ValidCopy2 {
        public static final String ISBN = BookTestData.ValidBook2.ISBN;
        public static final String STATUS = "RESERVED";

        public static final String JSON = """
                {
                    "book": {
                        "isbn": "%s"
                    },
                    "status": "%s"
                }
                """.formatted(ISBN, STATUS);
    }

    public static class ValidCopy3 {
        public static final String ISBN = BookTestData.ValidBook1.ISBN;
        public static final String STATUS = "BORROWED";

        public static final String JSON = """
                {
                    "book": {
                        "isbn": "%s"
                    },
                    "status": "%s"
                }
                """.formatted(ISBN, STATUS);
    }

    public static class InvalidCopyInvalidStatus {
        public static final String ISBN = BookTestData.ValidBook1.ISBN;
        public static final String STATUS = "INVALID_STATUS";

        public static final String JSON = """
                {
                    "book": {
                        "isbn": "%s"
                    },
                    "status": "%s"
                }
                """.formatted(ISBN, STATUS);
    }

    public static class InvalidCopyInvalidBook {
        public static final String ISBN = "9999999999999";
        public static final String STATUS = "AVAILABLE";

        public static final String JSON = """
                {
                    "book": {
                        "isbn": "%s"
                    },
                    "status": "%s"
                }
                """.formatted(ISBN, STATUS);
    }

    public static class InvalidCopyNoBook {
        public static final String STATUS = "AVAILABLE";

        public static final String JSON = """
                {
                    "status": "%s"
                }
                """.formatted(STATUS);
    }

    public static class InvalidCopyNullStatus {
        public static final String ISBN = BookTestData.ValidBook1.ISBN;

        public static final String JSON = """
                {
                    "book": {
                        "isbn": "%s"
                    },
                    "status": null
                }
                """.formatted(ISBN);
    }
}