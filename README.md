[![Maven Central](https://img.shields.io/maven-central/v/org.apache.vysper/vysper-parent)](https://search.maven.org/search?q=g:org.apache.vysper%20AND%20a:vysper-parent&core=gav)
[![Jenkins](https://img.shields.io/jenkins/build/https/builds.apache.org/Vysper)](https://builds.apache.org/job/Vysper/)
[![Jenkins tests](https://img.shields.io/jenkins/tests/https/builds.apache.org/Vysper?compact_message&failed_label=failed&passed_label=passed&skipped_label=skipped)](https://builds.apache.org/job/Vysper/)

# Apache Vysper

This project aims at delivering a server implementation of both the core and IM parts of the XMPP protocol 
(as specified in RFCs [3920](http://www.ietf.org/rfc/rfc3920.txt) + [3921](http://www.ietf.org/rfc/rfc3921.txt)).

"Vysper" is pronounced like in "whisper".

[XMPP](http://en.wikipedia.org/wiki/XMPP) is an open, secure and extensible instant messaging protocol which has evolved from Jabber.  
It provides interoperability features for communication with other XMPP and non-XMPP servers.  
It is used and supported by many IM applications, both client and server.  

The protocol and its many extensions (called XEPs) are maintained by the XMPP Standards Foundation ([XSF](http://www.xmpp.org/).

In addition to the basic protocol and many small extensions, Vysper currently comes with working implementations for  
&nbsp;&nbsp;Multi-User Chat ([XEP-0045](http://xmpp.org/extensions/xep-0045.html)) [5]  
and  
&nbsp;&nbsp;Publish-Subscribe ([XEP-0060](http://xmpp.org/extensions/xep-0060.html)) [6] 

# Building 

You need Apache Maven 2, Maven 2.2.1 or later is recommended. Run  
&nbsp;&nbsp;`mvn install`  
and you should find a number of JAR files in different target/ folders.  
The different build artifacts are compiled in dist/.  

# Running

There are different ways to run Vysper

A ready-to-run setup is created by the build in  
&nbsp;&nbsp;`dist/target/appassembler/`  
Under  
&nbsp;&nbsp;`dist/target/appassembler/bin`  
you'll find start scripts for Unix/Mac and Windows.

This makes use of the Spring-based server runtime.  
Main class is  
&nbsp;&nbsp;`org.apache.vysper.spring.ServerMain`  
and the bean configuration is located in  
&nbsp;&nbsp;`server/core/src/main/config/spring-config.xml`

There is a non-Spring runtime, too:  
&nbsp;&nbsp;`org.apache.vysper.spring.ServerMain`  
The source code shows how the different components are plugged together.

It can serve as a template for integrating Vysper in any other application.  
The class  
&nbsp;&nbsp;`org.apache.vysper.xmpp.server.XMPPServer`  
is built to make the server easily configurable and embeddable.
  
# Configuration

Vysper uses SSL encryption per default.  
For this purpose, an TLS certificate is provided.  
It is highly recommended to create and use a self-generated certificate!

There is one preconfigured user (admin@vysper.org). The password for this  
user is "CHOOSE A SECURE PASSWORD". Please change this password before starting!

Please note that the domain vysper.org is not running an XMPP server.  
If you go with the default setup and don't configure your own domain name,  
please note that you have to configure your Jabber clients to force the host name  
(for example the IP or localhost, depending on how you run it).

# Contributing

You can contribute by creating a new issue entry (or working on an existing).  

For an overview of all VYSPER related issues, visit https://github.com/apache/mina-vysper/issues

Any contribution is highly welcome. It can be easily reviewed if it comes in form of a Github pull request.  

Currently, all coding is done unit test driven. Well, at least it should be ;-)

Please find us on MINA'S developer mailing list dev@mina.apache.org. 