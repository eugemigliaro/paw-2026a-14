-- Mirror of prod V28. Creates the named sequences Hibernate expects for matches
-- and match_series (see V23 for why the default repoint / setval are
-- unnecessary under HSQLDB).
CREATE SEQUENCE matches_matchid_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE match_series_id_seq START WITH 1 INCREMENT BY 1;
