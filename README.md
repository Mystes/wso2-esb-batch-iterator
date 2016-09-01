# WSO2 ESB Batch Iterator Mediator

## What is WSO2 ESB?
[WSO2 ESB](http://wso2.com/products/enterprise-service-bus/) is an open source Enterprise Service Bus that enables interoperability among various heterogeneous systems and business applications.

## Features
Batch Iterator Mediator extends Iterate Mediator by providing several elements (batch) iteration at a time.

## Usage

### 1. Install the mediator to the ESB
Copy the `BatchIteratorMediator-x.y.jar` to `$WSO2_ESB_HOME/repository/components/dropins/`.

### 2. Use it in your proxies/sequences
Mediator can be used as follows:
```xml
<batchIterateor batchSize="number" [continueParent=(true | false)] [preservePayload=(true | false)] (attachPath="xpath")? expression="xpath">
   <target [to="uri"] [soapAction="qname"] [sequence="sequence_ref"] [endpoint="endpoint_ref"]>
     <sequence>
       (mediator)+
     </sequence>?
     <endpoint>
       endpoint
     </endpoint>?
   </target>+
</batchIterateor>
```

#### Example: detach by element
```xml
<iterate batchSize="3" expression="//iterate">
   <target>
     <sequence>
       <!-- Do something here with elements -->
     </sequence>
   </target>
</iterate>
```

## Tested with

- WSO2 ESB 4.5.1
- WSO2 ESB 4.8.1

## [License](LICENSE)

Copyright &copy; 2016 [Mystes Oy](http://www.mystes.fi). Licensed under the [Apache 2.0 License](LICENSE).
