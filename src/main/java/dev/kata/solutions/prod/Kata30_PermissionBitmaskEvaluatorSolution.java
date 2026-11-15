package dev.kata.solutions.prod;

import dev.kata.prod.Kata30_PermissionBitmaskEvaluator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Эталонное решение Kata 30.
 *
 * <h2>EnumSet как битовая маска</h2>
 *
 * <p>{@link EnumSet} для enum из ≤64 элементов — это {@code RegularEnumSet} поверх одного {@code
 * long}: {@code contains}/{@code addAll}/{@code removeAll} компилируются в битовые операции, без
 * боксинга, хеширования и аллокаций на элемент. Четыре права и четыре роли укладываются в одно
 * слово; для этой задачи это оптимальная структура. {@link EnumMap} аналогично хранит значения в
 * массиве по {@code ordinal}.
 *
 * <h2>Снимок вместо блокировки на чтении</h2>
 *
 * <p>Мутации редки и сериализуются {@code synchronized}. После каждой мутации строится неизменяемый
 * {@link State}: для каждой роли заранее вычислены объединённые grants/denies предков и замыкание
 * ролей. Ссылка на State публикуется через {@code volatile}, поэтому читатели видят либо старый,
 * либо новый снимок целиком, никогда — полусостояние. {@code hasPermission} не берёт lock и не
 * обходит граф наследования.
 *
 * <h2>Порядок разрешения</h2>
 *
 * <p>Сначала собираются статические эффективные права пользователя по всем его ролям. Если право
 * явно запрещено — отказ сразу. Затем перебираются условные правила: сработавший DENY немедленно
 * запрещает, сработавший ALLOW запоминается. В конце статический grant или сработавший allow дают
 * доступ; иначе default deny. Так DENY побеждает независимо от порядка регистрации правил.
 *
 * <h2>Иммутабельность</h2>
 *
 * <p>Входные коллекции копируются в {@link EnumSet} при регистрации, правила хранят собственные
 * копии, снимок не выдаёт мутабельных ссылок. {@link #effectivePermissions} возвращает
 * unmodifiable-обёртку над локальным {@link EnumSet}; изменить состояние движка через неё нельзя.
 */
public final class Kata30_PermissionBitmaskEvaluatorSolution
    extends Kata30_PermissionBitmaskEvaluator {

  private final EnumMap<Role, RoleDefinition> definitions = new EnumMap<>(Role.class);
  private final List<Rule> rules = new ArrayList<>();
  private volatile State state;

  public Kata30_PermissionBitmaskEvaluatorSolution() {
    state = buildState();
  }

  @Override
  public synchronized void registerRole(
      Role role, Set<Permission> grants, Set<Permission> denies, Set<Role> parents) {
    Objects.requireNonNull(role, "role");
    EnumSet<Permission> grantSet = copyEnumSet(grants, "grants", Permission.class);
    EnumSet<Permission> denySet = copyEnumSet(denies, "denies", Permission.class);
    EnumSet<Role> parentSet = copyEnumSet(parents, "parents", Role.class);

    Map<Role, EnumSet<Role>> proposed = currentParents();
    proposed.put(role, parentSet);
    ensureAcyclic(proposed);

    RoleDefinition definition = new RoleDefinition();
    definition.grants.addAll(grantSet);
    definition.denies.addAll(denySet);
    definition.parents.addAll(parentSet);
    definitions.put(role, definition);
    rebuild();
  }

  @Override
  public synchronized void addParentRole(Role child, Role parent) {
    Objects.requireNonNull(child, "child");
    Objects.requireNonNull(parent, "parent");
    if (child == parent) {
      throw new IllegalArgumentException("Role inheritance cycle: [" + child + ", " + parent + "]");
    }

    RoleDefinition existing = definitions.get(child);
    if (existing != null && existing.parents.contains(parent)) {
      return;
    }

    Map<Role, EnumSet<Role>> proposed = currentParents();
    proposed.get(child).add(parent);
    ensureAcyclic(proposed);

    RoleDefinition definition = existing != null ? existing : new RoleDefinition();
    definition.parents.add(parent);
    definitions.put(child, definition);
    rebuild();
  }

  @Override
  public synchronized void addConditionalRule(
      boolean allow,
      Set<Role> roles,
      Set<Permission> permissions,
      BiPredicate<UserContext, ResourceContext> predicate) {
    EnumSet<Role> roleSet = copyEnumSet(roles, "roles", Role.class);
    EnumSet<Permission> permissionSet = copyEnumSet(permissions, "permissions", Permission.class);
    Objects.requireNonNull(predicate, "predicate");
    if (roleSet.isEmpty()) {
      throw new IllegalArgumentException("roles must not be empty");
    }
    if (permissionSet.isEmpty()) {
      throw new IllegalArgumentException("permissions must not be empty");
    }

    rules.add(new Rule(allow, roleSet, permissionSet, predicate));
    rebuild();
  }

  @Override
  public boolean hasPermission(UserContext user, ResourceContext resource, Permission permission) {
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(resource, "resource");
    Objects.requireNonNull(permission, "permission");

    State snapshot = state;
    EnumSet<Permission> grants = EnumSet.noneOf(Permission.class);
    EnumSet<Permission> denies = EnumSet.noneOf(Permission.class);
    EnumSet<Role> effectiveRoles = EnumSet.noneOf(Role.class);
    collect(snapshot, user, grants, denies, effectiveRoles);

    if (denies.contains(permission)) {
      return false;
    }

    boolean allowedByRule = false;
    for (Rule rule : snapshot.rules()) {
      if (!rule.permissions().contains(permission)
          || Collections.disjoint(rule.roles(), effectiveRoles)) {
        continue;
      }
      if (rule.predicate().test(user, resource)) {
        if (!rule.allow()) {
          return false;
        }
        allowedByRule = true;
      }
    }

    return grants.contains(permission) || allowedByRule;
  }

  @Override
  public Set<Permission> effectivePermissions(UserContext user) {
    Objects.requireNonNull(user, "user");

    State snapshot = state;
    EnumSet<Permission> grants = EnumSet.noneOf(Permission.class);
    EnumSet<Permission> denies = EnumSet.noneOf(Permission.class);
    EnumSet<Role> effectiveRoles = EnumSet.noneOf(Role.class);
    collect(snapshot, user, grants, denies, effectiveRoles);
    grants.removeAll(denies);
    return Collections.unmodifiableSet(grants);
  }

  private static void collect(
      State snapshot,
      UserContext user,
      EnumSet<Permission> grants,
      EnumSet<Permission> denies,
      EnumSet<Role> effectiveRoles) {
    for (Role role : user.roles()) {
      EffectiveRole effective = snapshot.byRole().get(role);
      if (effective != null) {
        grants.addAll(effective.grants());
        denies.addAll(effective.denies());
        effectiveRoles.addAll(effective.roles());
      }
    }
  }

  private void rebuild() {
    state = buildState();
  }

  private State buildState() {
    EnumMap<Role, EffectiveRole> byRole = new EnumMap<>(Role.class);
    for (Role role : Role.values()) {
      EnumSet<Permission> grants = EnumSet.noneOf(Permission.class);
      EnumSet<Permission> denies = EnumSet.noneOf(Permission.class);
      EnumSet<Role> closure = EnumSet.noneOf(Role.class);

      Deque<Role> stack = new ArrayDeque<>();
      stack.push(role);
      while (!stack.isEmpty()) {
        Role current = stack.pop();
        if (!closure.add(current)) {
          continue;
        }
        RoleDefinition definition = definitions.get(current);
        if (definition != null) {
          grants.addAll(definition.grants);
          denies.addAll(definition.denies);
          for (Role parent : definition.parents) {
            stack.push(parent);
          }
        }
      }

      byRole.put(
          role,
          new EffectiveRole(
              Collections.unmodifiableSet(grants),
              Collections.unmodifiableSet(denies),
              Collections.unmodifiableSet(closure)));
    }
    return new State(Collections.unmodifiableMap(byRole), List.copyOf(rules));
  }

  private Map<Role, EnumSet<Role>> currentParents() {
    EnumMap<Role, EnumSet<Role>> parents = new EnumMap<>(Role.class);
    for (Role role : Role.values()) {
      RoleDefinition definition = definitions.get(role);
      parents.put(
          role,
          definition == null ? EnumSet.noneOf(Role.class) : EnumSet.copyOf(definition.parents));
    }
    return parents;
  }

  /** DFS по графу родителей. Ролей четыре, поэтому рекурсия безопасна и читается лучше стека. */
  private static void ensureAcyclic(Map<Role, EnumSet<Role>> parents) {
    EnumSet<Role> visited = EnumSet.noneOf(Role.class);
    EnumSet<Role> onStack = EnumSet.noneOf(Role.class);
    Deque<Role> path = new ArrayDeque<>();
    for (Role role : Role.values()) {
      if (!visited.contains(role)) {
        visit(role, parents, visited, onStack, path);
      }
    }
  }

  private static void visit(
      Role role,
      Map<Role, EnumSet<Role>> parents,
      EnumSet<Role> visited,
      EnumSet<Role> onStack,
      Deque<Role> path) {
    if (visited.contains(role)) {
      return;
    }
    if (onStack.contains(role)) {
      List<Role> all = new ArrayList<>(path);
      int start = all.indexOf(role);
      List<Role> cycle = new ArrayList<>(all.subList(start, all.size()));
      cycle.add(role);
      throw new IllegalArgumentException("Role inheritance cycle: " + cycle);
    }

    onStack.add(role);
    path.addLast(role);
    for (Role parent : parents.getOrDefault(role, EnumSet.noneOf(Role.class))) {
      visit(parent, parents, visited, onStack, path);
    }
    path.removeLast();
    onStack.remove(role);
    visited.add(role);
  }

  private static <E extends Enum<E>> EnumSet<E> copyEnumSet(
      Set<E> source, String field, Class<E> type) {
    Objects.requireNonNull(source, field);
    EnumSet<E> copy = EnumSet.noneOf(type);
    for (E element : source) {
      copy.add(Objects.requireNonNull(element, field));
    }
    return copy;
  }

  private static final class RoleDefinition {
    final EnumSet<Permission> grants = EnumSet.noneOf(Permission.class);
    final EnumSet<Permission> denies = EnumSet.noneOf(Permission.class);
    final EnumSet<Role> parents = EnumSet.noneOf(Role.class);
  }

  private record EffectiveRole(Set<Permission> grants, Set<Permission> denies, Set<Role> roles) {}

  private record Rule(
      boolean allow,
      Set<Role> roles,
      Set<Permission> permissions,
      BiPredicate<UserContext, ResourceContext> predicate) {
    private Rule {
      roles = Collections.unmodifiableSet(EnumSet.copyOf(roles));
      permissions = Collections.unmodifiableSet(EnumSet.copyOf(permissions));
      Objects.requireNonNull(predicate, "predicate");
    }
  }

  private record State(Map<Role, EffectiveRole> byRole, List<Rule> rules) {}
}
