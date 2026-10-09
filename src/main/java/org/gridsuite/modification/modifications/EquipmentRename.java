/*
  Copyright (c) 2026, RTE (http://www.rte-france.com)
  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.modification.modifications;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.iidm.network.Connectable;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.ConnectablePosition;
import lombok.*;
import org.gridsuite.modification.ModificationType;
import org.gridsuite.modification.dto.FreePropertyInfos;
import org.gridsuite.modification.error.NetworkModificationException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.gridsuite.modification.error.NetworkModificationExceptionType.*;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EquipmentRename extends AbstractEquipmentBase {

    private String newEquipmentId;

    @Builder
    public EquipmentRename(String equipmentId, List<FreePropertyInfos> properties, String newEquipmentId) {
        super(equipmentId, properties);
        this.newEquipmentId = newEquipmentId;
    }

    @Override
    public void check(Network network) throws NetworkModificationException {
        Identifiable<?> identifiable = network.getIdentifiable(equipmentId);
        if (identifiable == null) {
            throw new NetworkModificationException(EQUIPMENT_NOT_FOUND, "Equipment with id=" + equipmentId + " not found");
        }
        if (!(identifiable instanceof Connectable<?>)) {
            throw new NetworkModificationException(WRONG_EQUIPMENT_TYPE,
                    String.format("Equipment '%s' of type %s cannot be renamed", equipmentId, identifiable.getType()));
        }
        if (newEquipmentId == null || newEquipmentId.isBlank()) {
            throw new NetworkModificationException(MODIFICATION_ERROR, "New id of equipment '" + equipmentId + "' is empty");
        }
        if (network.getIdentifiable(newEquipmentId) != null) {
            throw new NetworkModificationException(EQUIPMENT_ALREADY_EXISTS, newEquipmentId);
        }
    }

    @Override
    public void apply(Network network, ReportNode subReportNode) {
        Connectable<?> connectable = (Connectable<?>) network.getIdentifiable(equipmentId);
        boolean nameFollowsId = connectable.getOptionalName().filter(equipmentId::equals).isPresent();

        try {
            connectable.setId(newEquipmentId);
        } catch (UnsupportedOperationException e) {
            throw new NetworkModificationException(MODIFICATION_ERROR,
                    "Renaming '" + equipmentId + "' is not supported by this network implementation");
        }
        if (nameFollowsId) {
            connectable.setName(newEquipmentId);
        }
        renameFeeders(connectable);

        subReportNode.newReportNode()
                .withMessageTemplate("network.modification.equipmentRenamed")
                .withUntypedValue("type", connectable.getType().name())
                .withUntypedValue("oldId", equipmentId)
                .withUntypedValue("newId", newEquipmentId)
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
    }

    private void renameFeeders(Connectable<?> connectable) {
        ConnectablePosition<?> position = (ConnectablePosition<?>) connectable.getExtension(ConnectablePosition.class);
        if (position == null) {
            return;
        }
        Stream.of(position.getFeeder(), position.getFeeder1(), position.getFeeder2(), position.getFeeder3())
                .filter(Objects::nonNull)
                .filter(feeder -> feeder.getName().filter(equipmentId::equals).isPresent())
                .forEach(feeder -> feeder.setName(newEquipmentId));
    }

    @Override
    public String getName() {
        return ModificationType.EQUIPMENT_RENAME.name();
    }
}
