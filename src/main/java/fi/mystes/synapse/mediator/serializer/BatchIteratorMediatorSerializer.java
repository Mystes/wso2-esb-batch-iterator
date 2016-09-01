/**
 * Copyright 2016 Mystes Oy
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
package fi.mystes.synapse.mediator.serializer;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.config.xml.IterateMediatorSerializer;

import fi.mystes.synapse.mediator.BatchIteratorMediator;
import fi.mystes.synapse.mediator.config.BatchIteratorConstants;

public class BatchIteratorMediatorSerializer extends IterateMediatorSerializer {

    @Override
    public String getMediatorClassName() {
        return BatchIteratorMediator.class.getName();
    }

    @Override
    public OMElement serializeSpecificMediator(Mediator m) {
        OMElement element = super.serializeSpecificMediator(m);
        element.setLocalName(BatchIteratorConstants.ROOT_TAG_NAME);
        Integer batchSize = ((BatchIteratorMediator) m).getBatchSize();
        element.addAttribute(BatchIteratorConstants.ATT_BATCH_SIZE, batchSize != null ? batchSize.toString() : "1",
                null);
        return element;
    }

}
