package com.fittracker.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fittracker.common.error.BusinessRuleException;
import com.fittracker.common.error.ForbiddenException;
import com.fittracker.common.error.NotFoundException;
import com.fittracker.training.dto.ProgramCreateRequest;
import com.fittracker.training.dto.ProgramUpdateRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests unitaires (Mockito + AssertJ) du {@link ProgramService}. */
@ExtendWith(MockitoExtension.class)
class ProgramServiceTest {

  @Mock private ProgramRepository repository;

  private ProgramService programService;

  private final UUID userId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private final LocalDate start = LocalDate.of(2026, 6, 1);
  private final LocalDate end = LocalDate.of(2026, 9, 1);

  @BeforeEach
  void setUp() {
    programService = new ProgramService(repository);
  }

  private Program ownedProgram() {
    return new Program(programId, userId, "Prep", "desc", "10km", start, end, null);
  }

  @Test
  void should_create_program_when_dates_valid() {
    when(repository.save(any(Program.class))).thenAnswer(inv -> inv.getArgument(0));

    Program created =
        programService.create(
            userId, new ProgramCreateRequest("Prep", "desc", "10km", start, end));

    assertThat(created.getName()).isEqualTo("Prep");
    assertThat(created.getUserId()).isEqualTo(userId);
    assertThat(created.getEndDate()).isEqualTo(end);
  }

  @Test
  void should_throw_business_rule_when_end_before_start_on_create() {
    assertThatThrownBy(
            () ->
                programService.create(
                    userId, new ProgramCreateRequest("Prep", "desc", "10km", end, start)))
        .isInstanceOf(BusinessRuleException.class);
    verify(repository, never()).save(any());
  }

  @Test
  void should_return_program_when_owned() {
    Program program = ownedProgram();
    when(repository.findById(programId)).thenReturn(Optional.of(program));

    assertThat(programService.getOwned(programId, userId)).isSameAs(program);
  }

  @Test
  void should_throw_not_found_when_program_absent() {
    when(repository.findById(programId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> programService.getOwned(programId, userId))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void should_throw_forbidden_when_program_not_owned() {
    when(repository.findById(programId)).thenReturn(Optional.of(ownedProgram()));

    assertThatThrownBy(() -> programService.getOwned(programId, UUID.randomUUID()))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void should_update_all_fields_when_provided() {
    when(repository.findById(programId)).thenReturn(Optional.of(ownedProgram()));
    when(repository.save(any(Program.class))).thenAnswer(inv -> inv.getArgument(0));

    LocalDate newStart = LocalDate.of(2026, 7, 1);
    LocalDate newEnd = LocalDate.of(2026, 10, 1);
    Program updated =
        programService.update(
            programId,
            userId,
            new ProgramUpdateRequest("Marathon", "12 weeks", "42km", newStart, newEnd));

    assertThat(updated.getName()).isEqualTo("Marathon");
    assertThat(updated.getDescription()).isEqualTo("12 weeks");
    assertThat(updated.getTargetMetric()).isEqualTo("42km");
    assertThat(updated.getStartDate()).isEqualTo(newStart);
    assertThat(updated.getEndDate()).isEqualTo(newEnd);
  }

  @Test
  void should_throw_business_rule_when_update_makes_end_before_start() {
    when(repository.findById(programId)).thenReturn(Optional.of(ownedProgram()));

    LocalDate beforeStart = LocalDate.of(2026, 5, 1);
    assertThatThrownBy(
            () ->
                programService.update(
                    programId,
                    userId,
                    new ProgramUpdateRequest(null, null, null, null, beforeStart)))
        .isInstanceOf(BusinessRuleException.class);
    verify(repository, never()).save(any());
  }

  @Test
  void should_keep_fields_when_update_is_null() {
    when(repository.findById(programId)).thenReturn(Optional.of(ownedProgram()));
    when(repository.save(any(Program.class))).thenAnswer(inv -> inv.getArgument(0));

    Program updated =
        programService.update(
            programId, userId, new ProgramUpdateRequest(null, null, null, null, null));

    assertThat(updated.getName()).isEqualTo("Prep");
    assertThat(updated.getStartDate()).isEqualTo(start);
  }

  @Test
  void should_delete_owned_program() {
    when(repository.findById(programId)).thenReturn(Optional.of(ownedProgram()));

    programService.delete(programId, userId);

    verify(repository).deleteById(programId);
  }

  @Test
  void should_list_programs_for_user() {
    List<Program> programs = List.of(ownedProgram());
    when(repository.findByUserId(userId)).thenReturn(programs);

    assertThat(programService.listForUser(userId)).isEqualTo(programs);
  }
}
