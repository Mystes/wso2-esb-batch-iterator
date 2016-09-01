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
package fi.mystes.synapse.mediator.factory;

import java.util.Properties;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.config.xml.IterateMediatorFactory;
import org.apache.synapse.mediators.eip.splitter.IterateMediator;

import fi.mystes.synapse.mediator.BatchIteratorMediator;
import fi.mystes.synapse.mediator.config.BatchIteratorConstants;

public class BatchIteratorMediatorFactory extends IterateMediatorFactory {

    /**
     * The QName of detach mediator element in the XML config
     * 
     * @return QName of detach mediator
     */
    @Override
    public QName getTagQName() {
        return BatchIteratorConstants.ROOT_TAG;
    }

    /**
     * Specific mediator factory implementation to build the
     * org.apache.synapse.Mediator by the given XML configuration
     * 
     * @param OMElement
     *            element configuration element describing the properties of the
     *            mediator
     * @param properties
     *            bag of properties to pass in any information to the factory
     * 
     * @return built detach mediator
     */
    @Override
    public Mediator createSpecificMediator(OMElement element, Properties properties) {
        IterateMediator mediator = (IterateMediator) super.createSpecificMediator(element, properties);
        BatchIteratorMediator batchIterator = new BatchIteratorMediator();
        batchIterator.setAttachPath(mediator.getAttachPath());
        batchIterator.setContinueParent(mediator.isContinueParent());
        batchIterator.setDescription(mediator.getDescription());
        batchIterator.setExpression(mediator.getExpression());
        batchIterator.setId(mediator.getId());
        batchIterator.setPreservePayload(mediator.isPreservePayload());
        batchIterator.setTarget(mediator.getTarget());
        batchIterator.setTraceState(mediator.getTraceState());
        String batchSize = element.getAttributeValue(new QName(BatchIteratorConstants.ATT_BATCH_SIZE));
        batchIterator.setBatchSize(new Integer(batchSize != null ? batchSize : "1"));

        return batchIterator;
    }
}
