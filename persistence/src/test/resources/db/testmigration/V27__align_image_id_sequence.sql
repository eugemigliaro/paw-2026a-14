-- Mirror of prod V27. Creates the named sequence Hibernate expects for images
-- (see V23 for why the default repoint / setval are unnecessary under HSQLDB).
CREATE SEQUENCE images_imageid_seq START WITH 1 INCREMENT BY 1;
