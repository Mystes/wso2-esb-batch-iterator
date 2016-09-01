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

import static org.junit.Assert.assertTrue;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.eip.Target;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;
import org.junit.Before;
import org.junit.Test;

import fi.mystes.synapse.mediator.BatchIteratorMediator;
import fi.mystes.synapse.mediator.config.BatchIteratorConstants;

public class BatchIteratorMediatorSerializerTest {

    private BatchIteratorMediatorSerializer serializer;
    private BatchIteratorMediator mediator;

    @Before
    public void setUp() throws JaxenException {
        serializer = new BatchIteratorMediatorSerializer();
        mediator = new BatchIteratorMediator();
        mediator.setContinueParent(true);
        mediator.setPreservePayload(false);
        mediator.setBatchSize(4);
        mediator.setExpression(new SynapseXPath("//iterate"));
        Target target = new Target();
        mediator.setTarget(target);
        SequenceMediator sequence = new SequenceMediator();
        target.setSequence(sequence);
    }

    @Test
    public void shouldSerializeBatchIteratorMediator() {
        OMElement mediatorElement = serializer.serializeSpecificMediator(mediator);

        assertTrue("Element name must be " + BatchIteratorConstants.ROOT_TAG_NAME,
                mediatorElement.getLocalName().equals(BatchIteratorConstants.ROOT_TAG_NAME));

        assertTrue("batchSie attribute should countain value of 4",
                mediatorElement.getAttributeValue(new QName(BatchIteratorConstants.ATT_BATCH_SIZE)).equals("4"));

        assertTrue("continueParent should contain value of true",
                mediatorElement.getAttributeValue(new QName("continueParent")).equals("true"));

        assertTrue("preservePayload should not be present due to false value",
                mediatorElement.getAttributeValue(new QName("preservePayload")) == null);

        assertTrue("expression should contain value of //iterate",
                mediatorElement.getAttributeValue(new QName("expression")).equals("//iterate"));
    }
}
