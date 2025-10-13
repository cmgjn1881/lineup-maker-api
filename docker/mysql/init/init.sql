-- docker/mysql/init/init.sql 파일

-- MySQL 8.0 컨테이너는 자동으로 DB를 생성하지만, 안전을 위해 사용 DB를 지정합니다.
USE lineup_db;

-- 1. 사용자 (users) 테이블
CREATE TABLE IF NOT EXISTS users (
                                     user_id BINARY(16) PRIMARY KEY NOT NULL COMMENT '사용자 ID (UUID)',
                                     email VARCHAR(255) UNIQUE NOT NULL COMMENT '로그인 ID 및 이메일',
                                     password VARCHAR(60) NOT NULL COMMENT '암호화된 비밀번호 (Bcrypt 기준 60자)',
                                     username VARCHAR(50) NOT NULL COMMENT '사용자 이름/닉네임',
                                     is_verified BOOLEAN NOT NULL DEFAULT FALSE COMMENT '이메일 인증 여부',
                                     created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가입 일시'
);

-- 2. 리프레시 토큰 (refresh_token) 테이블 (users 참조)
-- redis 사용으로 refresh_token 테이블 제거
# CREATE TABLE IF NOT EXISTS refresh_token (
#                                              token_id BIGINT PRIMARY KEY AUTO_INCREMENT,
#                                              user_id BINARY(16) NOT NULL COMMENT '사용자 ID (FK)',
#                                              token_value VARCHAR(512) UNIQUE NOT NULL COMMENT '갱신 토큰 값',
#                                              expires_at DATETIME NOT NULL COMMENT '토큰 만료 일시',
#
#                                              FOREIGN KEY (user_id) REFERENCES users(user_id)
#                                                  ON DELETE CASCADE
# );

-- 3. 팀 (team) 테이블 (users 참조)
CREATE TABLE IF NOT EXISTS team (
                                    team_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                    owner_id BINARY(16) NOT NULL COMMENT '팀 소유자 ID (FK)',
                                    name VARCHAR(100) NOT NULL COMMENT '팀 이름',
                                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                                    FOREIGN KEY (owner_id) REFERENCES users(user_id)
                                        ON DELETE CASCADE
);

-- 4. 선수 (player) 테이블 (team 참조)
CREATE TABLE IF NOT EXISTS player (
                                      player_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                      team_id BIGINT NOT NULL COMMENT '소속 팀 ID (FK)',
                                      name VARCHAR(50) NOT NULL COMMENT '선수 이름',
                                      back_number TINYINT UNSIGNED NOT NULL COMMENT '등번호 (1~99)',
                                      position VARCHAR(20) NOT NULL COMMENT '주 포지션 (FW, MF, DF, GK 등)',

                                      FOREIGN KEY (team_id) REFERENCES team(team_id)
                                          ON DELETE CASCADE,

                                      UNIQUE KEY uk_team_back_number (team_id, back_number),
                                      CHECK (back_number BETWEEN 1 AND 99)
);

-- 5. 포메이션 (formation) 테이블 (users, team 참조)
CREATE TABLE IF NOT EXISTS formation (
                                         formation_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                         user_id BINARY(16) NOT NULL COMMENT '포메이션을 생성한 사용자 ID (FK)',
                                         team_id BIGINT NOT NULL COMMENT '적용할 팀 ID (FK)',
                                         name VARCHAR(100) NOT NULL COMMENT '포메이션 이름 (예: 4-4-2)',

                                         FOREIGN KEY (user_id) REFERENCES users(user_id)
                                             ON DELETE CASCADE,
                                         FOREIGN KEY (team_id) REFERENCES team(team_id)
                                             ON DELETE CASCADE
);

-- 6. 포메이션 선수 배치 (formation_player) 테이블 (formation, player 참조)
CREATE TABLE IF NOT EXISTS formation_player (
                                                fp_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                formation_id BIGINT NOT NULL COMMENT '포메이션 ID (FK)',
                                                player_id BIGINT NOT NULL COMMENT '배치된 선수 ID (FK)',
                                                quarter TINYINT UNSIGNED NOT NULL COMMENT '적용 쿼터 (1~4)',
                                                coord_x SMALLINT UNSIGNED NOT NULL COMMENT '필드 내 상대적 X 좌표 (0~1000)',
                                                coord_y SMALLINT UNSIGNED NOT NULL COMMENT '필드 내 상대적 Y 좌표 (0~1000)',

                                                FOREIGN KEY (formation_id) REFERENCES formation(formation_id)
                                                    ON DELETE CASCADE,
                                                FOREIGN KEY (player_id) REFERENCES player(player_id)
                                                    ON DELETE CASCADE,

                                                UNIQUE KEY uk_formation_player_quarter (formation_id, player_id, quarter)
);