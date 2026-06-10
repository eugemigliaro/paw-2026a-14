-- Mirror of prod V23. Creates the named sequence Hibernate's @SequenceGenerator
-- expects. Prod also repoints users.id DEFAULT to this sequence and runs
-- setval(); under HSQLDB the BIGSERIAL column already supplies a default and
-- Hibernate assigns ids from the sequence explicitly, so only the CREATE
-- SEQUENCE is needed for parity (the test DB has no pre-existing rows).
CREATE SEQUENCE users_userid_seq START WITH 1 INCREMENT BY 1;
