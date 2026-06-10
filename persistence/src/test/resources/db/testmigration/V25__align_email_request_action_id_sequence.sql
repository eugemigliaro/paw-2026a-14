-- Mirror of prod V25. Creates the named sequence Hibernate expects for
-- email_action_requests (see V23 for why the default repoint / setval are
-- unnecessary under HSQLDB).
CREATE SEQUENCE email_action_requests_id_seq START WITH 1 INCREMENT BY 1;
