/*
  Copyright (c) 2026, RTE (http://www.rte-france.com)
  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.modification.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.powsybl.commons.report.ReportNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.gridsuite.modification.modifications.AbstractModification;
import org.gridsuite.modification.modifications.EquipmentRename;

import java.util.HashMap;
import java.util.Map;

@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
@Schema(description = "Equipment rename")
@JsonTypeName("EQUIPMENT_RENAME")
public class EquipmentRenameInfos extends EquipmentModificationInfos {
    @Schema(description = "New equipment ID")
    private String newEquipmentId;

    @Override
    public AbstractModification toModification() {
        return EquipmentRename.builder()
                .equipmentId(getEquipmentId())
                .properties(getProperties())
                .newEquipmentId(getNewEquipmentId())
                .build();
    }

    @Override
    public ReportNode createSubReportNode(ReportNode reportNode) {
        return reportNode.newReportNode()
                .withMessageTemplate("network.modification.equipmentRename")
                .withUntypedValue("equipmentId", getEquipmentId())
                .withUntypedValue("newEquipmentId", getNewEquipmentId())
                .add();
    }

    @Override
    public Map<String, String> getMapMessageValues() {
        Map<String, String> mapMessageValues = new HashMap<>();
        mapMessageValues.put("equipmentId", getEquipmentId());
        mapMessageValues.put("newEquipmentId", getNewEquipmentId());
        return mapMessageValues;
    }
}
