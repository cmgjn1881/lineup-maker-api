package com.lineupmaker.formation.repository;

import com.lineupmaker.formation.entity.Formation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormationRepository extends JpaRepository<Formation, Long> {
}
