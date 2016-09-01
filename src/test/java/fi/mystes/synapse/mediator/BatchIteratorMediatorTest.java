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
package fi.mystes.synapse.mediator;

import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.impl.OMNamespaceImpl;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.impl.llom.SOAPEnvelopeImpl;
import org.apache.axiom.soap.impl.llom.soap11.SOAP11BodyImpl;
import org.apache.axiom.soap.impl.llom.soap11.SOAP11Factory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.OperationContext;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.eip.EIPUtils;
import org.apache.synapse.mediators.eip.Target;
import org.apache.synapse.util.MessageHelper;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ EIPUtils.class, MessageHelper.class })
public class BatchIteratorMediatorTest {

    @Mock
    private Axis2MessageContext context;

    @Mock
    private OperationContext operationContext;

    @Mock
    private org.apache.axis2.context.MessageContext axis2MessageContext;

    @Mock
    private MessageContext cloneContext;

    @Mock
    private SynapseXPath synapseXPathMock;

    @Mock
    private Target target;

    private BatchIteratorMediator batchMediator;

    private OMFactory omFactory = OMAbstractFactory.getOMFactory();

    private OMElement payloadElement;

    private SOAPFactory soapFactory;

    private SOAPEnvelope envelope, cloneEnvelope;

    @Before
    public void setUp() throws JaxenException, AxisFault {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(EIPUtils.class, MessageHelper.class);

        batchMediator = new BatchIteratorMediator();
        payloadElement = omFactory.createOMElement(new QName("root"));

        soapFactory = new SOAP11Factory();
        envelope = new SOAPEnvelopeImpl(new OMNamespaceImpl("http://schemas.xmlsoap.org/soap/envelope/", "env"),
                soapFactory);

        cloneEnvelope = new SOAPEnvelopeImpl(new OMNamespaceImpl("http://schemas.xmlsoap.org/soap/envelope/", "env"),
                soapFactory);

        new SOAP11BodyImpl(cloneEnvelope, soapFactory);

        SOAP11BodyImpl body = new SOAP11BodyImpl(envelope, soapFactory);

        body.addChild(payloadElement);

        batchMediator.setExpression(synapseXPathMock);
        batchMediator.setTarget(target);

        @SuppressWarnings("serial")
        List<OMNode> elementMatch = new LinkedList<OMNode>() {
            {
                add(omFactory.createOMElement(new QName("iterable")));
                add(omFactory.createOMElement(new QName("iterable")));
                add(omFactory.createOMElement(new QName("iterable")));
                add(omFactory.createOMElement(new QName("iterable")));
                add(omFactory.createOMElement(new QName("iterable")));
                add(omFactory.createOMElement(new QName("iterable")));
                add(omFactory.createOMElement(new QName("iterable")));
            }
        };

        for (OMNode element : elementMatch) {
            payloadElement.addChild(element);
        }

        when(context.getEnvelope()).thenReturn(envelope);
        when(EIPUtils.getDetachedMatchingElements(notNull(SOAPEnvelopeImpl.class), notNull(MessageContext.class),
                notNull(SynapseXPath.class))).thenReturn(elementMatch);
        when(context.getAxis2MessageContext()).thenReturn(axis2MessageContext);
        when(axis2MessageContext.getOperationContext()).thenReturn(operationContext);
        when(MessageHelper.cloneMessageContext(context)).thenReturn(cloneContext);
        when(MessageHelper.cloneSOAPEnvelope(notNull(SOAPEnvelopeImpl.class))).thenReturn(cloneEnvelope);
    }

    @Test
    public void shouldBatchElementsInThree() {
        batchMediator.setBatchSize(3);
        batchMediator.mediate(context);
        verify(target, times(3)).mediate(notNull(MessageContext.class));
    }

    @Test
    public void shouldBatchElementsInTwo() {
        batchMediator.setBatchSize(4);
        batchMediator.mediate(context);
        verify(target, times(2)).mediate(notNull(MessageContext.class));
    }

    @Test
    public void shouldBatchElementsInSeven() {
        batchMediator.setBatchSize(1);
        batchMediator.mediate(context);
        verify(target, times(7)).mediate(notNull(MessageContext.class));
    }

}
