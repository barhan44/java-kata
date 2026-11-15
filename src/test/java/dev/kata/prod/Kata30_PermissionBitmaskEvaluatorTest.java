package dev.kata.prod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kata.prod.Kata30_PermissionBitmaskEvaluator.Permission;
import dev.kata.prod.Kata30_PermissionBitmaskEvaluator.ResourceContext;
import dev.kata.prod.Kata30_PermissionBitmaskEvaluator.Role;
import dev.kata.prod.Kata30_PermissionBitmaskEvaluator.UserContext;
import dev.kata.solutions.prod.Kata30_PermissionBitmaskEvaluatorSolution;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты Kata 30.
 *
 * <p>По умолчанию проверяют заготовку. Запуск против эталона:
 *
 * <pre>mvn test -Dtest=Kata30* -DuseSolution=true</pre>
 */
@DisplayName("Kata 30: RBAC + ABAC на EnumSet")
class Kata30_PermissionBitmaskEvaluatorTest {

  private static final boolean USE_SOLUTION = Boolean.getBoolean("useSolution");

  private Kata30_PermissionBitmaskEvaluator newEvaluator() {
    return USE_SOLUTION
        ? new Kata30_PermissionBitmaskEvaluatorSolution()
        : new Kata30_PermissionBitmaskEvaluator();
  }

  @Test
  @DisplayName("Трёхуровневое наследование ролей объединяет права")
  void threeLevelInheritance() {
    Kata30_PermissionBitmaskEvaluator evaluator = newEvaluator();
    evaluator.registerRole(Role.VIEWER, EnumSet.of(Permission.READ), noPermissions(), noRoles());
    evaluator.registerRole(
        Role.EDITOR, EnumSet.of(Permission.WRITE), noPermissions(), EnumSet.of(Role.VIEWER));
    evaluator.registerRole(
        Role.ADMIN, EnumSet.of(Permission.DELETE), noPermissions(), EnumSet.of(Role.EDITOR));

    UserContext admin = user("alice", Role.ADMIN);
    UserContext editor = user("bob", Role.EDITOR);
    UserContext viewer = user("carol", Role.VIEWER);

    assertThat(evaluator.effectivePermissions(admin))
        .containsExactlyInAnyOrder(Permission.READ, Permission.WRITE, Permission.DELETE);
    assertThat(evaluator.effectivePermissions(editor))
        .containsExactlyInAnyOrder(Permission.READ, Permission.WRITE);
    assertThat(evaluator.effectivePermissions(viewer)).containsExactly(Permission.READ);
    assertThat(evaluator.effectivePermissions(user("nobody"))).isEmpty();

    ResourceContext resource = resource("owner", Map.of());
    assertThat(evaluator.hasPermission(admin, resource, Permission.DELETE)).isTrue();
    assertThat(evaluator.hasPermission(viewer, resource, Permission.WRITE)).isFalse();
    assertThat(evaluator.hasPermission(user("nobody"), resource, Permission.READ)).isFalse();
  }

  @Test
  @DisplayName("DENY предка сильнее GRANT потомка")
  void denyFromAncestorWins() {
    Kata30_PermissionBitmaskEvaluator evaluator = newEvaluator();
    evaluator.registerRole(
        Role.VIEWER,
        EnumSet.of(Permission.READ, Permission.WRITE),
        EnumSet.of(Permission.DELETE),
        noRoles());
    evaluator.registerRole(
        Role.EDITOR, EnumSet.of(Permission.DELETE), noPermissions(), EnumSet.of(Role.VIEWER));

    UserContext editor = user("bob", Role.EDITOR);
    assertThat(evaluator.effectivePermissions(editor))
        .containsExactlyInAnyOrder(Permission.READ, Permission.WRITE);
    assertThat(evaluator.hasPermission(editor, resource("owner", Map.of()), Permission.DELETE))
        .isFalse();
  }

  @Test
  @DisplayName("DENY одной роли сильнее GRANT другой роли пользователя")
  void denyFromOtherRoleWins() {
    Kata30_PermissionBitmaskEvaluator evaluator = newEvaluator();
    evaluator.registerRole(
        Role.EDITOR, EnumSet.of(Permission.READ, Permission.WRITE), noPermissions(), noRoles());
    evaluator.registerRole(Role.AUDITOR, noPermissions(), EnumSet.of(Permission.READ), noRoles());

    UserContext both = user("dave", Role.EDITOR, Role.AUDITOR);
    assertThat(evaluator.effectivePermissions(both)).containsExactly(Permission.WRITE);
    assertThat(evaluator.hasPermission(both, resource("owner", Map.of()), Permission.READ))
        .isFalse();
  }

  @Test
  @DisplayName("Одновременные grant и deny одной роли: побеждает DENY")
  void directGrantAndDenySameRole() {
    Kata30_PermissionBitmaskEvaluator evaluator = newEvaluator();
    evaluator.registerRole(
        Role.AUDITOR, EnumSet.of(Permission.READ), EnumSet.of(Permission.READ), noRoles());

    UserContext auditor = user("eve", Role.AUDITOR);
    assertThat(evaluator.effectivePermissions(auditor)).isEmpty();
    assertThat(evaluator.hasPermission(auditor, resource("owner", Map.of()), Permission.READ))
        .isFalse();
  }

  @Test
  @DisplayName("Условное ALLOW даёт право сверх статических и проверяет контекст")
  void conditionalAllowBeyondStaticPermissions() {
    Kata30_PermissionBitmaskEvaluator evaluator = newEvaluator();
    evaluator.registerRole(Role.VIEWER, EnumSet.of(Permission.READ), noPermissions(), noRoles());
    evaluator.addConditionalRule(
        true,
        EnumSet.of(Role.VIEWER),
        EnumSet.of(Permission.DELETE),
        (user, resource) -> user.userId().equals(resource.ownerId()));

    UserContext viewer = user("alice", Role.VIEWER);
    assertThat(evaluator.hasPermission(viewer, resource("alice", Map.of()), Permission.DELETE))
        .isTrue();
    assertThat(evaluator.hasPermission(viewer, resource("bob", Map.of()), Permission.DELETE))
        .isFalse();
    assertThat(evaluator.effectivePermissions(viewer)).containsExactly(Permission.READ);
  }

  @Test
  @DisplayName("Условное DENY сильнее статического GRANT и условного ALLOW")
  void conditionalDenyWins() {
    Kata30_PermissionBitmaskEvaluator evaluator = newEvaluator();
    evaluator.registerRole(Role.EDITOR, EnumSet.of(Permission.WRITE), noPermissions(), noRoles());
    evaluator.addConditionalRule(
        true, EnumSet.of(Role.EDITOR), EnumSet.of(Permission.WRITE), (user, resource) -> true);
    evaluator.addConditionalRule(
        false,
        EnumSet.of(Role.EDITOR),
        EnumSet.of(Permission.WRITE),
        (user, resource) -> Boolean.TRUE.equals(resource.attributes().get("locked")));

    UserContext editor = user("bob", Role.EDITOR);
    assertThat(
            evaluator.hasPermission(
                editor, resource("owner", Map.of("locked", true)), Permission.WRITE))
        .isFalse();
    assertThat(
            evaluator.hasPermission(
                editor, resource("owner", Map.of("locked", false)), Permission.WRITE))
        .isTrue();
  }

  @Test
  @DisplayName("Условное правило роли-предка применяется к пользователю роли-потомка")
  void conditionalRuleAppliesToInheritedRole() {
    Kata30_PermissionBitmaskEvaluator evaluator = newEvaluator();
    evaluator.registerRole(Role.VIEWER, noPermissions(), noPermissions(), noRoles());
    evaluator.registerRole(
        Role.EDITOR, EnumSet.of(Permission.WRITE), noPermissions(), EnumSet.of(Role.VIEWER));
    evaluator.addConditionalRule(
        true, EnumSet.of(Role.VIEWER), EnumSet.of(Permission.DELETE), (user, resource) -> true);

    UserContext editor = user("bob", Role.EDITOR);
    assertThat(evaluator.hasPermission(editor, resource("owner", Map.of()), Permission.DELETE))
        .isTrue();
    assertThat(evaluator.hasPermission(editor, resource("owner", Map.of()), Permission.ADMIN))
        .isFalse();
  }

  @Test
  @DisplayName("Правило не применяется к чужой роли и чужому праву")
  void conditionalRuleScope() {
    Kata30_PermissionBitmaskEvaluator evaluator = newEvaluator();
    evaluator.addConditionalRule(
        true, EnumSet.of(Role.AUDITOR), EnumSet.of(Permission.READ), (user, resource) -> true);

    assertThat(
            evaluator.hasPermission(
                user("viewer", Role.VIEWER), resource("owner", Map.of()), Permission.READ))
        .isFalse();
    assertThat(
            evaluator.hasPermission(
                user("auditor", Role.AUDITOR), resource("owner", Map.of()), Permission.WRITE))
        .isFalse();
    assertThat(
            evaluator.hasPermission(
                user("auditor", Role.AUDITOR), resource("owner", Map.of()), Permission.READ))
        .isTrue();
  }

  @Test
  @DisplayName("Цикл наследования отклоняется, состояние не меняется")
  void cycleRejectedWithoutStateChange() {
    Kata30_PermissionBitmaskEvaluator evaluator = newEvaluator();
    evaluator.registerRole(Role.VIEWER, EnumSet.of(Permission.READ), noPermissions(), noRoles());
    evaluator.registerRole(
        Role.EDITOR, EnumSet.of(Permission.WRITE), noPermissions(), EnumSet.of(Role.VIEWER));

    assertThatThrownBy(() -> evaluator.addParentRole(Role.VIEWER, Role.EDITOR))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cycle");
    assertThatThrownBy(() -> evaluator.addParentRole(Role.AUDITOR, Role.AUDITOR))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cycle");

    assertThat(evaluator.effectivePermissions(user("viewer", Role.VIEWER)))
        .containsExactly(Permission.READ);
    assertThat(evaluator.effectivePermissions(user("editor", Role.EDITOR)))
        .containsExactlyInAnyOrder(Permission.READ, Permission.WRITE);
  }

  @Test
  @DisplayName("Перерегистрация роли с созданием цикла не применяется")
  void reRegistrationCycleRejectedAtomically() {
    Kata30_PermissionBitmaskEvaluator evaluator = newEvaluator();
    evaluator.registerRole(Role.VIEWER, EnumSet.of(Permission.READ), noPermissions(), noRoles());
    evaluator.registerRole(
        Role.EDITOR, EnumSet.of(Permission.WRITE), noPermissions(), EnumSet.of(Role.VIEWER));
    evaluator.registerRole(
        Role.ADMIN, EnumSet.of(Permission.DELETE), noPermissions(), EnumSet.of(Role.EDITOR));

    assertThatThrownBy(
            () ->
                evaluator.registerRole(
                    Role.VIEWER,
                    EnumSet.of(Permission.ADMIN),
                    noPermissions(),
                    EnumSet.of(Role.ADMIN)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cycle");

    // Новые grants не применились, старая иерархия цела.
    assertThat(evaluator.effectivePermissions(user("viewer", Role.VIEWER)))
        .containsExactly(Permission.READ);
    assertThat(evaluator.effectivePermissions(user("admin", Role.ADMIN)))
        .containsExactlyInAnyOrder(Permission.READ, Permission.WRITE, Permission.DELETE);
  }

  @Test
  @DisplayName("Повторное добавление ребра идемпотентно")
  void addParentRoleIdempotent() {
    Kata30_PermissionBitmaskEvaluator evaluator = newEvaluator();
    evaluator.registerRole(Role.VIEWER, EnumSet.of(Permission.READ), noPermissions(), noRoles());
    evaluator.registerRole(Role.EDITOR, EnumSet.of(Permission.WRITE), noPermissions(), noRoles());

    evaluator.addParentRole(Role.EDITOR, Role.VIEWER);
    evaluator.addParentRole(Role.EDITOR, Role.VIEWER);

    assertThat(evaluator.effectivePermissions(user("editor", Role.EDITOR)))
        .containsExactlyInAnyOrder(Permission.READ, Permission.WRITE);
  }

  @Test
  @DisplayName("Upsert роли пересчитывает права потомков")
  void registerRoleUpsertRecalculates() {
    Kata30_PermissionBitmaskEvaluator evaluator = newEvaluator();
    evaluator.registerRole(Role.VIEWER, EnumSet.of(Permission.READ), noPermissions(), noRoles());
    evaluator.registerRole(
        Role.EDITOR, EnumSet.of(Permission.WRITE), noPermissions(), EnumSet.of(Role.VIEWER));
    assertThat(evaluator.effectivePermissions(user("editor", Role.EDITOR)))
        .containsExactlyInAnyOrder(Permission.READ, Permission.WRITE);

    evaluator.registerRole(
        Role.VIEWER, EnumSet.of(Permission.DELETE), EnumSet.of(Permission.READ), noRoles());

    assertThat(evaluator.effectivePermissions(user("editor", Role.EDITOR)))
        .containsExactlyInAnyOrder(Permission.WRITE, Permission.DELETE);
  }

  @Test
  @DisplayName("Возвращаемые коллекции неизменяемы, входные коллекции копируются")
  void defensiveCopies() {
    Kata30_PermissionBitmaskEvaluator evaluator = newEvaluator();
    EnumSet<Permission> grants = EnumSet.of(Permission.READ);
    EnumSet<Role> parents = noRoles();
    evaluator.registerRole(Role.VIEWER, grants, noPermissions(), parents);
    grants.clear();
    parents.add(Role.EDITOR);

    UserContext viewer = user("alice", Role.VIEWER);
    Set<Permission> effective = evaluator.effectivePermissions(viewer);
    assertThat(effective).containsExactly(Permission.READ);
    assertThatThrownBy(() -> effective.add(Permission.ADMIN))
        .isInstanceOf(UnsupportedOperationException.class);

    EnumSet<Role> userRoles = EnumSet.of(Role.VIEWER);
    UserContext user = new UserContext("bob", userRoles);
    userRoles.add(Role.ADMIN);
    assertThat(user.roles()).containsExactly(Role.VIEWER);
    assertThatThrownBy(() -> user.roles().add(Role.AUDITOR))
        .isInstanceOf(UnsupportedOperationException.class);

    Map<String, Object> attributes = new HashMap<>();
    attributes.put("public", true);
    ResourceContext resource = new ResourceContext("doc", "owner", attributes);
    attributes.put("public", false);
    assertThat(resource.attributes()).containsEntry("public", true);
    assertThatThrownBy(() -> resource.attributes().put("x", 1))
        .isInstanceOf(UnsupportedOperationException.class);

    EnumSet<Role> ruleRoles = EnumSet.of(Role.VIEWER);
    evaluator.addConditionalRule(true, ruleRoles, EnumSet.of(Permission.DELETE), (u, r) -> true);
    ruleRoles.clear();
    assertThat(evaluator.hasPermission(viewer, resource, Permission.DELETE)).isTrue();
  }

  @Test
  @DisplayName("Lock-free чтение под конкурентной перерегистрацией другой роли")
  void concurrentReads() throws Exception {
    Kata30_PermissionBitmaskEvaluator evaluator = newEvaluator();
    evaluator.registerRole(Role.VIEWER, EnumSet.of(Permission.READ), noPermissions(), noRoles());
    UserContext viewer = user("alice", Role.VIEWER);
    ResourceContext resource = resource("owner", Map.of());

    ExecutorService pool = Executors.newFixedThreadPool(5);
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Callable<Void>> tasks = new ArrayList<>();
      tasks.add(
          () -> {
            start.await();
            for (int i = 0; i < 2000; i++) {
              evaluator.registerRole(
                  Role.AUDITOR,
                  EnumSet.of(i % 2 == 0 ? Permission.READ : Permission.WRITE),
                  noPermissions(),
                  noRoles());
            }
            return null;
          });
      for (int reader = 0; reader < 4; reader++) {
        tasks.add(
            () -> {
              start.await();
              for (int i = 0; i < 5000; i++) {
                if (!evaluator.effectivePermissions(viewer).equals(EnumSet.of(Permission.READ))) {
                  throw new AssertionError("unexpected effective permissions");
                }
                if (!evaluator.hasPermission(viewer, resource, Permission.READ)) {
                  throw new AssertionError("READ unexpectedly denied");
                }
              }
              return null;
            });
      }

      List<Future<Void>> futures = new ArrayList<>();
      for (Callable<Void> task : tasks) {
        futures.add(pool.submit(task));
      }
      start.countDown();
      for (Future<Void> future : futures) {
        future.get(20, TimeUnit.SECONDS);
      }
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  @DisplayName("null-аргументы и невалидные контексты")
  void validation() {
    Kata30_PermissionBitmaskEvaluator evaluator = newEvaluator();
    assertThatThrownBy(
            () -> evaluator.registerRole(null, noPermissions(), noPermissions(), noRoles()))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> evaluator.registerRole(Role.VIEWER, null, noPermissions(), noRoles()))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> evaluator.registerRole(Role.VIEWER, noPermissions(), null, noRoles()))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
            () -> evaluator.registerRole(Role.VIEWER, noPermissions(), noPermissions(), null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
            () ->
                evaluator.registerRole(
                    Role.VIEWER,
                    Collections.<Permission>singleton(null),
                    noPermissions(),
                    noRoles()))
        .isInstanceOf(NullPointerException.class);

    assertThatThrownBy(() -> evaluator.addParentRole(null, Role.VIEWER))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> evaluator.addParentRole(Role.VIEWER, null))
        .isInstanceOf(NullPointerException.class);

    assertThatThrownBy(
            () ->
                evaluator.addConditionalRule(
                    true, noRoles(), EnumSet.of(Permission.READ), (u, r) -> true))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                evaluator.addConditionalRule(
                    true, EnumSet.of(Role.VIEWER), noPermissions(), (u, r) -> true))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                evaluator.addConditionalRule(
                    true, EnumSet.of(Role.VIEWER), EnumSet.of(Permission.READ), null))
        .isInstanceOf(NullPointerException.class);

    UserContext user = user("alice", Role.VIEWER);
    ResourceContext resource = resource("owner", Map.of());
    assertThatThrownBy(() -> evaluator.hasPermission(null, resource, Permission.READ))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> evaluator.hasPermission(user, null, Permission.READ))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> evaluator.hasPermission(user, resource, null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> evaluator.effectivePermissions(null))
        .isInstanceOf(NullPointerException.class);

    assertThatThrownBy(() -> new UserContext(" ", noRoles()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new UserContext("bob", null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new UserContext("bob", Collections.<Role>singleton(null)))
        .isInstanceOf(NullPointerException.class);

    assertThatThrownBy(() -> new ResourceContext(" ", "owner", Map.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ResourceContext("doc", " ", Map.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ResourceContext("doc", "owner", null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
            () -> new ResourceContext("doc", "owner", Collections.singletonMap("k", null)))
        .isInstanceOf(NullPointerException.class);
  }

  private static UserContext user(String id, Role... roles) {
    EnumSet<Role> set = EnumSet.noneOf(Role.class);
    set.addAll(Arrays.asList(roles));
    return new UserContext(id, set);
  }

  private static ResourceContext resource(String ownerId, Map<String, Object> attributes) {
    return new ResourceContext("doc-1", ownerId, attributes);
  }

  private static EnumSet<Permission> noPermissions() {
    return EnumSet.noneOf(Permission.class);
  }

  private static EnumSet<Role> noRoles() {
    return EnumSet.noneOf(Role.class);
  }
}
