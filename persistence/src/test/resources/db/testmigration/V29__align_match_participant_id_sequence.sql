-- Mirror of prod V29. Creates the named sequence Hibernate expects for
-- match_participants (see V23 for why the default repoint / setval are
-- unnecessary under HSQLDB).
CREATE SEQUENCE match_participants_id_seq START WITH 1 INCREMENT BY 1;
