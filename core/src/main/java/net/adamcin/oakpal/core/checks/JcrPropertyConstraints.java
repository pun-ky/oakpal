/*
 * Copyright 2018 Mark Adamcin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.adamcin.oakpal.core.checks;

import net.adamcin.oakpal.core.SimpleViolation;
import net.adamcin.oakpal.core.Violation;
import org.apache.jackrabbit.vault.packaging.PackageId;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.adamcin.oakpal.core.JavaxJson.arrayOrEmpty;
import static net.adamcin.oakpal.core.JavaxJson.hasNonNull;
import static net.adamcin.oakpal.core.JavaxJson.mapArrayOfObjects;

/**
 * Encapsulation of constraints on a JCR property for the {@link JcrProperties} check.
 * <p>
 * {@code config} options:
 * <dl>
 * <dt>{@code name}</dt>
 * <dd>The property name to apply restrictions to.</dd>
 * <dt>{@code denyIfAbsent}</dt>
 * <dd>Report violation if this property is absent on nodes within scope.</dd>
 * <dt>{@code denyIfPresent}</dt>
 * <dd>Report violation if this property is present on nodes within scope.</dd>
 * <dt>{@code denyIfMultivalued}</dt>
 * <dd>Report violation if this property has more than one value.</dd>
 * <dt>{@code requireType}</dt>
 * <dd>Require a specific property type. Must be a valid name for {@link PropertyType#valueFromName(String)}, e.g.
 * Binary, Boolean, Date, Decimal, Double, Long, Name, Path, Reference, String, WeakReference, or URI.</dd>
 * <dt>{@code valueRules}</dt>
 * <dd>A list of patterns to match against string values of this property. All rules are applied in sequence to each
 * value of the property. If the type of the last rule to match any value is DENY, a violation is reported.</dd>
 * <dt>{@code severity}</dt>
 * <dd>(default: {@link net.adamcin.oakpal.core.Violation.Severity#MAJOR}) specify the severity if a violation is
 * reported by this set of constraints.</dd>
 * </dl>
 */
public final class JcrPropertyConstraints {
    public static final String CONFIG_NAME = "name";
    public static final String CONFIG_DENY_IF_ABSENT = "denyIfAbsent";
    public static final String CONFIG_DENY_IF_PRESENT = "denyIfPresent";
    public static final String CONFIG_DENY_IF_MULTIVALUED = "denyIfMultivalued";
    public static final String CONFIG_REQUIRE_TYPE = "requireType";
    public static final String CONFIG_VALUE_RULES = "valueRules";
    public static final String CONFIG_SEVERITY = "severity";
    public static final Violation.Severity DEFAULT_SEVERITY = Violation.Severity.MAJOR;

    public static JcrPropertyConstraints fromJson(final JsonObject checkJson) {
        final String name = checkJson.getString(CONFIG_NAME);
        final boolean denyIfAbsent = hasNonNull(checkJson, CONFIG_DENY_IF_ABSENT)
                && checkJson.getBoolean(CONFIG_DENY_IF_ABSENT);
        final boolean denyIfPresent = hasNonNull(checkJson, CONFIG_DENY_IF_PRESENT)
                && checkJson.getBoolean(CONFIG_DENY_IF_PRESENT);
        final boolean denyIfMultivalued = hasNonNull(checkJson, CONFIG_DENY_IF_MULTIVALUED)
                && checkJson.getBoolean(CONFIG_DENY_IF_MULTIVALUED);
        final String requireType = checkJson.getString(CONFIG_REQUIRE_TYPE, null);
        final List<Rule> valueRules = Rule.fromJsonArray(arrayOrEmpty(checkJson, CONFIG_VALUE_RULES));
        final Violation.Severity severity = Violation.Severity
                .valueOf(checkJson.getString(CONFIG_SEVERITY, DEFAULT_SEVERITY.name()).toUpperCase());

        return new JcrPropertyConstraints(name, denyIfAbsent, denyIfPresent, denyIfMultivalued, requireType, valueRules,
                severity);
    }

    public static List<JcrPropertyConstraints> fromJsonArray(final JsonArray rulesArray) {
        return mapArrayOfObjects(rulesArray, JcrPropertyConstraints::fromJson);
    }

    private final String name;
    private final boolean denyIfAbsent;
    private final boolean denyIfPresent;
    private final boolean denyIfMultivalued;
    private final String requireType;
    private final List<Rule> valueRules;
    private final Violation.Severity severity;

    public JcrPropertyConstraints(final String name,
                                  final boolean denyIfAbsent,
                                  final boolean denyIfPresent,
                                  final boolean denyIfMultivalued,
                                  final String requireType,
                                  final List<Rule> valueRules,
                                  final Violation.Severity severity) {
        this.name = name;
        this.denyIfAbsent = denyIfAbsent;
        this.denyIfPresent = denyIfPresent;
        this.denyIfMultivalued = denyIfMultivalued;
        this.requireType = requireType;
        this.valueRules = valueRules;
        this.severity = severity;
    }

    public String getName() {
        return name;
    }

    public boolean isDenyIfAbsent() {
        return denyIfAbsent;
    }

    public boolean isDenyIfPresent() {
        return denyIfPresent;
    }

    public boolean isDenyIfMultivalued() {
        return denyIfMultivalued;
    }

    public String getRequireType() {
        return requireType;
    }

    public List<Rule> getValueRules() {
        return valueRules;
    }

    public Violation.Severity getSeverity() {
        return severity;
    }

    Violation constructViolation(final PackageId packageId, final Node node, final String reason)
            throws RepositoryException {
        return new SimpleViolation(getSeverity(),
                String.format("%s (t: %s, m: %s): %s -> %s",
                        node.getPath(),
                        node.getPrimaryNodeType().getName(),
                        Stream.of(node.getMixinNodeTypes())
                                .map(NodeTypeDefinition::getName)
                                .collect(Collectors.toList()),
                        reason,
                        getName()),
                packageId);
    }

    Optional<Violation> evaluate(final PackageId packageId, final Node node) throws RepositoryException {
        if (!node.hasProperty(getName())) {
            if (isDenyIfAbsent()) {
                return Optional.of(constructViolation(packageId, node, "property absent"));
            }
        } else {
            if (isDenyIfPresent()) {
                return Optional.of(constructViolation(packageId, node, "property present"));
            }

            Property property = node.getProperty(getName());

            if (isDenyIfMultivalued() && property.isMultiple()) {
                return Optional.of(constructViolation(packageId, node, "property is multivalued"));
            }

            if (getRequireType() != null && !getRequireType().isEmpty()
                    && !getRequireType().equals(PropertyType.nameFromValue(property.getType()))) {
                return Optional.of(constructViolation(packageId, node,
                        String.format("required type mismatch: %s != %s",
                                PropertyType.nameFromValue(property.getType()), getRequireType())));
            }

            List<String> values = new ArrayList<>();
            if (property.isMultiple()) {
                for (Value value : property.getValues()) {
                    values.add(value.getString());
                }
            } else {
                values.add(property.getString());
            }

            for (String value : values) {
                final Rule lastMatch = Rule.lastMatch(getValueRules(), value);
                if (lastMatch.isDeny()) {
                    return Optional.of(constructViolation(packageId, node,
                            String.format("value %s denied by pattern %s",
                                    value, lastMatch.getPattern().pattern())));
                }
            }
        }

        return Optional.empty();
    }

}
