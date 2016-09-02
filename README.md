# WSO2 ESB Batch Iterator Mediator

## What is WSO2 ESB?
[WSO2 ESB](http://wso2.com/products/enterprise-service-bus/) is an open source Enterprise Service Bus that enables interoperability among various heterogeneous systems and business applications.

## Features
Batch Iterator Mediator extends [WOS2 Iterate Mediator](https://docs.wso2.com/display/ESB500/Iterate+Mediator) by providing iteration by several elements (batch) at a time.

## Usage

### 1. Install the mediator to the ESB
Copy the `wso2-esb-batch-iterator-mediator-x.y.jar` to `$WSO2_ESB_HOME/repository/components/dropins/`.

### 2. Use it in your proxies/sequences
Mediator can be used as follows:
```xml
<batchIterator batchSize="number" [continueParent=(true | false)] [preservePayload=(true | false)] (attachPath="xpath")? expression="xpath">
   <target [to="uri"] [soapAction="qname"] [sequence="sequence_ref"] [endpoint="endpoint_ref"]>
     <sequence>
       (mediator)+
     </sequence>?
     <endpoint>
       endpoint
     </endpoint>?
   </target>+
</batchIterator>
```

#### Example
```xml
<batchIterator batchSize="3" expression="//iterate">
   <target>
     <sequence>
       <!-- Do something here with elements -->
     </sequence>
   </target>
</batchIterator>
```

## Technical Requirements

#### Usage

* Oracle Java 6 or above
* WSO2 ESB
    * Wrapper Mediator has been tested with WSO2 ESB versions 4.9.0 & 5.0.0

#### Development

* Java 6 + Maven 3.0.X

### Contributors

- [Kreshnik Gunga](https://github.com/kgunga)
- [Ville Harvala](https://github.com/vharvala)

## [License](LICENSE)

Copyright &copy; 2016 [Mystes Oy](http://www.mystes.fi). Licensed under the [Apache 2.0 License](LICENSE).
