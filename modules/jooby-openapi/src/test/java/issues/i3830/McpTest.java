/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3830;

import static org.assertj.core.api.Assertions.assertThat;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;
import issues.i3820.app.AppLib;

public class McpTest {

  @OpenAPITest(value = AppLib.class)
  public void shouldGenerateMcpMetadata(OpenAPIResult result) {
    assertThat(result.toMcp())
        .isEqualToIgnoringNewLines(
            """
            {
              "resourceTemplates" : {
                "getBook" : {
                  "uriTemplate" : "library://library/books/{isbn}",
                  "name" : "The Public Front Desk of the library.",
                  "description" : "View the full information for a single specific book using its unique ISBN.",
                  "mimeType" : "application/json",
                  "parameters" : {
                    "properties" : {
                      "isbn" : {
                        "type" : "string",
                        "description" : "The unique ID from the URL (e.g., /books/978-3-16-148410-0)"
                      }
                    },
                    "required" : [ "isbn" ]
                  }
                }
              },
              "tools" : {
                "searchBooks" : {
                  "name" : "search_books",
                  "description" : "Quick Search",
                  "inputSchema" : {
                    "properties" : {
                      "q" : {
                        "type" : "string",
                        "description" : "The word or phrase to search for."
                      }
                    }
                  },
                  "outputSchema" : {
                    "type" : "array",
                    "items" : {
                      "description" : "Represents a physical Book in our library. <p>This is the main item visitors look for. It holds details like the title, the actual text content, and who published it.",
                      "properties" : {
                        "isbn" : {
                          "type" : "string",
                          "description" : "The unique \\"barcode\\" for this book (ISBN). We use this to identify exactly which book edition we are talking about."
                        },
                        "title" : {
                          "type" : "string",
                          "description" : "The name printed on the cover."
                        },
                        "publicationDate" : {
                          "type" : "string",
                          "format" : "date",
                          "description" : "When this book was released to the public."
                        },
                        "text" : {
                          "type" : "string",
                          "description" : "The full story or content of the book. Since this can be very long, we store it in a special way (Large Object) to keep the database fast."
                        },
                        "type" : {
                          "type" : "string",
                          "description" : "Categorizes the item (e.g., is it a regular Book or a Magazine?).\\n  - NOVEL: A fictional narrative story. Examples: \\"Pride and Prejudice\\", \\"Harry Potter\\", \\"Dune\\". These are creative works meant for entertainment or artistic expression.\\n  - BIOGRAPHY: A written account of a real person's life. Examples: \\"Steve Jobs\\" by Walter Isaacson, \\"The Diary of a Young Girl\\". These are non-fiction historical records of an individual.\\n  - TEXTBOOK: An educational book used for study. Examples: \\"Calculus: Early Transcendentals\\", \\"Introduction to Java Programming\\". These are designed for students and are often used as reference material in academic courses.\\n  - MAGAZINE: A periodical publication intended for general readers. Examples: Time, National Geographic, Vogue. These contain various articles, are published frequently (weekly/monthly), and often have a glossy format.\\n  - JOURNAL: A scholarly or professional publication. Examples: The New England Journal of Medicine, Harvard Law Review. These focus on academic research or trade news and are written by experts for other experts.",
                          "enum" : [ "NOVEL", "BIOGRAPHY", "TEXTBOOK", "MAGAZINE", "JOURNAL" ]
                        },
                        "publisher" : {
                          "description" : "A company that produces and sells books.",
                          "properties" : {
                            "id" : {
                              "type" : "integer",
                              "format" : "int64",
                              "description" : "The unique internal ID for this publisher. This is a number generated automatically by the system. Users usually don't need to memorize this, but it's used by the database to link books to their publishers."
                            },
                            "name" : {
                              "type" : "string",
                              "description" : "The official business name of the publishing house. Example: \\"Penguin Random House\\" or \\"O'Reilly Media\\"."
                            }
                          }
                        },
                        "authors" : {
                          "type" : "array",
                          "description" : "The list of people who wrote this book.",
                          "items" : {
                            "description" : "A person who writes books.",
                            "properties" : {
                              "ssn" : {
                                "type" : "string",
                                "description" : "The author's unique government ID (SSN)."
                              },
                              "name" : {
                                "type" : "string",
                                "description" : "The full name of the author."
                              },
                              "address" : {
                                "description" : "A reusable way to store address details (Street, City, Zip). We can reuse this on Authors, Publishers, or Users.",
                                "properties" : {
                                  "street" : {
                                    "type" : "string",
                                    "description" : "The specific street address. Includes the house number, street name, and apartment number if applicable. Example: \\"123 Maple Avenue, Apt 4B\\"."
                                  },
                                  "city" : {
                                    "type" : "string",
                                    "description" : "The town, city, or municipality. Used for grouping authors by location or calculating shipping regions."
                                  },
                                  "zip" : {
                                    "type" : "string",
                                    "description" : "The postal or zip code. Stored as text (String) rather than a number to support codes that start with zero (e.g., \\"02138\\") or contain letters (e.g., \\"K1A 0B1\\")."
                                  }
                                }
                              }
                            }
                          },
                          "uniqueItems" : true
                        }
                      }
                    }
                  }
                },
                "getBooksByTitle" : {
                  "name" : "get_books_by_title",
                  "description" : "Browse Books (Paginated)",
                  "inputSchema" : {
                    "properties" : {
                      "title" : {
                        "type" : "string",
                        "description" : "The exact book title to filter by."
                      },
                      "page" : {
                        "type" : "integer",
                        "format" : "int32",
                        "description" : "Which page number to load (defaults to 1)."
                      },
                      "size" : {
                        "type" : "integer",
                        "format" : "int32",
                        "description" : "How many books to show per page (defaults to 20)."
                      }
                    },
                    "required" : [ "title" ]
                  },
                  "outputSchema" : {
                    "properties" : {
                      "content" : {
                        "type" : "array",
                        "items" : {
                          "description" : "Represents a physical Book in our library. <p>This is the main item visitors look for. It holds details like the title, the actual text content, and who published it.",
                          "properties" : {
                            "isbn" : {
                              "type" : "string",
                              "description" : "The unique \\"barcode\\" for this book (ISBN). We use this to identify exactly which book edition we are talking about."
                            },
                            "title" : {
                              "type" : "string",
                              "description" : "The name printed on the cover."
                            },
                            "publicationDate" : {
                              "type" : "string",
                              "format" : "date",
                              "description" : "When this book was released to the public."
                            },
                            "text" : {
                              "type" : "string",
                              "description" : "The full story or content of the book. Since this can be very long, we store it in a special way (Large Object) to keep the database fast."
                            },
                            "type" : {
                              "type" : "string",
                              "description" : "Categorizes the item (e.g., is it a regular Book or a Magazine?).\\n  - NOVEL: A fictional narrative story. Examples: \\"Pride and Prejudice\\", \\"Harry Potter\\", \\"Dune\\". These are creative works meant for entertainment or artistic expression.\\n  - BIOGRAPHY: A written account of a real person's life. Examples: \\"Steve Jobs\\" by Walter Isaacson, \\"The Diary of a Young Girl\\". These are non-fiction historical records of an individual.\\n  - TEXTBOOK: An educational book used for study. Examples: \\"Calculus: Early Transcendentals\\", \\"Introduction to Java Programming\\". These are designed for students and are often used as reference material in academic courses.\\n  - MAGAZINE: A periodical publication intended for general readers. Examples: Time, National Geographic, Vogue. These contain various articles, are published frequently (weekly/monthly), and often have a glossy format.\\n  - JOURNAL: A scholarly or professional publication. Examples: The New England Journal of Medicine, Harvard Law Review. These focus on academic research or trade news and are written by experts for other experts.",
                              "enum" : [ "NOVEL", "BIOGRAPHY", "TEXTBOOK", "MAGAZINE", "JOURNAL" ]
                            },
                            "publisher" : {
                              "description" : "A company that produces and sells books.",
                              "properties" : {
                                "id" : {
                                  "type" : "integer",
                                  "format" : "int64",
                                  "description" : "The unique internal ID for this publisher. This is a number generated automatically by the system. Users usually don't need to memorize this, but it's used by the database to link books to their publishers."
                                },
                                "name" : {
                                  "type" : "string",
                                  "description" : "The official business name of the publishing house. Example: \\"Penguin Random House\\" or \\"O'Reilly Media\\"."
                                }
                              }
                            },
                            "authors" : {
                              "type" : "array",
                              "description" : "The list of people who wrote this book.",
                              "items" : {
                                "description" : "A person who writes books.",
                                "properties" : {
                                  "ssn" : {
                                    "type" : "string",
                                    "description" : "The author's unique government ID (SSN)."
                                  },
                                  "name" : {
                                    "type" : "string",
                                    "description" : "The full name of the author."
                                  },
                                  "address" : {
                                    "description" : "A reusable way to store address details (Street, City, Zip). We can reuse this on Authors, Publishers, or Users.",
                                    "properties" : {
                                      "street" : {
                                        "type" : "string",
                                        "description" : "The specific street address. Includes the house number, street name, and apartment number if applicable. Example: \\"123 Maple Avenue, Apt 4B\\"."
                                      },
                                      "city" : {
                                        "type" : "string",
                                        "description" : "The town, city, or municipality. Used for grouping authors by location or calculating shipping regions."
                                      },
                                      "zip" : {
                                        "type" : "string",
                                        "description" : "The postal or zip code. Stored as text (String) rather than a number to support codes that start with zero (e.g., \\"02138\\") or contain letters (e.g., \\"K1A 0B1\\")."
                                      }
                                    }
                                  }
                                }
                              },
                              "uniqueItems" : true
                            }
                          }
                        }
                      },
                      "numberOfElements" : {
                        "type" : "integer",
                        "format" : "int32"
                      },
                      "totalElements" : {
                        "type" : "integer",
                        "format" : "int64"
                      },
                      "totalPages" : {
                        "type" : "integer",
                        "format" : "int64"
                      },
                      "pageRequest" : {
                        "properties" : {
                          "page" : {
                            "type" : "integer",
                            "format" : "int64"
                          },
                          "size" : {
                            "type" : "integer",
                            "format" : "int32"
                          }
                        }
                      },
                      "nextPageRequest" : {
                        "properties" : {
                          "page" : {
                            "type" : "integer",
                            "format" : "int64"
                          },
                          "size" : {
                            "type" : "integer",
                            "format" : "int32"
                          }
                        }
                      },
                      "previousPageRequest" : {
                        "properties" : {
                          "page" : {
                            "type" : "integer",
                            "format" : "int64"
                          },
                          "size" : {
                            "type" : "integer",
                            "format" : "int32"
                          }
                        }
                      }
                    }
                  }
                },
                "addBook" : {
                  "name" : "add_book",
                  "description" : "Add New Book",
                  "inputSchema" : {
                    "description" : "Represents a physical Book in our library. <p>This is the main item visitors look for. It holds details like the title, the actual text content, and who published it.",
                    "properties" : {
                      "isbn" : {
                        "type" : "string",
                        "description" : "The unique \\"barcode\\" for this book (ISBN). We use this to identify exactly which book edition we are talking about."
                      },
                      "title" : {
                        "type" : "string",
                        "description" : "The name printed on the cover."
                      },
                      "publicationDate" : {
                        "type" : "string",
                        "format" : "date",
                        "description" : "When this book was released to the public."
                      },
                      "text" : {
                        "type" : "string",
                        "description" : "The full story or content of the book. Since this can be very long, we store it in a special way (Large Object) to keep the database fast."
                      },
                      "type" : {
                        "type" : "string",
                        "description" : "Categorizes the item (e.g., is it a regular Book or a Magazine?).\\n  - NOVEL: A fictional narrative story. Examples: \\"Pride and Prejudice\\", \\"Harry Potter\\", \\"Dune\\". These are creative works meant for entertainment or artistic expression.\\n  - BIOGRAPHY: A written account of a real person's life. Examples: \\"Steve Jobs\\" by Walter Isaacson, \\"The Diary of a Young Girl\\". These are non-fiction historical records of an individual.\\n  - TEXTBOOK: An educational book used for study. Examples: \\"Calculus: Early Transcendentals\\", \\"Introduction to Java Programming\\". These are designed for students and are often used as reference material in academic courses.\\n  - MAGAZINE: A periodical publication intended for general readers. Examples: Time, National Geographic, Vogue. These contain various articles, are published frequently (weekly/monthly), and often have a glossy format.\\n  - JOURNAL: A scholarly or professional publication. Examples: The New England Journal of Medicine, Harvard Law Review. These focus on academic research or trade news and are written by experts for other experts.",
                        "enum" : [ "NOVEL", "BIOGRAPHY", "TEXTBOOK", "MAGAZINE", "JOURNAL" ]
                      },
                      "publisher" : {
                        "description" : "A company that produces and sells books.",
                        "properties" : {
                          "id" : {
                            "type" : "integer",
                            "format" : "int64",
                            "description" : "The unique internal ID for this publisher. This is a number generated automatically by the system. Users usually don't need to memorize this, but it's used by the database to link books to their publishers."
                          },
                          "name" : {
                            "type" : "string",
                            "description" : "The official business name of the publishing house. Example: \\"Penguin Random House\\" or \\"O'Reilly Media\\"."
                          }
                        }
                      },
                      "authors" : {
                        "type" : "array",
                        "description" : "The list of people who wrote this book.",
                        "items" : {
                          "description" : "A person who writes books.",
                          "properties" : {
                            "ssn" : {
                              "type" : "string",
                              "description" : "The author's unique government ID (SSN)."
                            },
                            "name" : {
                              "type" : "string",
                              "description" : "The full name of the author."
                            },
                            "address" : {
                              "description" : "A reusable way to store address details (Street, City, Zip). We can reuse this on Authors, Publishers, or Users.",
                              "properties" : {
                                "street" : {
                                  "type" : "string",
                                  "description" : "The specific street address. Includes the house number, street name, and apartment number if applicable. Example: \\"123 Maple Avenue, Apt 4B\\"."
                                },
                                "city" : {
                                  "type" : "string",
                                  "description" : "The town, city, or municipality. Used for grouping authors by location or calculating shipping regions."
                                },
                                "zip" : {
                                  "type" : "string",
                                  "description" : "The postal or zip code. Stored as text (String) rather than a number to support codes that start with zero (e.g., \\"02138\\") or contain letters (e.g., \\"K1A 0B1\\")."
                                }
                              }
                            }
                          }
                        },
                        "uniqueItems" : true
                      }
                    }
                  },
                  "outputSchema" : {
                    "description" : "Represents a physical Book in our library. <p>This is the main item visitors look for. It holds details like the title, the actual text content, and who published it.",
                    "properties" : {
                      "isbn" : {
                        "type" : "string",
                        "description" : "The unique \\"barcode\\" for this book (ISBN). We use this to identify exactly which book edition we are talking about."
                      },
                      "title" : {
                        "type" : "string",
                        "description" : "The name printed on the cover."
                      },
                      "publicationDate" : {
                        "type" : "string",
                        "format" : "date",
                        "description" : "When this book was released to the public."
                      },
                      "text" : {
                        "type" : "string",
                        "description" : "The full story or content of the book. Since this can be very long, we store it in a special way (Large Object) to keep the database fast."
                      },
                      "type" : {
                        "type" : "string",
                        "description" : "Categorizes the item (e.g., is it a regular Book or a Magazine?).\\n  - NOVEL: A fictional narrative story. Examples: \\"Pride and Prejudice\\", \\"Harry Potter\\", \\"Dune\\". These are creative works meant for entertainment or artistic expression.\\n  - BIOGRAPHY: A written account of a real person's life. Examples: \\"Steve Jobs\\" by Walter Isaacson, \\"The Diary of a Young Girl\\". These are non-fiction historical records of an individual.\\n  - TEXTBOOK: An educational book used for study. Examples: \\"Calculus: Early Transcendentals\\", \\"Introduction to Java Programming\\". These are designed for students and are often used as reference material in academic courses.\\n  - MAGAZINE: A periodical publication intended for general readers. Examples: Time, National Geographic, Vogue. These contain various articles, are published frequently (weekly/monthly), and often have a glossy format.\\n  - JOURNAL: A scholarly or professional publication. Examples: The New England Journal of Medicine, Harvard Law Review. These focus on academic research or trade news and are written by experts for other experts.",
                        "enum" : [ "NOVEL", "BIOGRAPHY", "TEXTBOOK", "MAGAZINE", "JOURNAL" ]
                      },
                      "publisher" : {
                        "description" : "A company that produces and sells books.",
                        "properties" : {
                          "id" : {
                            "type" : "integer",
                            "format" : "int64",
                            "description" : "The unique internal ID for this publisher. This is a number generated automatically by the system. Users usually don't need to memorize this, but it's used by the database to link books to their publishers."
                          },
                          "name" : {
                            "type" : "string",
                            "description" : "The official business name of the publishing house. Example: \\"Penguin Random House\\" or \\"O'Reilly Media\\"."
                          }
                        }
                      },
                      "authors" : {
                        "type" : "array",
                        "description" : "The list of people who wrote this book.",
                        "items" : {
                          "description" : "A person who writes books.",
                          "properties" : {
                            "ssn" : {
                              "type" : "string",
                              "description" : "The author's unique government ID (SSN)."
                            },
                            "name" : {
                              "type" : "string",
                              "description" : "The full name of the author."
                            },
                            "address" : {
                              "description" : "A reusable way to store address details (Street, City, Zip). We can reuse this on Authors, Publishers, or Users.",
                              "properties" : {
                                "street" : {
                                  "type" : "string",
                                  "description" : "The specific street address. Includes the house number, street name, and apartment number if applicable. Example: \\"123 Maple Avenue, Apt 4B\\"."
                                },
                                "city" : {
                                  "type" : "string",
                                  "description" : "The town, city, or municipality. Used for grouping authors by location or calculating shipping regions."
                                },
                                "zip" : {
                                  "type" : "string",
                                  "description" : "The postal or zip code. Stored as text (String) rather than a number to support codes that start with zero (e.g., \\"02138\\") or contain letters (e.g., \\"K1A 0B1\\")."
                                }
                              }
                            }
                          }
                        },
                        "uniqueItems" : true
                      }
                    }
                  }
                },
                "addAuthor" : {
                  "name" : "add_author",
                  "description" : "Add New Author",
                  "inputSchema" : {
                    "description" : "A person who writes books.",
                    "properties" : {
                      "ssn" : {
                        "type" : "string",
                        "description" : "The author's unique government ID (SSN)."
                      },
                      "name" : {
                        "type" : "string",
                        "description" : "The full name of the author."
                      },
                      "address" : {
                        "description" : "A reusable way to store address details (Street, City, Zip). We can reuse this on Authors, Publishers, or Users.",
                        "properties" : {
                          "street" : {
                            "type" : "string",
                            "description" : "The specific street address. Includes the house number, street name, and apartment number if applicable. Example: \\"123 Maple Avenue, Apt 4B\\"."
                          },
                          "city" : {
                            "type" : "string",
                            "description" : "The town, city, or municipality. Used for grouping authors by location or calculating shipping regions."
                          },
                          "zip" : {
                            "type" : "string",
                            "description" : "The postal or zip code. Stored as text (String) rather than a number to support codes that start with zero (e.g., \\"02138\\") or contain letters (e.g., \\"K1A 0B1\\")."
                          }
                        }
                      }
                    }
                  },
                  "outputSchema" : {
                    "description" : "A person who writes books.",
                    "properties" : {
                      "ssn" : {
                        "type" : "string",
                        "description" : "The author's unique government ID (SSN)."
                      },
                      "name" : {
                        "type" : "string",
                        "description" : "The full name of the author."
                      },
                      "address" : {
                        "description" : "A reusable way to store address details (Street, City, Zip). We can reuse this on Authors, Publishers, or Users.",
                        "properties" : {
                          "street" : {
                            "type" : "string",
                            "description" : "The specific street address. Includes the house number, street name, and apartment number if applicable. Example: \\"123 Maple Avenue, Apt 4B\\"."
                          },
                          "city" : {
                            "type" : "string",
                            "description" : "The town, city, or municipality. Used for grouping authors by location or calculating shipping regions."
                          },
                          "zip" : {
                            "type" : "string",
                            "description" : "The postal or zip code. Stored as text (String) rather than a number to support codes that start with zero (e.g., \\"02138\\") or contain letters (e.g., \\"K1A 0B1\\")."
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            """);
  }
}
