-- Mirror of prod V31. Creates the named sequence Hibernate expects for
-- player_reviews (see V23 for why the default repoint / setval are unnecessary
-- under HSQLDB).
CREATE SEQUENCE player_reviews_id_seq START WITH 1 INCREMENT BY 1;
