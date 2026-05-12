CREATE SEQUENCE account_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE ledger_seq  START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE revinfo_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE account (
    id                BIGINT         NOT NULL,
    guid              UUID           NOT NULL,
    name              VARCHAR(255)   NOT NULL,
    maximal_overdraft NUMERIC(19, 4) NOT NULL,
    current_balance   NUMERIC(19, 4) NOT NULL,
    version           BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT pk_account PRIMARY KEY (id),
    CONSTRAINT uk_account_guid UNIQUE (guid)
);

CREATE TABLE ledger (
    id              BIGINT         NOT NULL,
    from_account_id BIGINT,
    to_account_id   BIGINT,
    amount          NUMERIC(19, 4) NOT NULL,
    balance_after   NUMERIC(19, 4) NOT NULL,
    time            TIMESTAMP,
    description     VARCHAR(255),
    version         BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT pk_ledger PRIMARY KEY (id),
    CONSTRAINT fk_ledger_from_account FOREIGN KEY (from_account_id) REFERENCES account (id),
    CONSTRAINT fk_ledger_to_account   FOREIGN KEY (to_account_id)   REFERENCES account (id)
);

CREATE INDEX idx_ledger_from_account ON ledger (from_account_id);
CREATE INDEX idx_ledger_to_account   ON ledger (to_account_id);

-- Hibernate Envers audit infrastructure
CREATE TABLE revinfo (
    rev      INTEGER NOT NULL,
    revtstmp BIGINT,
    CONSTRAINT pk_revinfo PRIMARY KEY (rev)
);

CREATE TABLE account_aud (
    id                BIGINT   NOT NULL,
    rev               INTEGER  NOT NULL,
    revtype           SMALLINT,
    guid              UUID,
    name              VARCHAR(255),
    maximal_overdraft NUMERIC(19, 4),
    current_balance   NUMERIC(19, 4),
    version           BIGINT,
    CONSTRAINT pk_account_aud PRIMARY KEY (id, rev),
    CONSTRAINT fk_account_aud_revinfo FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE ledger_aud (
    id              BIGINT   NOT NULL,
    rev             INTEGER  NOT NULL,
    revtype         SMALLINT,
    from_account_id BIGINT,
    to_account_id   BIGINT,
    amount          NUMERIC(19, 4),
    balance_after   NUMERIC(19, 4),
    time            TIMESTAMP,
    description     VARCHAR(255),
    version         BIGINT,
    CONSTRAINT pk_ledger_aud PRIMARY KEY (id, rev),
    CONSTRAINT fk_ledger_aud_revinfo FOREIGN KEY (rev) REFERENCES revinfo (rev)
);
