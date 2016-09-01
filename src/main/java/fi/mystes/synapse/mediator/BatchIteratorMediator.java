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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.OperationContext;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.eip.EIPConstants;
import org.apache.synapse.mediators.eip.EIPUtils;
import org.apache.synapse.mediators.eip.Target;
import org.apache.synapse.mediators.eip.splitter.IterateMediator;
import org.apache.synapse.util.MessageHelper;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

/**
 * Custom mediator to that extends IterateMediator to iterate several elements
 * (batch) at a time.
 * 
 * <batchIterator batchSize="number" [continueParent=(true | false)]
 * [preservePayload=(true | false)] (attachPath="xpath")? expression="xpath">
 * <target [to="uri"] [soapAction="qname"] [sequence="sequence_ref"]
 * [endpoint="endpoint_ref"] > <sequence> (mediator)+ </sequence>?
 * <endpoint> endpoint </endpoint> ? </target>+ </batchIterator>
 * 
 */
public class BatchIteratorMediator extends IterateMediator {

    private Integer batchSize;

    /**
     * Invokes the mediator passing the current message for mediation. Each
     * mediator performs its mediation action, and returns true if mediation
     * should continue, or false if further mediation should be aborted.
     *
     * @param context
     *            Current message context for mediation
     * @return true if further mediation should continue, otherwise false
     */
    @Override
    public boolean mediate(MessageContext synCtx) {
        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : BatchIterate mediator");

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }

        try {
            // get a copy of the message for the processing, if the
            // continueParent is set to true
            // this original message can go in further mediations and hence we
            // should not change
            // the original message context
            SOAPEnvelope envelope = MessageHelper.cloneSOAPEnvelope(synCtx.getEnvelope());
            // get the iteration elements and iterate through the list,
            // this call will also detach all the iteration elements
            SynapseXPath expression = getExpression();
            List<?> splitElements = EIPUtils.getDetachedMatchingElements(envelope, synCtx, expression);

            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug(
                        "Splitting with XPath : " + expression + " resulted in " + splitElements.size() + " elements");
            }

            removeChildrenIfPayloadNotPreserved(envelope);

            if (batchSize == null || batchSize < 0) {
                batchSize = 1;
            }

            List<ElementBatch> batchList = batchSplittedElements(synCtx, expression, splitElements);

            proceedWithBatches(synCtx, synLog, envelope, batchList);

        } catch (JaxenException e) {
            handleException("Error evaluating split XPath expression : " + getExpression(), e, synCtx);
        } catch (AxisFault af) {
            handleException("Error creating an iterated copy of the message", af, synCtx);
        } catch (SynapseException synEx) {
            throw synEx;
        } catch (Exception e) {
            handleException("Exception occurred while executing the Batch Iterate Mediator", e, synCtx);
        }

        // if the continuation of the parent message is stopped from here set
        // the RESPONSE_WRITTEN
        // property to SKIP to skip the blank http response
        OperationContext opCtx = ((Axis2MessageContext) synCtx).getAxis2MessageContext().getOperationContext();
        if (!isContinueParent() && opCtx != null) {
            opCtx.setProperty(Constants.RESPONSE_WRITTEN, "SKIP");
        }

        synLog.traceOrDebug("End : BatchIterate mediator");

        // whether to continue mediation on the original message
        return isContinueParent();
    }

    /**
     * Getter for batch size.
     * 
     * @return
     */
    public Integer getBatchSize() {
        return batchSize;
    }

    /**
     * Setter for batch size.
     * 
     * @param batchSize
     */
    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * Helper method to remove payload from given envelope if payload was not
     * preserved.
     * 
     * @param envelope
     *            To remove payload from
     */
    private void removeChildrenIfPayloadNotPreserved(SOAPEnvelope envelope) {
        // if not preservePayload remove all the child elements
        if (!isPreservePayload() && envelope.getBody() != null) {
            for (Iterator<?> itr = envelope.getBody().getChildren(); itr.hasNext();) {
                ((OMNode) itr.next()).detach();
            }
        }
    }

    /**
     * Helper method to proceed with given batched elements.
     * 
     * @param synCtx
     *            Message context used to create batch iterable message context
     * @param synLog
     *            Used for logging
     * @param envelope
     *            SOAP Envelope instance used to clone current without modifying
     *            current one
     * @param batchList
     *            Batches to process
     * @throws AxisFault
     *             If cloning current message context fails
     * @throws JaxenException
     *             If XPath express evaluation fails
     */
    private void proceedWithBatches(MessageContext synCtx, SynapseLog synLog, SOAPEnvelope envelope,
            List<ElementBatch> batchList) throws AxisFault, JaxenException {
        int msgCount = batchList.size();
        int msgNumber = 0;
        Target target = getTarget();
        for (ElementBatch elementBatch : batchList) {
            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("Submitting " + (msgNumber + 1) + " of " + msgCount + (target.isAsynchronous()
                        ? " messages for processing in parallel" : " messages for processing in sequentially"));
            }

            MessageContext iteratedMsgCtx = getIteratedMessageContext(synCtx, msgNumber++, msgCount, envelope,
                    elementBatch.elements);
            if (target.isAsynchronous()) {
                target.mediate(iteratedMsgCtx);
            } else {
                proceedWithSequentialIteration(synCtx, target, iteratedMsgCtx);
            }
        }
    }

    /**
     * Helper method to batch given splitted elements
     * 
     * @param synCtx
     *            Used to validate splitted element
     * @param expression
     *            Used to validate splitted element
     * @param splitElements
     *            Elements to be batched in defined size
     * @return List containing batches
     */

    private List<ElementBatch> batchSplittedElements(MessageContext synCtx, SynapseXPath expression,
            List<?> splitElements) {
        List<ElementBatch> batchList = new LinkedList<ElementBatch>();
        int elementSize = splitElements.size();
        ElementBatch batch = null;
        for (int i = 0; i < elementSize; i++) {
            Object o = splitElements.get(i);
            validateIterableObject(synCtx, expression, o);
            boolean newBatch = i % batchSize == 0;
            if (newBatch || batch == null) {
                batch = new ElementBatch();
                batchList.add(batch);
            }
            batch.elements.add((OMNode) o);
        }
        return batchList;
    }

    /**
     * Helper method to proceed with sequential iteration and providing
     * exception handling.
     * 
     * @param synCtx
     *            Used for exception handling
     * @param target
     *            Target mediator to be invoked with given message context
     * @param iteratedMsgCtx
     *            Message context to be passed to target mediator
     */
    private void proceedWithSequentialIteration(MessageContext synCtx, Target target, MessageContext iteratedMsgCtx) {
        try {
            /*
             * if Iteration is sequential we won't be able to execute correct
             * fault handler as data are lost with clone message ending
             * execution. So here we copy fault stack of clone message context
             * to original message context
             */
            target.mediate(iteratedMsgCtx);
        } catch (SynapseException synEx) {
            copyFaultyIteratedMessage(synCtx, iteratedMsgCtx);
            throw synEx;
        } catch (Exception e) {
            copyFaultyIteratedMessage(synCtx, iteratedMsgCtx);
            handleException("Exception occurred while executing sequential iteration " + "in the Iterator Mediator", e,
                    synCtx);
        }
    }

    /**
     * Helper method to validate given Object o.
     * 
     * @param synCtx
     *            Used for describing validation failure
     * @param expression
     *            Used for describing validation failure
     * @param o
     *            Object to be validated whether it is instance of OMNode class
     */
    private void validateIterableObject(MessageContext synCtx, SynapseXPath expression, Object o) {
        // for the moment iterator will look for an OMNode as the
        // iteration element
        if (!(o instanceof OMNode)) {
            handleException("Error splitting message with XPath : " + expression + " - result not an OMNode", synCtx);
        }
    }

    /**
     * Create a new message context using the given original message context,
     * the envelope and the split result element.
     *
     * @param synCtx
     *            - original message context
     * @param msgNumber
     *            - message number in the iteration
     * @param msgCount
     *            - total number of messages in the split
     * @param envelope
     *            - envelope to be used in the iteration
     * @param o
     *            - element which participates in the iteration replacement
     * @return newCtx created by the iteration
     * @throws AxisFault
     *             if there is a message creation failure
     * @throws JaxenException
     *             if the expression evaluation failure
     */
    private MessageContext getIteratedMessageContext(MessageContext synCtx, int msgNumber, int msgCount,
            SOAPEnvelope envelope, List<OMNode> elements) throws AxisFault, JaxenException {

        // clone the message context without cloning the SOAP envelope, for the
        // mediation in iteration.
        MessageContext newCtx = MessageHelper.cloneMessageContext(synCtx);

        setMessageSequenceAndParentCorrelationProperties(synCtx, msgNumber, msgCount, newCtx);

        // get a clone of the envelope to be attached
        SOAPEnvelope newEnvelope = MessageHelper.cloneSOAPEnvelope(envelope);

        // if payload should be preserved then attach the iteration element to
        // the node specified by the attachPath
        if (isPreservePayload()) {
            preservePayload(synCtx, elements, newEnvelope);
        } else if (newEnvelope.getBody() != null) {
            dontPreservePayload(elements, newEnvelope);
        }
        // set the envelope and mediate as specified in the target
        newCtx.setEnvelope(newEnvelope);

        return newCtx;
    }

    private void dontPreservePayload(List<OMNode> elements, SOAPEnvelope newEnvelope) {
        // if not preserve payload then attach the iteration element to the body
        OMElement firstElement = newEnvelope.getBody().getFirstElement();
        if (firstElement != null) {
            firstElement.detach();
        }
        OMElement batchElement = newEnvelope.getOMFactory().createOMElement(new QName("batch"));
        newEnvelope.getBody().addChild(batchElement);
        for (OMNode element : elements) {
            batchElement.addChild(element);
        }
    }

    private void preservePayload(MessageContext synCtx, List<OMNode> elements, SOAPEnvelope newEnvelope) {
        Object attachElem = getAttachPath().evaluate(newEnvelope, synCtx);
        if (attachElem != null && attachElem instanceof List<?> && !((List<?>) attachElem).isEmpty()) {
            attachElem = ((List<?>) attachElem).get(0);
        }

        // for the moment attaching element should be an OMElement
        if (attachElem != null && attachElem instanceof OMElement) {
            OMElement attachElement = (OMElement) attachElem;
            for (OMNode child : elements) {
                attachElement.addChild(child);
            }
        } else {
            handleException("Error in attaching the splitted elements :: "
                    + "Unable to get the attach path specified by the expression " + getAttachPath(), synCtx);
        }
    }

    private void setMessageSequenceAndParentCorrelationProperties(MessageContext synCtx, int msgNumber, int msgCount,
            MessageContext newCtx) {
        String messageSequenceKey = EIPConstants.MESSAGE_SEQUENCE;
        String id = getId();
        if (id != null) {
            // set the parent correlation details to the cloned MC -
            // for the use of aggregation like tasks
            newCtx.setProperty(EIPConstants.AGGREGATE_CORRELATION + "." + id, synCtx.getMessageID());
            // set the messageSequence property for possible aggregations
            messageSequenceKey += "." + id;
        }

        newCtx.setProperty(messageSequenceKey, msgNumber + EIPConstants.MESSAGE_SEQUENCE_DELEMITER + msgCount);
    }

    /**
     * Copy fault stack and properties of the iteratedMsgCtx to synCtx
     *
     * @param synCtx
     *            Original Synapse Message Context
     * @param iteratedMsgCtx
     *            cloned Message Context used for the iteration
     */
    private void copyFaultyIteratedMessage(MessageContext synCtx, MessageContext iteratedMsgCtx) {
        synCtx.getFaultStack().clear(); // remove original fault stack
        Stack<FaultHandler> faultStack = iteratedMsgCtx.getFaultStack();

        if (!faultStack.isEmpty()) {
            List<FaultHandler> newFaultStack = new ArrayList<FaultHandler>();
            newFaultStack.addAll(faultStack);
            for (FaultHandler faultHandler : newFaultStack) {
                if (faultHandler != null) {
                    synCtx.pushFaultHandler(faultHandler);
                }
            }
        }
        // copy all the String keyed synapse level properties to the Original
        // synCtx
        for (Object keyObject : iteratedMsgCtx.getPropertyKeySet()) {
            /*
             * There can be properties added while executing the iterated
             * sequential flow and these may be accessed in the fault sequence,
             * so updating string valued properties
             */
            if (keyObject instanceof String) {
                String stringKey = (String) keyObject;
                synCtx.setProperty(stringKey, iteratedMsgCtx.getProperty(stringKey));
            }
        }
    }

    private class ElementBatch {
        List<OMNode> elements;

        ElementBatch() {
            elements = new LinkedList<OMNode>();
        }
    }
}
