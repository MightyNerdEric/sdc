/*-
 * ============LICENSE_START=======================================================
 * SDC
 * ================================================================================
 * Copyright (C) 2021, Nordix Foundation. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.openecomp.sdc.be.components.attribute;

import fj.data.Either;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.collections4.CollectionUtils;
import org.openecomp.sdc.be.components.impl.ComponentInstanceBusinessLogic;
import org.openecomp.sdc.be.datatypes.elements.AttributeDataDefinition;
import org.openecomp.sdc.be.datatypes.elements.GetOutputValueDataDefinition;
import org.openecomp.sdc.be.model.Component;
import org.openecomp.sdc.be.model.ComponentInstance;
import org.openecomp.sdc.be.model.ComponentInstanceAttribute;
import org.openecomp.sdc.be.model.OutputDefinition;
import org.openecomp.sdc.be.model.jsonjanusgraph.operations.ToscaOperationFacade;
import org.openecomp.sdc.be.model.operations.api.StorageOperationStatus;
import org.openecomp.sdc.common.log.wrappers.Logger;

@org.springframework.stereotype.Component
public class ComponentInstanceAttributeDeclarator extends DefaultAttributeDeclarator<ComponentInstance, ComponentInstanceAttribute> {

    private static final Logger log = Logger.getLogger(ComponentInstanceAttributeDeclarator.class);
    private ToscaOperationFacade toscaOperationFacade;
    private ComponentInstanceBusinessLogic componentInstanceBusinessLogic;

    public ComponentInstanceAttributeDeclarator(final ToscaOperationFacade toscaOperationFacade,
                                                final ComponentInstanceBusinessLogic componentInstanceBusinessLogic) {
        this.toscaOperationFacade = toscaOperationFacade;
        this.componentInstanceBusinessLogic = componentInstanceBusinessLogic;
    }

    @Override
    public ComponentInstanceAttribute createDeclaredAttribute(AttributeDataDefinition attributeDataDefinition) {
        return new ComponentInstanceAttribute(attributeDataDefinition);
    }

    @Override
    public Either<?, StorageOperationStatus> updateAttributesValues(final Component component, final String cmptInstanceId,
                                                                    final List<ComponentInstanceAttribute> attributetypeList) {
        log.debug("#updateAttributesValues - updating component instance attributes for instance {} on component {}", cmptInstanceId,
            component.getUniqueId());
        Map<String, List<ComponentInstanceAttribute>> instAttributes = Collections.singletonMap(cmptInstanceId, attributetypeList);
        return toscaOperationFacade.addComponentInstanceAttributesToComponent(component, instAttributes);
    }

    @Override
    public Optional<ComponentInstance> resolvePropertiesOwner(final Component component, final String propertiesOwnerId) {
        log.debug("#resolvePropertiesOwner - fetching component instance {} of component {}", propertiesOwnerId, component.getUniqueId());
        return component.getComponentInstanceById(propertiesOwnerId);
    }

    @Override
    public StorageOperationStatus unDeclareAttributesAsOutputs(final Component component, final OutputDefinition output) {
        final List<ComponentInstanceAttribute> componentInstanceAttributesDeclaredAsOutput = componentInstanceBusinessLogic
            .getComponentInstanceAttributesByOutputId(component, output.getUniqueId());
        if (CollectionUtils.isEmpty(componentInstanceAttributesDeclaredAsOutput)) {
            return StorageOperationStatus.OK;
        }
        unDeclareOutput(output, componentInstanceAttributesDeclaredAsOutput);
        return toscaOperationFacade
            .updateComponentInstanceAttributes(component, componentInstanceAttributesDeclaredAsOutput.get(0).getComponentInstanceId(),
                componentInstanceAttributesDeclaredAsOutput);
    }

    private void unDeclareOutput(OutputDefinition output,
                                 List<ComponentInstanceAttribute> componentInstanceAttributesDeclaredAsOutput) {
        componentInstanceAttributesDeclaredAsOutput.forEach(attribute -> {
            Optional<GetOutputValueDataDefinition> attributeOutput =
                attribute.getGetOutputValues().stream().filter(attOut -> output.getUniqueId().equals(attOut.getOutputId())).findFirst();
            if (attributeOutput.isPresent()) {
                attribute.getGetOutputValues().remove(attributeOutput.get());
            }
        });
    }
}
