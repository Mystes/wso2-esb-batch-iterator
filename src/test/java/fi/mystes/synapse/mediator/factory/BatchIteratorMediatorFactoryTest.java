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

import static org.junit.Assert.assertTrue;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.synapse.SynapseException;
import org.junit.Before;
import org.junit.Test;

import fi.mystes.synapse.mediator.BatchIteratorMediator;
import fi.mystes.synapse.mediator.config.BatchIteratorConstants;

public class BatchIteratorMediatorFactoryTest {
    private OMElement mediatorElement;
    private OMFactory omFactory = OMAbstractFactory.getOMFactory();
    private BatchIteratorMediatorFactory mediatorFactory;

    @Before
    public void setUp() {
        mediatorElement = omFactory.createOMElement(BatchIteratorConstants.ROOT_TAG);
        mediatorElement.addAttribute("continueParent", "true", null);
        mediatorElement.addAttribute("preservePayload", "false", null);
        mediatorElement.addAttribute("expression", "//iterate", null);
        mediatorElement.addAttribute("batchSize", "3", null);
        OMElement targetElement = omFactory
                .createOMElement(new QName(BatchIteratorConstants.NAMESPACE_STRING, "target"));
        mediatorElement.addChild(targetElement);
        OMElement sequenceElement = omFactory
                .createOMElement(new QName(BatchIteratorConstants.NAMESPACE_STRING, "sequence"));
        targetElement.addChild(sequenceElement);
        OMElement sendElement = omFactory.createOMElement(new QName(BatchIteratorConstants.NAMESPACE_STRING, "send"));
        sequenceElement.addChild(sendElement);
        mediatorFactory = new BatchIteratorMediatorFactory();
    }

    @Test
    public void shouldCreateBatchMediatorFromOMElement() {
        BatchIteratorMediator batchIteratorMediator = (BatchIteratorMediator) mediatorFactory
                .createSpecificMediator(mediatorElement, null);
        assertTrue("Expected batchSize to be set to 3", batchIteratorMediator.getBatchSize() == 3);
        assertTrue("Expected continueParent to be true", batchIteratorMediator.isContinueParent());
        assertTrue("Expected expression to be //iterate",
                batchIteratorMediator.getExpression().toString().equals("//iterate"));
        assertTrue("Expected preservePayload to be false", batchIteratorMediator.isPreservePayload() == false);
    }

    @Test(expected = SynapseException.class)
    public void shouldThrowExceptionDueToMissingBatchSizeAttribute() {
        mediatorElement.removeAttribute(omFactory.createOMAttribute("batchSize", null, "3"));
        BatchIteratorMediator batchIteratorMediator = (BatchIteratorMediator) mediatorFactory
                .createSpecificMediator(mediatorElement, null);
        assertTrue("Expected batchSize to be set to 1", batchIteratorMediator.getBatchSize() == 1);
    }
}
