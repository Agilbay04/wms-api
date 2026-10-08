CREATE TABLE export_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requested_by_user_id UUID NOT NULL REFERENCES users(id),
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    report_type VARCHAR(30) NOT NULL CHECK (report_type IN ('STOCKS', 'MOVEMENTS')),
    filters JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(20) NOT NULL CHECK (status IN ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED')),
    output_path TEXT,
    error_message TEXT,
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    email_sent_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_export_jobs_requested_by_created_at ON export_jobs(requested_by_user_id, created_at DESC);
CREATE INDEX idx_export_jobs_status_created_at ON export_jobs(status, created_at);
