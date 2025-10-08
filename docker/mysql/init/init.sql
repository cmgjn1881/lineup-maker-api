-- init.sql 파일
-- lineup_db 데이터베이스를 사용합니다.
USE lineup_db;

-- User 테이블이 존재하지 않을 때만 생성합니다.
CREATE TABLE IF NOT EXISTS users (
                                    user_id BINARY(16) PRIMARY KEY NOT NULL COMMENT '사용자 ID (UUID)',
                                    email VARCHAR(255) UNIQUE NOT NULL COMMENT '로그인 ID 및 이메일',
                                    password VARCHAR(60) NOT NULL COMMENT '암호화된 비밀번호 (Bcrypt 기준 60자)',
                                    username VARCHAR(50) NOT NULL COMMENT '사용자 이름/닉네임',
                                    is_verified BOOLEAN NOT NULL DEFAULT FALSE COMMENT '이메일 인증 여부',
                                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가입 일시'
);

-- 초기 데이터 삽입 (선택 사항)
-- INSERT IGNORE INTO User ...;