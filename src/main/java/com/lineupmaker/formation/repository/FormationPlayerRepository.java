package com.lineupmaker.formation.repository;

import com.lineupmaker.formation.entity.FormationPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FormationPlayerRepository extends JpaRepository<FormationPlayer, Long> {

    // 특정 Formation ID에 속한 모든 배치 정보를 조회하는 메서드
    List<FormationPlayer> findByFormationFormationId(Long formationId);

    /**
     * [Fetch Join 적용]
     * 특정 포메이션에 속한 모든 선수 배치 정보를 가져올 때, 연관된 Player 엔티티를 함께 조회합니다.
     * 이를 통해 N+1 쿼리 문제를 해결하고, LazyInitializationException을 방지합니다.
     * @param formationId 조회할 포메이션의 ID
     * @return Player 정보가 포함된 FormationPlayer 목록
     */
    @Query("SELECT fp FROM FormationPlayer fp JOIN FETCH fp.player WHERE fp.formation.formationId = :formationId")
    List<FormationPlayer> findAllWithPlayerByFormationId(@Param("formationId") Long formationId);
}
