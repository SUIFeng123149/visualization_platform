-- Run once on existing v2 databases before deploying the analytics performance change.
-- These indexes support the most common content filters and interaction trend queries.
CREATE INDEX idx_content_platform_published
  ON dim_content (platform_code, published_at, content_id);

CREATE INDEX idx_interaction_content_type_occurred
  ON fact_interaction (content_id, interaction_type, occurred_at, captured_at);
