-- Run once on existing v2 databases before deploying the performance change.
CREATE INDEX idx_metric_content_latest
  ON fact_content_metric_snapshot (content_id, captured_at, snapshot_id);
