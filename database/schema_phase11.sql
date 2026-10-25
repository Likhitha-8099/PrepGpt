-- PrepGPT Phase 11: Job Description Table & Session Snapshot Reference Script
-- Active development strategy uses spring.jpa.hibernate.ddl-auto=update

CREATE TABLE IF NOT EXISTS job_descriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_role VARCHAR(255) NOT NULL,
    company_name VARCHAR(255),
    raw_text TEXT NOT NULL,
    extracted_skills_json TEXT,
    experience_level VARCHAR(50),
    key_responsibilities_json TEXT,
    ai_analysis_summary TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_job_description_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_job_description_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
