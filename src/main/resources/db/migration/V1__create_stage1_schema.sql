-- Enum types
CREATE TYPE template_status AS ENUM ('DRAFT', 'ACTIVE', 'DISABLED');
CREATE TYPE job_status AS ENUM ('PROCESSING', 'READY', 'FAILED', 'EXPIRED');
CREATE TYPE audit_action AS ENUM ('GENERATE', 'DOWNLOAD', 'EXPIRE');
CREATE TYPE audit_outcome AS ENUM ('SUCCESS', 'FAILURE');

-- templates
CREATE TABLE templates (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    template_id     VARCHAR(64)     NOT NULL,
    name            VARCHAR(150)    NOT NULL,
    version         INTEGER         NOT NULL,
    status          template_status NOT NULL DEFAULT 'DRAFT',
    template_html   TEXT,
    contract_json   TEXT,
    mapping_json    TEXT,
    created_by      VARCHAR(100),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    activated_at    TIMESTAMPTZ,
    disabled_at     TIMESTAMPTZ,

    CONSTRAINT pk_templates PRIMARY KEY (id),
    CONSTRAINT uq_templates_template_id_version UNIQUE (template_id, version)
);

-- At most one ACTIVE version per template_id
CREATE UNIQUE INDEX uq_templates_one_active ON templates (template_id) WHERE status = 'ACTIVE';

-- Versions are write-once: only status and its timestamps may change
CREATE FUNCTION fn_templates_write_once() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.template_id   IS DISTINCT FROM OLD.template_id
    OR NEW.version       IS DISTINCT FROM OLD.version
    OR NEW.template_html IS DISTINCT FROM OLD.template_html
    OR NEW.contract_json IS DISTINCT FROM OLD.contract_json
    OR NEW.mapping_json  IS DISTINCT FROM OLD.mapping_json THEN
        RAISE EXCEPTION 'templates row % is write-once', OLD.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_templates_write_once
    BEFORE UPDATE ON templates
    FOR EACH ROW EXECUTE FUNCTION fn_templates_write_once();

-- document_jobs
CREATE TABLE document_jobs (
    id                  UUID         NOT NULL,
    template_version_id UUID         NOT NULL,
    channel             VARCHAR(20),
    request_id          VARCHAR(100),
    status              job_status   NOT NULL,
    error_code          VARCHAR(50),
    storage_path        VARCHAR(512),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ,
    expires_at          TIMESTAMPTZ,

    CONSTRAINT pk_document_jobs PRIMARY KEY (id),
    CONSTRAINT fk_document_jobs_template_version_id
        FOREIGN KEY (template_version_id) REFERENCES templates (id) ON DELETE RESTRICT
);

CREATE INDEX idx_document_jobs_template_version_id ON document_jobs (template_version_id);

-- audit_logs (append-only)
CREATE TABLE audit_logs (
    id              UUID          NOT NULL DEFAULT gen_random_uuid(),
    document_job_id UUID          NOT NULL,
    action          audit_action  NOT NULL,
    outcome         audit_outcome NOT NULL,
    client_id       VARCHAR(100),
    channel         VARCHAR(20),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_audit_logs PRIMARY KEY (id),
    CONSTRAINT fk_audit_logs_document_job_id
        FOREIGN KEY (document_job_id) REFERENCES document_jobs (id) ON DELETE RESTRICT
);

CREATE INDEX idx_audit_logs_document_job_id ON audit_logs (document_job_id);
