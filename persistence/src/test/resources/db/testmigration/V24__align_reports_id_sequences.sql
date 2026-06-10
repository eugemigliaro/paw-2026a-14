-- Mirror of prod V24. Creates the named sequences Hibernate expects for
-- user_bans and moderation_reports (see V23 for why the default repoint /
-- setval are unnecessary under HSQLDB).
CREATE SEQUENCE user_bans_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE moderation_reports_id_seq START WITH 1 INCREMENT BY 1;
