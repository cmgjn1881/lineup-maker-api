-- docker/postgres/init/init.sql 파일 (refresh_token 테이블 제거 버전)

-- 1. 사용자 (users) 테이블
CREATE TABLE IF NOT EXISTS users (
                                     user_id UUID PRIMARY KEY,
                                     email VARCHAR(255) UNIQUE NOT NULL,
                                     password VARCHAR(60) NOT NULL,
                                     username VARCHAR(50) NOT NULL,
                                     is_verified BOOLEAN NOT NULL DEFAULT FALSE,
                                     created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- 2. 팀 (team) 테이블 (users 참조)
CREATE TABLE IF NOT EXISTS team (
                                    team_id BIGSERIAL PRIMARY KEY,
                                    owner_id UUID NOT NULL,
                                    name VARCHAR(100) NOT NULL,
                                    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),

                                    FOREIGN KEY (owner_id) REFERENCES users(user_id)
                                        ON DELETE CASCADE
);

-- 3. 선수 (player) 테이블 (team 참조)
CREATE TABLE IF NOT EXISTS player (
                                      player_id BIGSERIAL PRIMARY KEY,
                                      team_id BIGINT NOT NULL,
                                      name VARCHAR(50) NOT NULL,
                                      back_number SMALLINT NOT NULL,
                                      position VARCHAR(20) NOT NULL,

                                      FOREIGN KEY (team_id) REFERENCES team(team_id)
                                          ON DELETE CASCADE,

                                      UNIQUE (team_id, back_number),
                                      CHECK (back_number >= 1 AND back_number <= 99)
);

-- 4. 포메이션 (formation) 테이블 (users, team 참조)
CREATE TABLE IF NOT EXISTS formation (
                                         formation_id BIGSERIAL PRIMARY KEY,
                                         user_id UUID NOT NULL,
                                         team_id BIGINT NOT NULL,
                                         name VARCHAR(100) NOT NULL,

                                         FOREIGN KEY (user_id) REFERENCES users(user_id)
                                             ON DELETE CASCADE,
                                         FOREIGN KEY (team_id) REFERENCES team(team_id)
                                             ON DELETE CASCADE
);

-- 5. 포메이션 선수 배치 (formation_player) 테이블 (formation, player 참조)
CREATE TABLE IF NOT EXISTS formation_player (
                                                fp_id BIGSERIAL PRIMARY KEY,
                                                formation_id BIGINT NOT NULL,
                                                player_id BIGINT NOT NULL,
                                                quarter SMALLINT NOT NULL,
                                                coord_x SMALLINT NOT NULL,
                                                coord_y SMALLINT NOT NULL,

                                                FOREIGN KEY (formation_id) REFERENCES formation(formation_id)
                                                    ON DELETE CASCADE,
                                                FOREIGN KEY (player_id) REFERENCES player(player_id)
                                                    ON DELETE CASCADE,

                                                UNIQUE (formation_id, player_id, quarter)
);