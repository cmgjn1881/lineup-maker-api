package com.lineupmaker.formation.repository;

import com.lineupmaker.formation.entity.Formation;
import com.lineupmaker.formation.entity.FormationQuarterReferee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormationQuarterRefereeRepository extends JpaRepository<FormationQuarterReferee, Long> {
    void deleteByFormation(Formation formation);
}
