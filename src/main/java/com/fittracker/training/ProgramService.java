package com.fittracker.training;

import com.fittracker.common.error.BusinessRuleException;
import com.fittracker.common.error.ForbiddenException;
import com.fittracker.common.error.NotFoundException;
import com.fittracker.training.dto.ProgramCreateRequest;
import com.fittracker.training.dto.ProgramUpdateRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgramService {

  private final ProgramRepository repository;

  public ProgramService(ProgramRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public Program create(UUID userId, ProgramCreateRequest req) {
    if (req.endDate().isBefore(req.startDate())) {
      throw new BusinessRuleException("endDate doit etre apres startDate");
    }
    Program program =
        new Program(
            UUID.randomUUID(),
            userId,
            req.name(),
            req.description(),
            req.targetMetric(),
            req.startDate(),
            req.endDate(),
            OffsetDateTime.now());
    return repository.save(program);
  }

  @Transactional(readOnly = true)
  public Program getOwned(UUID id, UUID userId) {
    Program p = repository.findById(id).orElseThrow(() -> new NotFoundException("Program", id));
    if (!p.getUserId().equals(userId)) {
      throw new ForbiddenException("Ce programme ne vous appartient pas");
    }
    return p;
  }

  @Transactional
  public Program update(UUID id, UUID userId, ProgramUpdateRequest req) {
    Program p = getOwned(id, userId);
    if (req.name() != null) {
      p.setName(req.name());
    }
    if (req.description() != null) {
      p.setDescription(req.description());
    }
    if (req.targetMetric() != null) {
      p.setTargetMetric(req.targetMetric());
    }
    if (req.startDate() != null) {
      p.setStartDate(req.startDate());
    }
    if (req.endDate() != null) {
      p.setEndDate(req.endDate());
    }
    if (p.getEndDate().isBefore(p.getStartDate())) {
      throw new BusinessRuleException("endDate doit etre apres startDate");
    }
    return repository.save(p);
  }

  @Transactional
  public void delete(UUID id, UUID userId) {
    getOwned(id, userId);
    repository.deleteById(id);
  }

  @Transactional(readOnly = true)
  public List<Program> listForUser(UUID userId) {
    return repository.findByUserId(userId);
  }
}
